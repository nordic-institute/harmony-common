#!/bin/bash
set -e
DIR="$(cd "$(dirname $0)" && pwd)"
cd "$DIR"
source ./_build_common.sh

TAG=$APVERSION
while getopts "t:" opt; do
  case "$opt" in
    t)
      TAG="$OPTARG"
      ;;
    *)
      echo "Unexpected argument $opt"
      exit 1;
      ;;
  esac
done
shift $((OPTIND-1))

prepare_commonbin
docker build \
  --build-arg VERSION="${APVERSION}" --build-arg MYSQLJ_VERSION="$MYSQLJ_VERSION" --build-arg BUILD_ID="${BUILD_ID:-local}" \
  -t niis/harmony-ap:"$TAG" \
  -t artifactory.niis.org/harmony-snapshot-docker/niis/harmony-ap:"$TAG" \
  -f ./ap/docker/Dockerfile ..

