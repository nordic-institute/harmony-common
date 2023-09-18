#!/bin/bash
set -e

DIR="$(cd "$(dirname $0)" && pwd)"
cd "$DIR"

source ./_build_common.sh

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

prepare_commonbin

rm -rf build/*
mkdir -p build/harmony
cp -r ap build/harmony
cp -r smp build/harmony

prepare ubuntu20.04
builddeb build/harmony/ap/ubuntu focal ubuntu20.04
builddeb build/harmony/smp/ubuntu focal ubuntu20.04

prepare ubuntu22.04
builddeb build/harmony/ap/ubuntu jammy ubuntu22.04
builddeb build/harmony/smp/ubuntu jammy ubuntu22.04
