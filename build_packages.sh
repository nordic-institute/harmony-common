#!/bin/bash
set -e

HAS_DOCKER=""

errorExit() {
    echo "*** $*" 1>&2
    exit 1
}

usage () {
    echo "Usage: $0 [option for $0...] [other options]"
    echo "Options for $0:"
    echo " -s, --skip-tests      Skip tests, just compile code and build packages"
    echo " -d, --docker-compile  Compile in Docker container instead of native Maven build"
    echo " -h, --help            This help text."
    echo "The option for $0, if present, must come fist, before other options."
    test -z "$1" || exit "$1"
}

for i in "$@"; do
case "$i" in
    "--skip-tests"|"-s")
        SKIP_TESTS=1
        ;;
esac
done

AP_ARGUMENTS=(-Ptomcat -Pdefault-plugins -Pdatabase -PUI)
SMP_ARGUMENTS=()

if [[ -n "$SKIP_TESTS" ]]; then
    echo "Skipping tests"...
    AP_ARGUMENTS+=(-DskipTests=true -DskipITs=true)
    SMP_ARGUMENTS+=(-DskipTests=true -DskipITs=true)
fi

buildInDocker() {
    test -n "$HAS_DOCKER" || errorExit "Error, docker is not installed/running."
    echo "Building in docker..."
}

buildLocally() {
    echo "Building locally..."
    # Compile code
    cd ../harmony-access-point/
    mvn clean -f neds-pom.xml install "${AP_ARGUMENTS[@]}"
    cd ../harmony-smp/
    mvn clean -f neds-pom.xml install "${SMP_ARGUMENTS[@]}"

    # Build packages
    cd ../harmony-common/packaging/
    ./build-deb.sh
}

if command -v docker &>/dev/null; then
    HAS_DOCKER=true
fi

case "$1" in
    --docker-compile|-d) buildInDocker "$@";;
    --help|-h) usage 0;;
    *) buildLocally "$@";;
esac
