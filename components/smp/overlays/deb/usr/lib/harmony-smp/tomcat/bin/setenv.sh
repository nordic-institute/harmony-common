#!/bin/sh

if [ -z "${JAVA_HOME:-}" ]; then
  if command -v update-java-alternatives > /dev/null 2>&1; then
    JAVA_HOME="$(update-java-alternatives -l 2>/dev/null \
      | awk '$1 ~ /java-(1\.)?21[^0-9]/ { print $3; exit }')"
  fi

  if [ -z "${JAVA_HOME:-}" ]; then
    echo "harmony-smp: Java 21 is required but could not be found." >&2
    echo "  Install openjdk-21-jre-headless and retry." >&2
    exit 1
  fi

  export JAVA_HOME
fi

CATALINA_OPTS="${CATALINA_OPTS:-} \
  -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource,org.apache.tomcat.util.digester.SystemPropertySource \
  -Dlogback.configurationFile=${SMP_LOG_CONFIGURATION_FILE:-/etc/harmony-smp/logback.xml} \
  -Dsmp.log.folder=/var/log/harmony-smp"
