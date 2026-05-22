#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname $0)" && pwd)"
cd "$DIR"
source ./_build_common.sh

S6_VERSION=3.2.1.0
TAG=$APVERSION
PLATFORMS="linux/amd64,linux/arm64"
OUTPUT_MODE="load"
BUILDER=""
EXTRA_TAGS=()

while getopts "t:p:o:b:T:" opt; do
  case "$opt" in
    t) TAG="$OPTARG" ;;
    p) PLATFORMS="$OPTARG" ;;
    o) OUTPUT_MODE="$OPTARG" ;;
    b) BUILDER="$OPTARG" ;;
    T) EXTRA_TAGS+=("-t" "$OPTARG") ;;
    *) echo "Invalid option" >&2; exit 1 ;;
  esac
done
shift $((OPTIND-1))

case "$OUTPUT_MODE" in
  load) OUTPUT_FLAG="--load" ;;
  push) OUTPUT_FLAG="--push" ;;
  *)
    echo "Invalid -o value: '$OUTPUT_MODE' (expected: load|push)" >&2
    exit 1
    ;;
esac

if [ "${#EXTRA_TAGS[@]}" -eq 0 ]; then
  TAGS=(
    "-t" "niis/harmony-ap:$TAG"
    "-t" "artifactory.niis.org/harmony-snapshot-docker/niis/harmony-ap:$TAG"
  )
else
  TAGS=("${EXTRA_TAGS[@]}")
fi

prepare_commonbin
docker buildx build \
  ${BUILDER:+--builder "$BUILDER"} \
  --platform "$PLATFORMS" \
  --pull \
  $OUTPUT_FLAG \
  --build-arg VERSION="${APVERSION}" \
  --build-arg BUILD_ID="${BUILD_ID:-local}" \
  --build-arg S6_VERSION="${S6_VERSION}" \
  "${TAGS[@]}" \
  -f ./ap/docker/Dockerfile ..
