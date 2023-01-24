#!/bin/sh

if [ -d /usr/lib/jvm/java-11-openjdk-amd64 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
else
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
fi