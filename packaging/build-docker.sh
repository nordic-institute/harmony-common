#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname $0)" && pwd)"
cd "$DIR"
source ./_build_common.sh

S6_VERSION=3.2.1.0
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
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --pull \
  --load \
  --build-arg VERSION="${APVERSION}" \
  --build-arg BUILD_ID="${BUILD_ID:-local}" \
  --build-arg S6_VERSION="${S6_VERSION}" \
  -t niis/harmony-ap:"$TAG" \
  -t artifactory.niis.org/harmony-snapshot-docker/niis/harmony-ap:"$TAG" \
  -f ./ap/docker/Dockerfile ..
