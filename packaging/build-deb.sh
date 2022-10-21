#!/bin/bash
set -e

# CHANGE VERSION NUMBERS BELOW AS NEEDED
# Version of Harmony Access Point
APVERSION=1.3.0

# Version of Harmony SMP
SMPVERSION=1.3.0

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
cp -a ubuntu build/harmony

cd commonbin
if [ ! -f apache-tomcat-9.0.52.tar.gz ]; then
  echo "Fetching tomcat 9"
  curl -O -s https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.52/bin/apache-tomcat-9.0.52.tar.gz
fi

if [ ! -f mysql-connector-java-8.0.26.jar ]; then
  echo "Fetching mysql connector"
  curl -O -s https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.26/mysql-connector-java-8.0.26.jar
fi

cd "$DIR"

mkdir -p commonbin/tomcat9
tar -xvzf commonbin/apache-tomcat-9.0.52.tar.gz -C commonbin/tomcat9 --strip-components=1
rm commonbin/tomcat9/conf/logging.properties
rm commonbin/tomcat9/conf/server.xml

# explode domibus war
rm -rf commonbin/harmony-ap

mkdir -p commonbin/harmony-ap

unzip "$HARMONY_AP_REPO_PATH/Domibus-MSH-tomcat/target/harmony-MSH-tomcat-$APVERSION.war" -d commonbin/harmony-ap

# copy ws plugin jar
cp "$HARMONY_AP_REPO_PATH/Domibus-default-ws-plugin/target/harmony-default-ws-plugin-$APVERSION.jar" commonbin/ws-plugin.jar

# cleanup
rm -rf commonbin/harmony-smp

# explode smp war
mkdir -p commonbin/harmony-smp

unzip "$HARMONY_SMP_REPO_PATH/smp-webapp/target/harmonysmp-$SMPVERSION.war" -d commonbin/harmony-smp

prepare ubuntu20.04
builddeb build/harmony/ubuntu focal ubuntu20.04
