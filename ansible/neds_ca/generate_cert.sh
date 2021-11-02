#!/bin/bash
set -e

if [ -z "$1" ]; then
  echo "Usage: generate_cert.sh <hostname>"
  exit
fi

SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)

cleanup() {
  rm "$SCRIPT_DIR"/work/* 2> /dev/null || true
  popd > /dev/null
}

gencert() {
  FILE_NAME=$1
  FQDN=$2

  if [ ! -f "host_certs/$FILE_NAME.key.pem" ] || [ ! -f "host_certs/$FILE_NAME.crt.pem" ]; then
    if [ -f "host/certs/$FILE_NAME.key.pem" ]; then
      echo "Removing orphan file $FILE_NAME.key.pem"
      rm "host_certs/$FILE_NAME.key.pem"
    fi
    if [ -f "host/certs/$FILE_NAME.crt.pem" ]; then
      echo Removing orphan file "$FILE_NAME.crt.pem"
      rm "host_certs/$FILE_NAME.crt.pem"
    fi
  else
    echo "Files for $FQDN already exist."
    exit
  fi

  rm "$SCRIPT_DIR"/work/* 2> /dev/null || true

  touch ./work/neds-ca.index
  openssl rand -hex 16 > ./work/neds-ca.serial

  # generate key and certificate request
  openssl req -newkey rsa:2048 -nodes -keyout  work/"$FILE_NAME".key.pem -out work/"$1".csr.pem \
    -subj "/CN=$FQDN/C=FI/O=NIIS"

  # generate certificate
  openssl ca -config neds-ca.cnf -batch -in work/"$FILE_NAME".csr.pem -out work/"$FILE_NAME".crt.pem \
    -extensions server_ext -startdate 211001000000Z

  # move files
  mv work/"$1".key.pem host_certs/
  mv work/"$1".crt.pem host_certs/
}

pushd . > /dev/null

trap cleanup EXIT
cd "$SCRIPT_DIR" || exit

gencert "$1" "$1"
gencert "$1_tls" "$1"

