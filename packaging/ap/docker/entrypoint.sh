#!/bin/bash
# runtime state
HARMONY_BASE=/var/opt/harmony-ap
# static and read-only
HARMONY_HOME=/opt/harmony-ap

# Make it possible to easily exec into the container
[[ "$1" = "bash" || "$1" = "sh" ]] && { shift; exec /bin/bash "$@"; }

if [[ $1 == "help" ]]; then
  cat <<__EOF
  Harmony eDelivery Access - Access Point version ${HARMONY_VERSION:-UNKNOWN}

  Parameters configurable via environment and/or configuration file
  (location can be passed by variable HARMONY_PARAM_FILE).
  Environment variables override the parameter file.

  param                            (default)
  --------------------------------------------------
  DB_HOST                          *required*
  DB_PORT                          3306
  DB_SCHEMA                        harmony_ap
  DB_USER                          harmony_ap
  DB_PASSWORD                      *required*
  USE_DYNAMIC_DISCOVERY            false
  SML_ZONE                         *empty*

  # For clustered deployment, needs to be unique per node
  NODE_ID                          $HOSTNAME

  # Other config
  MAX_MEM                          512m
  PRESERVE_BACKUP_FILE_DATE        false if config is on SMB filesystem, otherwise true

  # The following params can not be set once written to config:
  PARTY_NAME                       selfsigned
  SERVER_FQDN                      *output of 'hostname -f'*
  SERVER_DN                        CN=*SERVER_FQDN*
  SERVER_SAN                       DNS:*SERVER_FQDN*
  SECURITY_DN                      CN=*PARTY_NAME*
  SECURITY_KEYSTORE_PASSWORD       *random*
  SECURITY_TRUSTSTORE_PASSWORD     *random*
  TLS_KEYSTORE_PASSWORD            *random*
  TLS_KEYSTORE_FILE                ${HARMONY_BASE}/etc/tls-keystore.p12
  TLS_TRUSTSTORE_PASSWORD          *random*
  ADMIN_USER                       harmony
  ADMIN_PASSWORD                   *random*

  # clustering setup
  DEPLOYMENT_CLUSTERED             false
  ACTIVEMQ_BROKER_HOST             *required if clustered*
  ACTIVEMQ_BROKER_NAME             localhost
  ACTIVEMQ_USERNAME                *required*
  ACTIVEMQ_PASSWORD                *required*

  # if true, offload TLS termination to an external LB. AP will use port 8080
  EXTERNAL_LB                      false
__EOF
  exit 1
fi

# force initialization
INIT=false
[[ -v FORCE_INIT || "$1" = "init" ]] && INIT=true

set -euo pipefail
umask 027

log() { echo "$(date --utc -Iseconds) INFO [entrypoint] $*"; }
warn() { echo "$(date --utc -Iseconds) WARN [entrypoint] $*" >&2; }
error() { echo "$(date --utc -Iseconds) ERROR [entrypoint] $*" >&2; }

set_prop() {
  local prop="$1"
  local value="${2:-}"
  local dir="${3:-$HARMONY_BASE}"

  # escape \ -> \\ (required by java property file syntax)
  value="${value//\\/\\\\}"
  # escape Spring ${property} placeholders: $ -> ${:$}
  value="${value//\$\{/\$\{:\$\}\{}"
  crudini --inplace --set "$dir/domibus.properties" "" "$prop" "$value"
}

set_prop_raw() {
  local prop="$1"
  local value="${2:-}"
  local dir="${3:-$HARMONY_BASE}"

  # escape \ -> \\ (required by java property file syntax)
  value="${value//\\/\\\\}"
  crudini --inplace --set "$dir/domibus.properties" "" "$prop" "$value"
}

del_prop() {
  local prop="$1";
  local dir="${2:-$HARMONY_BASE}"
  crudini --inplace --del "$dir/domibus.properties" "" "$prop"
}

get_prop() {
  local prop="$1"
  local default="${2:-}"
  local value
  value=$(crudini --get ${HARMONY_BASE}/etc/domibus.properties "" "$prop" 2>/dev/null)
  # Unescape: \\ -> \, ${:$} -> $
  value="${value//\\\\/\\}"
  value="${value//\$\{:\$\}\{/\$\{}"
  echo "${value:-$default}"
}

get_tomcat_prop() {
  local default="${2:-}"
  local value
  value=$(xmlstarlet sel -t -v "//Connector/@$1" ${HARMONY_BASE}/conf/server.xml 2>/dev/null)
  echo "${value:-$default}";
}


