#!/bin/bash
set -e

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


mkdir -p commonbin/tomcat9
tar -xvzf commonbin/apache-tomcat-9.0.52.tar.gz -C commonbin/tomcat9 --strip-components=1
rm commonbin/tomcat9/conf/logging.properties
rm commonbin/tomcat9/conf/server.xml

# copy domibus war and ws plugin jar
cp ../../domibus/Domibus-MSH-tomcat/target/domibus-MSH-tomcat-4.2.3.war commonbin/domibus.war
cp ../../domibus/Domibus-default-ws-plugin/target/domibus-default-ws-plugin-4.2.3.jar commonbin/ws-plugin.jar

prepare ubuntu20.04
builddeb build/harmony/ubuntu focal ubuntu20.04
