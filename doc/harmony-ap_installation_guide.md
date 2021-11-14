# Harmony eDelivery Access - Access Point Installation Guide

This guide describes installation and postinstallation procedures for  Harmony eDelivery Access Access Point,
referenced below as AP.

## Installation

### 2.1 Installation prerequisites

AP installation assumes existing Ubuntu 20.04 installation. Packages are tested on x86-64 platform but as this is a 
100% Java solution installation and usage may be possible on other architectures.

If MySQL database engine is already installed then installation requires that root user can access database using UNIX
socket peer authentication without password.

### 2.2 Setup Package Repository

Add the Harmony repository’s signing key to the list of trusted keys:
```
curl https://artifactory.niis.org/api/gpg/key/public | sudo apt-key add -
```

Add Harmony package repository 
```
sudo apt-add-repository -y "deb https://artifactory.niis.org/harmony-release-deb $(lsb_release -sc)-current main"
```

### 2.3 Package Installation

Update package repository metadata:
```
sudo apt update
```

Issue the following command to install the Harmony eDelivery Access Access Point:
```
sudo apt install harmony-ap
```

Upon the first installation of the access point, the system asks for the following information.

- whether you want your AP installation to use dynamic discovery
  
  If yes then: 
  - SML zone that you want to use 
- user name of administrative SMP user - username to use to log in to SMP administrative UI
- initial password for administrative SMP user
- Common Name for generated self signed content and transport certificates

### 2.4 Changes made to system during package installation

In addition to installing needed dependencies, installation scripts:
- creates linux user `harmony` that is used to run AP service
- creates MySQL database user `harmony` and generates random password for it
- creates MySQL database schema `harmony_ap` and populates it with needed metadata
- loads initial configuration into database
- generates self-signed certificates for content encryption and for transport encryption
- installs systemd service `harmony-ap` but does not enable or start it

### 2.5 Location of configuration and generated passwords 

All AP configuration files are located at /etc/harmony-ap.

Installation scripts generate multiple random passwords.

| Password purpose | Password location |
|---|---|
| MySQL user harmony password  | /etc/harmony-ap/domibus.properties, keys domibus.datasource.xa.property.password and domibus.datasource.password |
| Content encryption keystore (/etc/harmony-ap/ap-keystore.jks) password | /etc/harmony-ap/domibus.properties, keys domibus.security.keystore.password and domibus.security.key.private.password. Content of this keystore can be changed using UI|
| Content encryption truststore (/etc/harmony-ap/ap-truststore.jks) password | /etc/harmony-ap/domibus.properties, key domibus.security.truststore.password. Content of this keystore can be changed using UI|
| TLS keystore (/etc/harmony-ap/tls-keystore.jks) password | /etc/harmony-ap/conf/server.xml |

### 2.6 Starting harmony-ap service and enabling automatic startup 

Before starting harmony-ap service the first time please do any necessary configuration changes.

To start harmony-ap service issue the following command:
```
sudo systemctl start harmony-ap
```

If you want harmony-smp service start at system startup issue the following command:
```
sudo apt enable harmony-ap
```