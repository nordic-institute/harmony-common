#!/bin/sh
export JAVA_HOME=$(update-java-alternatives -l | grep -E "^(java-1.8|java-1.11)" | head -n 1 | tr -s " " | cut -d " " -f 3)

if [ -z $JAVA_HOME ]; then
  echo 'Java 8 or 11 required. Please install one of them and try again.'
  exit 1
fi
