# Harmony eDelivery Access - Service Metadata Publisher Installation Guide

This guide describes installation and postinstallation procedures for  Harmony eDelivery Access Service Metadata Publisher,
referenced below as SMP.

## 1 Installation

### 1.1 Installation prerequisites

SMP installation assumes existing Ubuntu 20.04 installation. Packages are tested on x86-64 platform but as this is a 
100% Java solution installation and usage may be possible on other architectures.

If MySQL database engine is already installed then installation requires that root user can access database using UNIX
socket peer authentication - ie without password.

### 1.2 Setup Package Repository

Add the Harmony repository’s signing key to the list of trusted keys:
```
curl https://artifactory.niis.org/api/gpg/key/public | sudo apt-key add -
```

Add Harmony package repository 
```
sudo apt-add-repository -y "deb https://artifactory.niis.org/harmony-release-deb $(lsb_release -sc)-current main"
```

### 1.3 Package Installation

Update package repository metadata:
```
sudo apt update
```

Issue the following command to install the Harmony eDelivery Access Service Metadata Publisher:
```
sudo apt install harmony-smp
```

Upon the first installation of the service metadata publisher, the system asks for the following information.

- Distinguished Name for generated self signed content and transport certificates
- whether you want your SMP installation to publish information to some Service Metadata Locator (SML)
  
  If yes then: 
  - full URL of SML server used 
  - full URL of this SMP server as seen from public internet
  - IP address of this SMP server, reachable from public internet
- user name of administrative SMP user - username to use to log in to SMP administrative UI
- initial password for administrative SMP user

### 1.4 Changes made to system during package installation

In addition to installing needed dependencies, installation scripts:
- creates linux user `harmony_smp` that is used to run SMP service
- creates MySQL database user `harmony_smp` and generates random password for it
- creates MySQL database schema `harmony_smp` and populates it with needed metadata
- loads initial configuration into database
- generates self-signed certificates for content encryption and for transport encryption
- installs systemd service `harmony-smp` but does not enable or start it

## 2 Post-installation steps

### 2.1 Location of configuration and generated passwords 

All SMP configuration files are located at /etc/harmony-smp.

Installation scripts generate multiple random passwords.

| Password purpose | Password location |
|---|---|
| MySQL user harmony_smp password  | /etc/harmony-smp/tomcat-conf/context.xml |
| Content encryption keystore password | MySQL database table SMP_CONFIGURATION with key 'smp.keystore.password'. Note that when service is started password will be encrypted. Content of this keystore can be changed using UI.|
| TLS keystore password | /etc/harmony-smp/tomcat-conf/server.xml |

### 2.2 Starting harmony-smp service and enabling automatic startup 

Before starting harmony-smp service the first time please do any necessary configuration changes.

To start harmony-smp service issue the following command:
```
sudo systemctl start harmony-smp
```

If you want harmony-smp service start at system startup issue the following command:
```
sudo apt enable harmony-smp
```