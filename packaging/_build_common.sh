#!/bin/bash
set -euo pipefail

# CHANGE VERSION NUMBERS BELOW AS NEEDED
# Version of Harmony Access Point
APVERSION=2.6.1
# Version of Harmony SMP
SMPVERSION=2.2.0

TOMCAT_VERSION=9.0.118
MYSQLJ_VERSION=8.4.0
MARIADBJ_VERSION=2.7.13

# NO VERSIONING RELATED MODIFICATIONS ARE NECESSARY AFTER THIS POINT!

# DO NOT change variables below, instead if needed assign values externally
if [ -z "${HARMONY_AP_REPO_PATH:-}" ]; then
  HARMONY_AP_REPO_PATH=../../harmony-access-point
fi

if [ -z "${HARMONY_SMP_REPO_PATH:-}" ]; then
  HARMONY_SMP_REPO_PATH=../../harmony-smp
fi

fetch_jdbc_drivers() {
  if [ ! -d commonbin/jdbc-drivers ]; then
    mkdir -p commonbin/jdbc-drivers
  fi

  if [ ! -f commonbin/jdbc-drivers/mysql-connector-j-$MYSQLJ_VERSION.jar ]; then
    echo "Fetching MySQL connector $MYSQLJ_VERSION"
    rm -f commonbin/jdbc-drivers/mysql-connector-j-*.jar
    curl -s -o commonbin/jdbc-drivers/mysql-connector-j-$MYSQLJ_VERSION.jar "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$MYSQLJ_VERSION/mysql-connector-j-$MYSQLJ_VERSION.jar"
  fi

  if [ ! -f commonbin/jdbc-drivers/mariadb-connector-j-$MARIADBJ_VERSION.jar ]; then
    echo "Fetching MariaDB connector $MARIADBJ_VERSION"
    rm -f commonbin/jdbc-drivers/mariadb-connector-j-*.jar
    curl -s -o commonbin/jdbc-drivers/mariadb-connector-j-$MARIADBJ_VERSION.jar "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/$MARIADBJ_VERSION/mariadb-java-client-$MARIADBJ_VERSION.jar"
  fi
}

fetch_tomcat() {
  if [ ! -d commonbin/tomcat ]; then
    mkdir -p commonbin/tomcat
  fi

  if [ ! -f commonbin/tomcat/tomcat-$TOMCAT_VERSION.tar.gz ]; then
    echo "Fetching Tomcat $TOMCAT_VERSION"
    curl -s -o commonbin/tomcat/tomcat-$TOMCAT_VERSION.tar.gz "https://repo1.maven.org/maven2/org/apache/tomcat/tomcat/$TOMCAT_VERSION/tomcat-$TOMCAT_VERSION.tar.gz"

    tar -xzf commonbin/tomcat/tomcat-$TOMCAT_VERSION.tar.gz -C commonbin/tomcat --strip-components=1
    rm commonbin/tomcat/conf/logging.properties
    rm commonbin/tomcat/conf/server.xml
  fi
}

prepare_commonbin() {
  fetch_jdbc_drivers
  fetch_tomcat

  # explode domibus war
  if [[ -d $HARMONY_AP_REPO_PATH ]]; then
    rm -rf commonbin/harmony-ap
    mkdir -p commonbin/harmony-ap
    unzip -q "$HARMONY_AP_REPO_PATH/Tomcat/Domibus-MSH-tomcat-distribution/target/harmony-MSH-tomcat-distribution-$APVERSION.war" -d commonbin/harmony-ap
    # copy ws plugin jar
    cp "$HARMONY_AP_REPO_PATH/Plugin-WS/Domibus-default-ws-plugin/target/harmony-default-ws-plugin-$APVERSION.jar" commonbin/ws-plugin.jar
  fi

  # explode smp war
  if [[ -d $HARMONY_SMP_REPO_PATH ]]; then
    rm -rf commonbin/harmony-smp
    mkdir -p commonbin/harmony-smp
    unzip -q "$HARMONY_SMP_REPO_PATH/smp-webapp/target/harmonysmp.war" -d commonbin/harmony-smp
  fi
}
