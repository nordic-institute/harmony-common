#!/bin/sh

export JAVA_OPTS="$JAVA_OPTS -Djava.protocol.handler.pkgs=org.apache.catalina.webresources \
   -Ddomibus.config.location=/etc/harmony-ap -Ddomibus.work.location=/opt/harmony-ap/work"

