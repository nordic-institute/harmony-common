#!/bin/bash
set -e

# CHANGE VERSION NUMBERS BELOW AS NEEDED
# Version of Harmony Access Point
APVERSION="2.0.0"

# Version of Harmony SMP
SMPVERSION=2.0.0

# NO VERSIONING RELATED MODIFICATIONS ARE NECESSARY AFTER THIS POINT!

# DO NOT change variables below, instead if needed assign values externally
if [ -z "$HARMONY_AP_REPO_PATH" ]; then
  HARMONY_AP_REPO_PATH=../../harmony-access-point
fi

if [ -z "$HARMONY_SMP_REPO_PATH" ]; then
  HARMONY_SMP_REPO_PATH=../../harmony-smp
fi

function builddeb {
    local root="$1"
    local dist="$2"
    local suffix="$3"

    pushd "$(pwd)"
    cd "$root/$dist"
    cp ../generic/* debian/
    version="$(dpkg-parsechangelog -l../generic/changelog | sed -n -e 's/^Version: //p')"
    compat="$(cat debian/compat)"

    sed -i "s/\${debhelper-version}/$compat/" debian/control

    export DEB_BUILD_OPTIONS=release

    dch -b -v "$version.$suffix" "Build for $dist"
    dch --distribution $dist -r ""
    dpkg-buildpackage -tc -b -us -uc
    popd

    find $root -name "harmony*$suffix*.deb" -exec mv {} "build/$suffix/" \;
}

function prepare {
    mkdir -p "build/$1"
    rm -f "build/$1/"*.deb
}

DIR="$(cd "$(dirname $0)" && pwd)"
cd "$DIR"

rm -rf build/*

mkdir -p build/harmony
cp -r ap build/harmony
cp -r smp build/harmony

TOMCAT_VERSION=9.0.76
MYSQLJ_VERSION=8.0.33
cd commonbin
if [ ! -f tomcat-$TOMCAT_VERSION.tar.gz ]; then
  echo "Fetching tomcat 9"
  curl -O -s "https://repo1.maven.org/maven2/org/apache/tomcat/tomcat/$TOMCAT_VERSION/tomcat-$TOMCAT_VERSION.tar.gz"
fi

if [ ! -f mysql-connector-j-$MYSQLJ_VERSION.jar ]; then
  echo "Fetching mysql connector"
  curl -O -s "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$MYSQLJ_VERSION/mysql-connector-j-$MYSQLJ_VERSION.jar"
fi

cd "$DIR"

mkdir -p commonbin/tomcat9
tar -xvzf commonbin/tomcat-$TOMCAT_VERSION.tar.gz -C commonbin/tomcat9 --strip-components=1
rm commonbin/tomcat9/conf/logging.properties
rm commonbin/tomcat9/conf/server.xml

# explode domibus war
rm -rf commonbin/harmony-ap

mkdir -p commonbin/harmony-ap

unzip "$HARMONY_AP_REPO_PATH/Tomcat/Domibus-MSH-tomcat-distribution/target/harmony-MSH-tomcat-distribution-$APVERSION.war" -d commonbin/harmony-ap

# copy ws plugin jar
cp "$HARMONY_AP_REPO_PATH/Plugin-WS/Domibus-default-ws-plugin/target/harmony-default-ws-plugin-$APVERSION.jar" commonbin/ws-plugin.jar

# cleanup
rm -rf commonbin/harmony-smp

# explode smp war
mkdir -p commonbin/harmony-smp

unzip "$HARMONY_SMP_REPO_PATH/smp-webapp/target/harmonysmp.war" -d commonbin/harmony-smp

prepare ubuntu20.04
builddeb build/harmony/ap/ubuntu focal ubuntu20.04
builddeb build/harmony/smp/ubuntu focal ubuntu20.04
