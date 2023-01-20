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
    echo " -d, --docker-compile  Compile in Docker container instead of native Maven build."
    echo " -h, --help            This help text."
    echo "The option for $0, if present, must come fist, before other options."
    echo "Other options:"
    echo " -s, --skip-tests      Skip tests, just compile code and build packages."
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
SMP_ARGUMENTS=(-Duser.timezone=CET)

if [[ -n "$SKIP_TESTS" ]]; then
    echo "Skipping tests"...
    AP_ARGUMENTS+=(-DskipTests=true -DskipITs=true)
    SMP_ARGUMENTS+=(-DskipTests=true -DskipITs=true)
fi

buildInDocker() {
    test -n "$HAS_DOCKER" || errorExit "Error, docker is not installed/running."
    echo "Building in docker..."

    # Build Docker image for compiling the code
    docker build -q -f docker/Dockerfile-compile -t harmony-compile --build-arg uid=$(id -u) --build-arg gid=$(id -g) .

    # Compile AP
    docker run -it --rm \
       --user builder \
       -v "$(pwd)/../harmony-access-point/":/mnt \
       harmony-compile \
       mvn clean -f harmony-pom.xml install "${AP_ARGUMENTS[@]}"

    # Compile SMP
    docker run -it --rm \
       -u builder \
       -v "$(pwd)/../harmony-smp/":/mnt \
       harmony-compile \
       mvn clean -f harmony-pom.xml install "${SMP_ARGUMENTS[@]}"

    # Build Docker image for the build
    docker build -q -f docker/Dockerfile-build -t harmony-build --build-arg uid=$(id -u) --build-arg gid=$(id -g) .

    # Build packages using the image
     docker run -it --rm \
       -u builder \
       -v "$(pwd)/..":/mnt \
       harmony-build \
       /bin/bash -c "cd /mnt/harmony-common/packaging/ && ./build-deb.sh"
}

buildLocally() {
    echo "Building locally..."
    # Compile code
    cd ../harmony-access-point/
    mvn clean -f harmony-pom.xml install "${AP_ARGUMENTS[@]}"
    cd ../harmony-smp/
    mvn clean -f harmony-pom.xml install "${SMP_ARGUMENTS[@]}"

    # Build packages
    cd ../harmony-common/packaging/
    ./build-deb.sh
}

if command -v docker &>/dev/null; then
    HAS_DOCKER=true
fi

case "$1" in
    --docker-compile|-d) shift; buildInDocker "$@";;
    --help|-h) usage 0;;
    *) buildLocally "$@";;
esac
