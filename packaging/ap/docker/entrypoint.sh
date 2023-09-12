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

  MAX_MEM                          512m

  # The following params can not be set once written to config:

  PARTY_NAME                       selfsigned
  SERVER_FQDN                      *output of 'hostname -f'*
  SERVER_DN                        CN=*SERVER_FQDN*
  SECURITY_KEYSTORE_PASSWORD       *random*
  SECURITY_TRUSTSTORE_PASSWORD     *random*
  TLS_KEYSTORE_PASSWORD            *random*
  TLS_KEYSTORE_FILE                ${HARMONY_BASE}/etc/tls-keystore.p12
  TLS_TRUSTSTORE_PASSWORD          *random*
  USE_DYNAMIC_DISCOVERY            false
  SML_ZONE                         *empty*
  ADMIN_USER                       harmony
  ADMIN_PASSWORD                   *random*
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
  # escape \ -> \\ (required by java property file syntax)
  value="${value//\\/\\\\}"
  # escape Spring ${property} placeholders: $ -> ${:$}
  value="${value//\$\{/\$\{:\$\}\{}"
  crudini --inplace --set ${HARMONY_BASE}/etc/domibus.properties "" "$prop" "$value"
}

delete_prop() {
  local prop="$1";
  crudini --inplace --get ${HARMONY_BASE}/etc/domibus.properties "" "$prop"
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
  value=$(xmlstarlet sel -t -v "//Connector[@SSLEnabled=\"true\"]/@$1" ${HARMONY_BASE}/conf/server.xml 2>/dev/null)
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

CONF_VERSION=
[[ -f $HARMONY_BASE/HARMONY_VERSION ]] && read -r CONF_VERSION <$HARMONY_BASE/HARMONY_VERSION

[[ $INIT != true ]] && log "Staring Harmony Access Point version ${HARMONY_VERSION:-UNDEFINED}..."

DB_SCHEMA="${DB_SCHEMA:-$(get_prop 'domibus.database.schema' 'harmony_ap')}"
DB_HOST="${DB_HOST:-$(get_prop 'domibus.database.serverName')}"
DB_PORT="${DB_PORT:-$(get_prop 'domibus.database.port' '3306')}"
DB_USER="${DB_USER:-$(get_prop 'domibus.datasource.user' 'harmony_ap')}"
DB_PASSWORD="${DB_PASSWORD:-$(get_prop 'domibus.datasource.password')}"
TLS_TRUSTSTORE_PASSWORD=$(get_tomcat_prop truststorePass "${TLS_TRUSTSTORE_PASSWORD:-}")

if [[ -z $DB_HOST || -z $DB_PASSWORD ]]; then
  error "Database host (DB_HOST) and password (DB_PASSWORD) are required";
  exit 1
fi

log "Waiting for database $DB_HOST:$DB_PORT to become available...."
count=0
wait_count=60;
[[ $INIT = true ]] && wait_count=1
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

if [[ $INIT = "true" || $HARMONY_VERSION != "$CONF_VERSION" || ! -f ${HARMONY_BASE}/etc/domibus.properties ]]; then
  # populate the home dir at first use to support volumes e.g. in k8s environments
  log "Configuring access point..."

  mkdir -p -m 0750 \
    "${HARMONY_BASE}/etc" \
    "${HARMONY_BASE}/etc/plugins" \
    "${HARMONY_BASE}/conf" \
    "${HARMONY_BASE}/log" \
    "${HARMONY_BASE}/work"
  chown -R harmony-ap:harmony-ap "${HARMONY_BASE}"/*

  if [[ $INIT = "true" && $_DB_AVAILABLE = "false" ]]; then
    warn "Initialization forced. Skipping database migrations due to DB not available."
  else
    log "Applying database migrations..."
    export LIQUIBASE_HOME=${HARMONY_HOME}/liquibase
    export URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_SCHEMA"
    export LIQUIBASE_SHOW_BANNER=false
    _AUSER="${ADMIN_USER:-harmony}"
    _APASSWORD="${ADMIN_PASSWORD:-$(openssl rand -base64 12)}"
    HASHEDPASSWORD=$(java -cp "${HARMONY_HOME}/webapps/ROOT/WEB-INF/lib/*" \
      eu.domibus.api.util.BCryptPasswordHash "$_APASSWORD")

    $LIQUIBASE_HOME/liquibase.sh --defaultsFile=<(echo "\
searchPath:${HARMONY_HOME}/db
classpath:${HARMONY_HOME}/lib/mysql-connector-j-8.0.33.jar
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
  SML_ZONE="$(get_prop domibus.smlzone "${SML_ZONE:-}")"
  USE_DYNAMIC_DISCOVERY=$(get_prop domibus.dynamicdiscovery.useDynamicDiscovery "${USE_DYNAMIC_DISCOVERY:-false}")

  SECURITY_KEYSTORE_PASSWORD="$(get_prop domibus.security.keystore.password "${SECURITY_KEYSTORE_PASSWORD:-$(openssl rand -base64 12)}")"
  SECURITY_TRUSTSTORE_PASSWORD="$(get_prop domibus.security.truststore.password "${SECURITY_TRUSTSTORE_PASSWORD:-$(openssl rand -base64 12)}")"

  TLS_KEYSTORE_PASSWORD="$(get_tomcat_prop keystorePass "${TLS_KEYSTORE_PASSWORD:-$(openssl rand -base64 12)}")"
  TLS_KEYSTORE_FILE="$(get_tomcat_prop keystoreFile "${TLS_KEYSTORE_FILE:-$HARMONY_BASE/etc/tls-keystore.p12}")"
  TLS_TRUSTSTORE_PASSWORD="${TLS_TRUSTSTORE_PASSWORD:-$(openssl rand -base64 12)}"

  if [ ! -f ${HARMONY_BASE}/etc/ap-keystore.p12 ]; then
    log "Creating keystore..."
    keytool -storetype pkcs12 -genkeypair -keyalg RSA -alias "$PARTY_NAME" -keystore ${HARMONY_BASE}/etc/ap-keystore.p12 -storepass "$SECURITY_KEYSTORE_PASSWORD" \
      -keypass "$SECURITY_KEYSTORE_PASSWORD" -validity 333 -keysize 3072 -dname "$SERVER_DN" 2>/dev/null
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
      -validity 333 -keysize 3072 -dname "$SERVER_DN" 2>/dev/null
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

  cp -n ${HARMONY_HOME}/setup/domibus.properties.template ${HARMONY_BASE}/etc/domibus.properties
  chmod 640 ${HARMONY_BASE}/etc/domibus.properties

  cp -n ${HARMONY_HOME}/setup/server.xml.template ${HARMONY_BASE}/conf/server.xml
  chmod 640 ${HARMONY_BASE}/conf/server.xml
  cp -n ${HARMONY_HOME}/conf/* ${HARMONY_BASE}/conf/
  ln -sfn ${HARMONY_BASE}/conf ${HARMONY_BASE}/etc/tomcat-conf

  cp -n ${HARMONY_HOME}/setup/clientauthentication.xml.template ${HARMONY_BASE}/etc/clientauthentication.xml
  chmod 640 ${HARMONY_BASE}/etc/clientauthentication.xml

  cp -r -n ${HARMONY_HOME}/internal ${HARMONY_BASE}/etc/
  cp -r -n ${HARMONY_HOME}/policies ${HARMONY_BASE}/etc/
  cp -n ${HARMONY_HOME}/setup/logback.xml ${HARMONY_BASE}/etc/

  ln -sfn ${HARMONY_HOME}/plugins/lib ${HARMONY_BASE}/etc/plugins/
  cp -r -n ${HARMONY_HOME}/plugins/config ${HARMONY_BASE}/etc/plugins/

  xmlstarlet edit --pf --inplace \
    --update '//Connector[@SSLEnabled="true"]/@keystorePass' --value "$TLS_KEYSTORE_PASSWORD" \
    --update '//Connector[@SSLEnabled="true"]/@keystoreFile' --value "$TLS_KEYSTORE_FILE" \
    --update '//Connector[@SSLEnabled="true"]/@truststorePass' --value "$TLS_TRUSTSTORE_PASSWORD" \
    ${HARMONY_BASE}/conf/server.xml

  xmlstarlet edit --pf --inplace \
    -N s='http://cxf.apache.org/configuration/security' \
    --update '//s:trustManagers/s:keyStore/@password' --value "$TLS_TRUSTSTORE_PASSWORD" \
    --update '//s:trustManagers/s:keyStore/@file' --value "/var/opt/harmony-ap/etc/tls-truststore.p12" \
    --update '//s:keyManagers/s:keyStore/@password' --value "$TLS_KEYSTORE_PASSWORD" \
    --update '//s:keyManagers/s:keyStore/@file' --value "$TLS_KEYSTORE_FILE" \
    ${HARMONY_BASE}/etc/clientauthentication.xml

  set_prop domibus.database.serverName "$DB_HOST"
  set_prop domibus.database.port       "$DB_PORT"
  set_prop domibus.database.schema     "$DB_SCHEMA"
  set_prop domibus.datasource.user     "$DB_USER"
  set_prop domibus.datasource.password "$DB_PASSWORD"

  set_prop domibus.security.keystore.password     "$SECURITY_KEYSTORE_PASSWORD"
  set_prop domibus.security.key.private.alias     "$PARTY_NAME"
  set_prop domibus.security.key.private.password  "$SECURITY_KEYSTORE_PASSWORD"
  set_prop domibus.security.truststore.password   "$SECURITY_TRUSTSTORE_PASSWORD"

  set_prop domibus.dynamicdiscovery.useDynamicDiscovery "${USE_DYNAMIC_DISCOVERY:-false}"
  set_prop domibus.smlzone "${SML_ZONE:-}"

  [[ $_DB_AVAILABLE == "true" ]] && echo "$HARMONY_VERSION" >$HARMONY_BASE/HARMONY_VERSION
  log "Configuration done"
else
  # always update database properies to make it possible to rotate DB credentials
  [[ -n $DB_HOST     ]] && set_prop 'domibus.database.serverName' "$DB_HOST"
  [[ -n $DB_PORT     ]] && set_prop 'domibus.database.port'       "$DB_PORT"
  [[ -n $DB_SCHEMA   ]] && set_prop 'domibus.database.schema'     "$DB_SCHEMA"
  [[ -n $DB_USER     ]] && set_prop 'domibus.datasource.user'     "$DB_USER"
  [[ -n $DB_PASSWORD ]] && set_prop 'domibus.datasource.password' "$DB_PASSWORD"
fi

if [[ $INIT = "true" ]]; then
  log "Just init requested, exiting..."
  exit 0;
fi

CATALINA_TMPDIR=/var/tmp/harmony-ap
CATALINA_HOME=${HARMONY_HOME}
CATALINA_BASE=${HARMONY_BASE}
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# escape \ and " in the password
_TLS_TRUSTSTORE_PASSWORD="${TLS_TRUSTSTORE_PASSWORD//\\/\\\\}"
_TLS_TRUSTSTORE_PASSWORD="${_TLS_TRUSTSTORE_PASSWORD//\"/\\\"}"

# @<(echo "...") is an on-the-fly generated Java command-line argument file
exec tini -e 143 -- $JAVA_HOME/bin/java @<(echo "\
-Xms256m -Xmx${MAX_MEM:-512m}
-XX:+UseParallelGC
-XX:+ExitOnOutOfMemoryError
-XX:MaxMetaspaceSize=256m
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED
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
-Ddomibus.work.location=${HARMONY_BASE}/work
-classpath $CATALINA_HOME/bin/bootstrap.jar:$CATALINA_HOME/bin/tomcat-juli.jar
") "$@" org.apache.catalina.startup.Bootstrap
