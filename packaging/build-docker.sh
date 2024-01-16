#!/bin/bash
set -e
DIR="$(cd "$(dirname $0)" && pwd)"
cd "$DIR"
source ./_build_common.sh

prepare_commonbin
docker build \
  --build-arg VERSION="${APVERSION}" --build-arg MYSQLJ_VERSION="$MYSQLJ_VERSION" --build-arg BUILD_ID="${BUILD_ID:-local}" \
  -t niis/harmony-ap:"$APVERSION" \
  -t artifactory.niis.org/harmony-snapshot-docker/niis/harmony-ap:"$APVERSION" \
  -f ./ap/docker/Dockerfile ..