if [[ -n ${HARMONY_PARAM_FILE:-} && -f $HARMONY_PARAM_FILE ]]; then
  log "Reading parameters from $HARMONY_PARAM_FILE..."
  while IFS='=' read -r key value
  do
    if [[ $key != \#* && -n "$key" && -z "${!key:-}" && -n "$value" ]]; then
      printf -v "${key}" '%s' "${value}"
    fi
  done < "${HARMONY_PARAM_FILE}"
fi

# for clustering support -- only one entrypoint should run simultaneously
mkdir -p -m 0750 "${HARMONY_BASE}/etc"
exec 9>${HARMONY_BASE}/etc/.init-lock
flock -w 300 9 || exit 1

CONF_VERSION=
[[ -f $HARMONY_BASE/etc/HARMONY_VERSION ]] && read -r CONF_VERSION <$HARMONY_BASE/etc/HARMONY_VERSION

[[ $INIT != true ]] && log "Staring Harmony Access Point version ${HARMONY_VERSION:-UNDEFINED}..."

DB_SCHEMA="${DB_SCHEMA:-$(get_prop 'domibus.database.schema' 'harmony_ap')}"
DB_HOST="${DB_HOST:-$(get_prop 'domibus.database.serverName')}"
DB_PORT="${DB_PORT:-$(get_prop 'domibus.database.port' '3306')}"
DB_USER="${DB_USER:-$(get_prop 'domibus.datasource.user' 'harmony_ap')}"
DB_PASSWORD="${DB_PASSWORD:-$(get_prop 'domibus.datasource.password')}"
USE_DYNAMIC_DISCOVERY="${USE_DYNAMIC_DISCOVERY:-$(get_prop domibus.dynamicdiscovery.useDynamicDiscovery false)}"
SML_ZONE="${SML_ZONE:-$(get_prop domibus.smlzone)}"
TLS_TRUSTSTORE_PASSWORD=$(get_tomcat_prop 'truststorePass' "${TLS_TRUSTSTORE_PASSWORD:-}")

DEPLOYMENT_CLUSTERED=$(get_prop 'domibus.deployment.clustered' "${DEPLOYMENT_CLUSTERED:-false}")
EXTERNAL_LB=${EXTERNAL_LB:-false}
NODE_ID=${NODE_ID:-$HOSTNAME}
_SETUP=false

if [[ -z $DB_HOST || -z $DB_PASSWORD ]]; then
  error "Database host (DB_HOST) and password (DB_PASSWORD) are required";
  exit 1
fi

# Workaround for Azure File SMB shares
FSTYPE=$(stat -f "${HARMONY_BASE}/etc" --format "%T")
if [[ $FSTYPE == smb* || $FSTYPE == "cifs" ]]; then
  warn "${HARMONY_BASE/etc} is located on a $FSTYPE filesystem"
  PRESERVE_BACKUP_FILE_DATE=${PRESERVE_BACKUP_FILE_DATE:-false}
else
  PRESERVE_BACKUP_FILE_DATE=${PRESERVE_BACKUP_FILE_DATE:-true}
fi

log "Waiting for database $DB_HOST:$DB_PORT to become available...."
count=0
wait_count=60;
[[ $INIT = true ]] && wait_count=2
while ((count++ < wait_count)) && ! mysqladmin status -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" &> /tmp/mysql-status; do
  sleep 5;
done

if ((count>=wait_count)); then
  error "Unable to determine database status:"
  sed 's/^/    /' /tmp/mysql-status;
  [[ $INIT != true ]] && exit 1
  _DB_AVAILABLE=false
else
  _DB_AVAILABLE=true
fi

# temporary dir for configuration updates
# workaround for SMB shares, but also makes updates more atomic
TEMPDIR=$(mktemp -d -p /tmp)
set_prop_tmp() { set_prop "$1" "$2" "$TEMPDIR"; }

if [[ $INIT = "true" || $HARMONY_VERSION != "$CONF_VERSION" || ! -f ${HARMONY_BASE}/etc/domibus.properties ]]; then
  # populate the home dir at first use to support volumes e.g. in k8s environments
  _SETUP="true"
  log "Configuring access point..."
  mkdir -p -m 0770 \
    "${HARMONY_BASE}/etc" \
    "${HARMONY_BASE}/etc/plugins/config" \
    "${HARMONY_BASE}/etc/certs" \
    "${HARMONY_BASE}/conf" \
    "${HARMONY_BASE}/log" \
    "${HARMONY_BASE}/work"

  chown -R harmony-ap:harmony-ap "${HARMONY_BASE}"/{etc,conf,log,work} || true

  if [[ $INIT = "true" && $_DB_AVAILABLE = "false" ]]; then
    warn "Initialization forced. Skipping database migrations due to DB not available."
  else
    log "Applying database migrations..."
    export LIQUIBASE_HOME=${HARMONY_HOME}/liquibase
    export LIQUIBASE_SHOW_BANNER=false
    URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_SCHEMA"
    _AUSER="${ADMIN_USER:-harmony}"
    _APASSWORD="${ADMIN_PASSWORD:-$(openssl rand -base64 12)}"
    HASHEDPASSWORD=$(java -cp "${HARMONY_HOME}/webapps/ROOT/WEB-INF/lib/*" \
      eu.domibus.api.util.BCryptPasswordHash "$_APASSWORD")

    $LIQUIBASE_HOME/liquibase.sh --defaultsFile=<(echo "\
searchPath:${HARMONY_HOME}/db
classpath:${HARMONY_HOME}/lib/mysql-connector-j-8.2.0.jar
driver:com.mysql.cj.jdbc.Driver
url:$URL
username:$DB_USER
password:${DB_PASSWORD//\\/\\\\}
changeLogFile:db.changelog.xml") \
    update -DadminUser="$_AUSER" -DadminPassword="$HASHEDPASSWORD" 2>&1 | sed 's/^/    /'

    if [[ -z ${ADMIN_PASSWORD:-} ]]; then
      log "*** Generated Harmony AP admin user: $_AUSER, password: $_APASSWORD"
    fi
  fi

  PARTY_NAME="$(get_prop domibus.security.key.private.alias "${PARTY_NAME:-selfsigned}")"
  SERVER_FQDN="${SERVER_FQDN:-$(hostname -f)}"
  SERVER_DN="${SERVER_DN:-CN=$SERVER_FQDN}"
  SERVER_SAN="${SERVER_SAN:-DNS:$SERVER_FQDN}"
  SECURITY_DN="${SECURITY_DN:-CN=$PARTY_NAME}"

  SECURITY_KEYSTORE_PASSWORD="$(get_prop domibus.security.keystore.password "${SECURITY_KEYSTORE_PASSWORD:-$(openssl rand -base64 12)}")"
  SECURITY_TRUSTSTORE_PASSWORD="$(get_prop domibus.security.truststore.password "${SECURITY_TRUSTSTORE_PASSWORD:-$(openssl rand -base64 12)}")"

  TLS_KEYSTORE_PASSWORD="$(get_tomcat_prop keystorePass "${TLS_KEYSTORE_PASSWORD:-$(openssl rand -base64 12)}")"
  TLS_KEYSTORE_FILE="$(get_tomcat_prop keystoreFile "${TLS_KEYSTORE_FILE:-$HARMONY_BASE/etc/tls-keystore.p12}")"
  TLS_TRUSTSTORE_PASSWORD="${TLS_TRUSTSTORE_PASSWORD:-$(openssl rand -base64 12)}"

  if [ ! -f ${HARMONY_BASE}/etc/ap-keystore.p12 ]; then
    log "Creating keystore..."
    keytool -storetype pkcs12 -genkeypair -keyalg RSA -alias "$PARTY_NAME" -keystore ${HARMONY_BASE}/etc/ap-keystore.p12 -storepass "$SECURITY_KEYSTORE_PASSWORD" \
      -keypass "$SECURITY_KEYSTORE_PASSWORD" -validity 333 -keysize 3072 -dname "$SECURITY_DN" 2>/dev/null

    keytool -export -alias "$PARTY_NAME" -keystore ${HARMONY_BASE}/etc/ap-keystore.p12 -storepass "$SECURITY_KEYSTORE_PASSWORD" \
      -file "$HARMONY_BASE/etc/certs/security-${PARTY_NAME}.cer"
  fi

  if [ ! -f ${HARMONY_BASE}/etc/ap-truststore.p12 ]; then
    log "Creating empty truststore..."
    keytool -storetype pkcs12 -genkeypair -alias mock -keystore ${HARMONY_BASE}/etc/ap-truststore.p12 \
      -storepass "$SECURITY_TRUSTSTORE_PASSWORD" \
      -keypass "$SECURITY_TRUSTSTORE_PASSWORD" -dname "CN=mock" 2>/dev/null
    keytool -delete -alias mock -keystore ${HARMONY_BASE}/etc/ap-truststore.p12 -storepass "$SECURITY_TRUSTSTORE_PASSWORD" 2>/dev/null
  fi

  if [ ! -f "${TLS_KEYSTORE_FILE}" ]; then
    log "Creating TLS keystore..."
    keytool -storetype pkcs12 -genkeypair -keyalg RSA -alias "$PARTY_NAME" \
      -keystore "${TLS_KEYSTORE_FILE}" -storepass "$TLS_KEYSTORE_PASSWORD" \
      -validity 333 -keysize 3072 -dname "$SERVER_DN" -ext "SAN=$SERVER_SAN" 2>/dev/null

    keytool -export -alias "$PARTY_NAME" \
      -keystore "${TLS_KEYSTORE_FILE}" -storepass "$TLS_KEYSTORE_PASSWORD" \
      -file "$HARMONY_BASE/etc/certs/tls-${PARTY_NAME}.cer"
  fi

  if [ ! -f ${HARMONY_BASE}/etc/tls-truststore.p12 ]; then
    log "Creating TLS truststore..."
    keytool -export -alias "$PARTY_NAME" \
      -keystore "$TLS_KEYSTORE_FILE" \
      -storepass "$TLS_KEYSTORE_PASSWORD" 2>/dev/null | keytool \
        -storetype pkcs12 -import -noprompt -alias "$PARTY_NAME" \
        -keystore ${HARMONY_BASE}/etc/tls-truststore.p12 \
        -storepass "$TLS_TRUSTSTORE_PASSWORD" 2>/dev/null
  fi

  _copy() { [[ -f "$2" ]] || cat "$1" > "$2"; }
  _copy ${HARMONY_HOME}/setup/domibus.properties.template ${HARMONY_BASE}/etc/domibus.properties
  _copy ${HARMONY_HOME}/setup/server.xml.template ${HARMONY_BASE}/conf/server.xml
  _copy ${HARMONY_HOME}/setup/clientauthentication.xml.template ${HARMONY_BASE}/etc/clientauthentication.xml
  _copy ${HARMONY_HOME}/setup/logback.xml ${HARMONY_BASE}/etc/logback.xml

  cp -r -n ${HARMONY_HOME}/internal ${HARMONY_BASE}/etc/
  cp -r -n ${HARMONY_HOME}/policies ${HARMONY_BASE}/etc/
  cp -n ${HARMONY_HOME}/conf/* ${HARMONY_BASE}/conf/

  if [[ $DEPLOYMENT_CLUSTERED = "true" || $EXTERNAL_LB = "true" ]]; then
    _sslenabled=false
    _port=8080
  else
    _sslenabled=true
    _port=8443
  fi

  xmlstarlet edit --pf  \
    --update '//Connector/@SSLEnabled'     --value "$_sslenabled" \
    --update '//Connector/@port'           --value "$_port" \
    --update '//Connector/@keystorePass'   --value "$TLS_KEYSTORE_PASSWORD" \
    --update '//Connector/@keystoreFile'   --value "$TLS_KEYSTORE_FILE" \
    --update '//Connector/@truststorePass' --value "$TLS_TRUSTSTORE_PASSWORD" \
    ${HARMONY_BASE}/conf/server.xml >$TEMPDIR/server.xml
  cp "$TEMPDIR/server.xml" "${HARMONY_BASE}/conf/server.xml"

  xmlstarlet edit --pf  \
    -N s='http://cxf.apache.org/configuration/security' \
    --update '//s:trustManagers/s:keyStore/@password' --value "$TLS_TRUSTSTORE_PASSWORD" \
    --update '//s:trustManagers/s:keyStore/@file'     --value "${HARMONY_BASE}/etc/tls-truststore.p12" \
    --update '//s:keyManagers/s:keyStore/@password'   --value "$TLS_KEYSTORE_PASSWORD" \
    --update '//s:keyManagers/s:keyStore/@file'       --value "$TLS_KEYSTORE_FILE" \
    ${HARMONY_BASE}/etc/clientauthentication.xml >/$TEMPDIR/clientauthentication.xml
  cp "$TEMPDIR/clientauthentication.xml" "${HARMONY_BASE}/etc/clientauthentication.xml"

  cp "$HARMONY_BASE/etc/domibus.properties" "$TEMPDIR/"

  set_prop_tmp domibus.security.keystore.password     "$SECURITY_KEYSTORE_PASSWORD"
  set_prop_tmp domibus.security.key.private.alias     "$PARTY_NAME"
  set_prop_tmp domibus.security.key.private.password  "$SECURITY_KEYSTORE_PASSWORD"
  set_prop_tmp domibus.security.truststore.password   "$SECURITY_TRUSTSTORE_PASSWORD"

  if [[ $DEPLOYMENT_CLUSTERED = "true" ]]; then
    log "Enabling clustered deployment, using ActiveMQ Broker at $ACTIVEMQ_BROKER_HOST"
    set_prop_tmp "domibus.deployment.clustered"        "true"
    set_prop_tmp "activeMQ.brokerName"                 "${ACTIVEMQ_BROKERNAME:-localhost}"
    set_prop_tmp "activeMQ.embedded.configurationFile" ""
    set_prop_tmp "activeMQ.broker.host"                "${ACTIVEMQ_BROKER_HOST}"
    set_prop_tmp "activeMQ.username"                   "${ACTIVEMQ_USERNAME}"
    set_prop_tmp "activeMQ.password"                   "${ACTIVEMQ_PASSWORD}"
  fi
fi

# always updated properies
cp -n "$HARMONY_BASE/etc/domibus.properties" "$TEMPDIR/"

set_prop_tmp 'domibus.database.serverName' "$DB_HOST"
set_prop_tmp 'domibus.database.port'       "$DB_PORT"
set_prop_tmp 'domibus.database.schema'     "$DB_SCHEMA"
set_prop_tmp 'domibus.datasource.user'     "$DB_USER"
set_prop_tmp 'domibus.datasource.password' "$DB_PASSWORD"
set_prop_tmp 'domibus.smlzone'             "$SML_ZONE"
set_prop_tmp 'domibus.dynamicdiscovery.useDynamicDiscovery' "$USE_DYNAMIC_DISCOVERY"

cp "$TEMPDIR/domibus.properties" "$HARMONY_BASE/etc/"

if [[ $_SETUP = "true" && $_DB_AVAILABLE = "true" ]]; then
  echo "$HARMONY_VERSION" >$HARMONY_BASE/etc/HARMONY_VERSION
  log "Configuration done"
fi

rm -rf "$TEMPDIR"
# release lock
exec 9>&-

if [[ $INIT = "true" ]]; then
  log "Just init requested, exiting..."
  exit 0;
fi

if [[ $DEPLOYMENT_CLUSTERED = "true" ]]; then
  _WORK_LOCATION="${HARMONY_BASE}/work/${NODE_ID}"
else
  _WORK_LOCATION="${HARMONY_BASE}/work"
fi
CATALINA_TMPDIR=/var/tmp/harmony-ap
CATALINA_HOME=${HARMONY_HOME}
CATALINA_BASE=${HARMONY_BASE}

# escape \ and " in the password
_TLS_TRUSTSTORE_PASSWORD="${TLS_TRUSTSTORE_PASSWORD//\\/\\\\}"
_TLS_TRUSTSTORE_PASSWORD="${_TLS_TRUSTSTORE_PASSWORD//\"/\\\"}"

# @<(echo "...") is an on-the-fly generated Java command-line argument file
exec tini -e 143 -- java @<(echo "\
-Xms256m -Xmx${MAX_MEM:-512m}
-XX:+UseParallelGC
-XX:+ExitOnOutOfMemoryError
-XX:MaxMetaspaceSize=256m
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
--add-opens java.management/sun.management=ALL-UNNAMED
--add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED
--add-exports java.base/jdk.internal.ref=ALL-UNNAMED
-Djava.awt.headless=true
-Djava.io.tmpdir=$CATALINA_TMPDIR
-Djava.security.egd=file:/dev/urandom
-Djava.util.logging.config.file=$CATALINA_BASE/conf/logging.properties
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
-Djavax.net.ssl.trustStore=${HARMONY_BASE}/etc/tls-truststore.p12
-Djavax.net.ssl.trustStorePassword=\"$_TLS_TRUSTSTORE_PASSWORD\"
-Dlogback.configurationFile=${HARMONY_BASE}/etc/logback.xml
-Dcatalina.base=$CATALINA_BASE
-Dcatalina.home=$CATALINA_HOME
-Ddomibus.config.location=${HARMONY_BASE}/etc
-Ddomibus.work.location=${_WORK_LOCATION}
-Ddomibus.extensions.location=/opt/harmony-ap
-Ddomibus.node.id=${NODE_ID}
-Ddomibus.backup.preserveFileDate=${PRESERVE_BACKUP_FILE_DATE}
-classpath $CATALINA_HOME/bin/bootstrap.jar:$CATALINA_HOME/bin/tomcat-juli.jar
") "$@" org.apache.catalina.startup.Bootstrap
