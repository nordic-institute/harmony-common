#!/bin/bash

echo Running script

if [ -z "$1" ]
  then
    echo "Usage: generate_cert.sh <hostname>"
    exit
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

cleanup() {
  rm "$SCRIPT_DIR"/work/* 2> /dev/null
}

trap cleanup EXIT
pushd .

rm "$SCRIPT_DIR"/work/* 2> /dev/null


cd "$SCRIPT_DIR" || exit

touch ./work/neds-ca.index
openssl rand -hex 16 > ./work/neds-ca.serial

# generate key and certificate request
openssl req -newkey rsa:2048 -nodes -keyout  work/"$1".key.pem -out work/"$1".csr.pem -subj "/CN=$1"

# generate certificate
openssl ca -config neds-ca.cnf -batch -in work/"$1".csr.pem -out work/"$1".crt.pem -extensions server_ext

# move files
mv work/"$1".key.pem host_certs/
mv work/"$1".crt.pem host_certs/