#!/bin/sh

if [ -z "${JAVA_HOME:-}" ]; then
  if command -v update-java-alternatives > /dev/null 2>&1; then
    JAVA_HOME="$(update-java-alternatives -l 2>/dev/null \
      | awk '$1 ~ /java-(1\.)?11[^0-9]/ { print $3; exit }')"
  fi

  if [ -z "${JAVA_HOME:-}" ]; then
    echo "harmony-ap: Java 11 is required but could not be found." >&2
    echo "  Install openjdk-11-jre-headless and retry." >&2
    exit 1
  fi

  export JAVA_HOME
fi

CATALINA_OPTS="${CATALINA_OPTS:-} \
  -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource,org.apache.tomcat.util.digester.SystemPropertySource \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
  --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens=java.management/sun.management=ALL-UNNAMED \
  --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
  --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
  -Dlogback.configurationFile=${LOGBACK_CONFIG_FILE:-/etc/harmony-ap/logback.xml} \
  -Ddomibus.config.location=${HARMONY_CONFIG:-/etc/harmony-ap} \
  -Ddomibus.work.location=${ACTIVEMQ_WORK_DIR:-/var/lib/harmony-ap/broker} \
  -Ddomibus.extensions.location=${HARMONY_ADDONS:-/usr/local/lib/harmony-ap},/usr/share/harmony-ap"
