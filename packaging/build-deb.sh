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

if [ ! -f commonbin/apache-tomcat-9.0.52.tar.gz ]; then
  echo "Fetching tomcat 9"
  wget -Pcommonbin/ https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.52/bin/apache-tomcat-9.0.52.tar.gz
fi

if [ ! -f commonbin/mysql-connector-java-8.0.26.jar ]; then
  echo "Fetching mysql connector"
  wget -Pcommonbin/ https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.26/mysql-connector-java-8.0.26.jar
fi


mkdir -p commonbin/tomcat9
tar -xvzf commonbin/apache-tomcat-9.0.52.tar.gz -C commonbin/tomcat9 --strip-components=1
rm commonbin/tomcat9/conf/logging.properties
rm commonbin/tomcat9/conf/server.xml

# copy domibus war and ws plugin jar
cp ../../domibus/Domibus-MSH-tomcat/target/domibus-MSH-tomcat-4.2.3.war commonbin/domibus.war
cp ../../domibus/Domibus-default-ws-plugin/target/domibus-default-ws-plugin-4.2.3.jar commonbin/ws-plugin.jar

# copy smp war
cp ../../smp/smp-webapp/target/smp.war commonbin/smp.war

prepare ubuntu20.04
builddeb build/harmony/ubuntu focal ubuntu20.04
