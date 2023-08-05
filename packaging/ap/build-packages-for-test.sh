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
  chmod a+x prepare_buildhost.sh
  chmod a+x build_packages.sh
  ./prepare_buildhost.sh
  npm install -g npm@9.8.1
  ./build_packages.sh -s -d
  cp packaging/build/ubuntu20.04/harmony-ap*.deb packaging/build/ubuntu20.04/harmony-ap.deb

}

install_harmony() {
  export DEBIAN_FRONTEND="noninteractive"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/usedynamicdiscovery boolean $USE_DYNAMIC"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/adminuser string $ADM_USER"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/adminpassword password $ADM_PASS"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/serverdn string $SERVER_DN"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/partyname string $PARTY_NAME"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/tomcatport string $TOMCAT_PORT"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/database-host string $DB_HOST"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/database-port string $DB_PORT"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/database-schema string $DB_SCHEMA"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/database-user string $DB_USER"
  sudo debconf-set-selections <<< "harmony-ap harmony-ap/database-password password $DB_PASS"
  sudo apt install -y ./packaging/build/ubuntu20.04/harmony-ap.deb
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
install_harmony
