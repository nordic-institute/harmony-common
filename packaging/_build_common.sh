#!/bin/bash
set -e
# CHANGE VERSION NUMBERS BELOW AS NEEDED
# Version of Harmony Access Point
APVERSION="2.2.0"
# Version of Harmony SMP
SMPVERSION=2.1.0

TOMCAT_VERSION=9.0.80
MYSQLJ_VERSION=8.0.33

# NO VERSIONING RELATED MODIFICATIONS ARE NECESSARY AFTER THIS POINT!

# DO NOT change variables below, instead if needed assign values externally
if [ -z "$HARMONY_AP_REPO_PATH" ]; then
  HARMONY_AP_REPO_PATH=../../harmony-access-point
fi

if [ -z "$HARMONY_SMP_REPO_PATH" ]; then
  HARMONY_SMP_REPO_PATH=../../harmony-smp
fi

prepare_commonbin() {

  if [ ! -f commonbin/mysql-connector-j-$MYSQLJ_VERSION.jar ]; then
    echo "Fetching mysql connector"
    curl -s -o commonbin/mysql-connector-j-$MYSQLJ_VERSION.jar "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$MYSQLJ_VERSION/mysql-connector-j-$MYSQLJ_VERSION.jar"
  fi

  if [ ! -f commonbin/tomcat-$TOMCAT_VERSION.tar.gz ]; then
    echo "Fetching tomcat 9"
    curl -s -o commonbin/tomcat-$TOMCAT_VERSION.tar.gz "https://repo1.maven.org/maven2/org/apache/tomcat/tomcat/$TOMCAT_VERSION/tomcat-$TOMCAT_VERSION.tar.gz"
  fi

  mkdir -p commonbin/tomcat9
  tar -xzf commonbin/tomcat-$TOMCAT_VERSION.tar.gz -C commonbin/tomcat9 --strip-components=1
  rm commonbin/tomcat9/conf/logging.properties
  rm commonbin/tomcat9/conf/server.xml

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
