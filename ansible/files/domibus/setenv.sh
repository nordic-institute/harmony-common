#!/bin/sh

# enable next line for debugging
#JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
export JAVA_OPTS="$JAVA_OPTS -Djava.protocol.handler.pkgs=org.apache.catalina.webresources -Xms128m -Xmx1024m -Ddomibus.config.location=/opt/tomcat/conf/domibus"

