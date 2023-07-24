#!/bin/bash

usage() {
  echo "Usage: build-packages-for-test.sh -a ap_repository -c common_repository -s smp_repository"
}

exit_abnormal() {
  usage
  exit 1
}

clone_repository() {
  git clone "$1"
  git clone "$2"
  git clone "$3"
}

build_packages() {
  cd harmony-common
  ./prepare_buildhost.sh
  ./build_packages.sh -s -d
  cp packaging/build/ubuntu20.04/harmony-ap*.deb packaging/build/ubuntu20.04/harmony-ap.deb

}

install_harmony() {
  apt install ./packaging/build/ubuntu20.04/harmony-ap.deb
}

while getopts ":a:c:s:" options; do
  case "${options}" in
    a )
      AP_REPO=${OPTARG}
      ;;  
    c )
      COMMON_REPO=${OPTARG}
      ;;          
    s )
      SMP_REPO=${OPTARG}
      ;;     
    \? )
        exit_abnormal
      ;;
  esac
done

if [[ $AP_REPO == "" ]] | [[ $COMMON_REPO == "" ]] | [[ $SMP_REPO == "" ]]; then
    exit_abnormal
fi

clone_repository "$AP_REPO" "$COMMON_REPO" "$SMP_REPO"
build_packages
