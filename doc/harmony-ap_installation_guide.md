# Harmony eDelivery Access - Access Point Installation Guide <!-- omit in toc -->

Version: 1.2  
Doc. ID: IG-AP

---

## Version history <!-- omit in toc -->

 Date       | Version | Description                                                             | Author
 ---------- | ------- | ------------------------------------------------------------------------| --------------------
 15.11.2021 | 1.0     | Initial version                                                         |
 07.01.2022 | 1.1     | Add reference to the Static Discovery Configuration Guide \[UG-SDCG\]   | Petteri Kivimäki
 08.01.2022 | 1.2     | Add TLS truststore to section [2.10](#210-location-of-configuration-and-generated-passwords) | Petteri Kivimäki
 
## License <!-- omit in toc -->

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>
 
## Table of Contents <!-- omit in toc -->

- [License](#license)
- [1 Introduction](#1-introduction)
  - [1.1 Target Audience](#11-target-audience)
  - [1.2 Terms and abbreviations](#12-terms-and-abbreviations)
  - [1.3 References](#13-references)
- [2 Installation](#2-installation)
  - [2.1 Prerequisites to Installation](#21-prerequisites-to-installation)
  - [2.2 Network Diagram](#22-network-diagram)
  - [2.3 Requirements for the Access Point](#23-requirements-for-the-access-point)
  - [2.4 Setup Package Repository](#24-setup-package-repository)
  - [2.5 Access Point Installation](#25-access-point-installation)
  - [2.6 Starting harmony-ap service and enabling automatic startup](#26-starting-harmony-ap-service-and-enabling-automatic-startup)
  - [2.7 Post-Installation Checks](#27-post-installation-checks)
  - [2.8 Installing Custom Plugins](#28-installing-custom-plugins)
  - [2.9 Changes made to system during installation](#29-changes-made-to-system-during-installation)
  - [2.10 Location of configuration and generated passwords](#210-location-of-configuration-and-generated-passwords)
 
## 1 Introduction

Harmony eDelivery Access Access Point is an AS4 Access Point for joining eDelivery policy domains. The Access Point is based on the Domibus open source project by the European Commission.

### 1.1 Target Audience

This guide describes installation and post-installation procedures for Harmony eDelivery Access Access Point.

The intended audience of this Installation Guide are Access Point system administrators responsible for installing and using the Access Point software.

The document is intended for readers with a moderate knowledge of Linux server management, computer networks, and the eDelivery working principles.

### 1.2 Terms and abbreviations

See eDelivery definitions documentation \[[TERMS](#Ref_TERMS)\].

### 1.3 References

1. <a id="Ref_TERMS" class="anchor"></a>\[TERMS\] CEF Definitions - eDelivery Definitions, <https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/CEF+Definitions#CEFDefinitions-eDeliveryDefinitions>
2. <a id="Ref_DOMIBUS_ADMIN_GUIDE" class="anchor"></a>\[DOMIBUS_ADMIN_GUIDE\] Access Point Administration Guide - Domibus 4.2.5, <https://ec.europa.eu/cefdigital/wiki/download/attachments/447677321/%28eDelivery%29%28AP%29%28AG%29%284.2.5%29%288.9.6%29.pdf>
3. <a id="Ref_WS_PLUGIN" class="anchor"></a>\[WS_PLUGIN\] Access Point Interface Control Document - WS Plugin, <https://ec.europa.eu/cefdigital/wiki/download/attachments/447677321/%28eDelivery%29%28AP%29%28ICD%29%28WS%20plugin%29%281.7%29.pdf>
4. <a id="Ref_PLUGIN_COOKBOOK" class="anchor"></a>\[PLUGIN_COOKBOOK\] Domibus Plugin Cookbook, <https://ec.europa.eu/cefdigital/wiki/download/attachments/447677321/%28eDelivery%29%28AP%29%28Plugin-CB%29%28Domibus.4.2.5%29%284.5%29.pdf>
5. <a id="Ref_UG-DDCG" class="anchor"></a>\[UG-DDCG\] Harmony eDelivery Access - Dynamic Discovery Configuration Guide. Document ID: [UG-DDCG](dynamic_discovery_configuration_guide.md)
6. <a id="Ref_UG-SDCG" class="anchor"></a>\[UG-SDCG\] Harmony eDelivery Access - Static Discovery Configuration Guide. Document ID: [UG-SDCG](static_discovery_configuration_guide.md)

## 2. Installation

### 2.1 Prerequisites to Installation

The Access Point is officially supported on the following platforms:

* Ubuntu Server 20.04 Long-Term Support (LTS) operating system on a x86-64 platform.

The software can be installed both on physical and virtualized hardware.

*Note*: If MySQL database engine is already installed then installation requires that `root` user can access database using UNIX
socket peer authentication without password.

*Note*: Access Point and SMP must not be installed on the same host because they both use port `8443`.

### 2.2 Network Diagram

The network diagram below provides an example of an Access Point setup when dynamic discovery is used.

![network diagram](img/ig-as_network_diagram.svg)

The table below lists the required connections between different components.

**Connection Type** | **Source** | **Target** | **Target Ports** | **Protocol** | **Note** |
-----------|------------|-----------|-----------|-----------|-----------|
Out | Access Point | Data Exchange Partner Access Point | 443, 8443, other | tcp | |
Out | Access Point | SMP | 443, 8443, other | tcp | |
Out | Access Point | Backend (push) | 80, 443, other | tcp | Target in the internal network |
In  | Data Exchange Partner Access Point | Access Point | 8443 | tcp | URL path: `/services/msh` |
In | Backend (submit, pull) | Access Point | 8443 | tcp | Source in the internal network<br /><br />URL path: `/services/backend` |
In | Admin | Access Point | 8443 | tcp | Source in the internal network<br /><br />URL path: `/` |

It is strongly recommended to protect the Access Point from unwanted access using a firewall (hardware or software based). The firewall can be applied to both incoming and outgoing connections depending on the security requirements of the environment where the Access Point is deployed. It is recommended to allow incoming traffic to specific ports only from explicitly defined sources using IP filtering. **Special attention should be paid with the firewall configuration since incorrect configuration may leave the Access Point vulnerable to exploits and attacks.**

In addition, it's strongly recommended to use URL path filtering for the Access Point since the admin UI, backend interface and AS4 interface all run on port `8443`.

**Port** | **URL Path** | **Description** |
---------|----------|-----------------|
 8443    | `/` | Admin UI for managing the Access Point. |
 8443    | `/services/backend` |  Webservice interface (submit requests, pull messages) between the Access Point and backend. |
 8443    | `/services/msh` | AS4 interface between Access Points. | 

### 2.3 Requirements for the Access Point

Minimum recommended hardware parameters:

* the server’s hardware (motherboard, CPU, network interface cards, storage system) must be supported by Ubuntu in general;
* a 64-bit dual-core Intel, AMD or compatible CPU; AES instruction set support is highly recommended;
* 4 GB RAM;
* a 100 Mbps network interface card.

Requirements to software and settings:

* an installed and configured Ubuntu 20.04 LTS x86-64 operating system;
* if the Access Point is separated from other networks by a firewall and/or NAT, the necessary connections to and from the Access Point are allowed;
* if the Access Point has a private IP address, a corresponding NAT record must be created in the firewall;
* enabling auxiliary services which are necessary for the functioning and management of the operating system (such as DNS, NTP, and SSH) stay outside the scope of this guide.

### 2.4 Setup Package Repository

Add the Harmony eDelivery Access repository’s signing key to the list of trusted keys:
```bash
curl https://artifactory.niis.org/api/gpg/key/public | sudo apt-key add -
```

The repository key details:

- Hash: `935CC5E7FA5397B171749F80D6E3973B`
- Fingerprint: `A01B FE41 B9D8 EAF4 872F  A3F1 FB0D 532C 10F6 EC5B`
- 3rd party key server: [Ubuntu key server](https://keyserver.ubuntu.com/pks/lookup?search=0xfb0d532c10f6ec5b&fingerprint=on&op=index)

Add Harmony eDelivery Access package repository:
```bash
sudo apt-add-repository -y "deb https://artifactory.niis.org/harmony-release-deb $(lsb_release -sc)-current main"
```

Update package repository metadata:
```bash
sudo apt update
```

### 2.5 Access Point Installation

Issue the following command to install the Harmony eDelivery Access Access Point:
```bash
sudo apt install harmony-ap
```

Upon the first installation of the Access Point, the system asks for the following information.

- do you want the Access Point installation to use dynamic discovery:
  - if yes: SML zone that you want to use;
  - if you're not sure about the correct value, please contact the domain authority of the policy domain where the Access Point is registered;
  - the value can be edited later by changing the `domibus.smlzone` property in the `/etc/harmony-ap/domibus.properties` configuration file;
- username of the administrative user - username to use to log in to administrative UI;
- initial password for the administrative user;
- `Distinguished Name` for generated self-signed content encryption and TLS certificates;
  - For example: `CN=example.com, O=My Organisation, C=FI`;
  - *Note:* different eDelivery policy domains may have different requirements for the `Distinguished Name`. If you're not sure about the requirements, please contact the domain authority of the policy domain where the Access Point is registered.

See the Static Discovery Configuration Guide \[[UG-SDCG](static_discovery_configuration_guide.md)\] and the Dynamic Discovery Configuration Guide \[[UG-DDCG](dynamic_discovery_configuration_guide.md)\] for more information about how to configure different discovery options.

### 2.6 Starting harmony-ap Service and Enabling Automatic Startup 

To start `harmony-ap` service issue the following command:
```bash
sudo systemctl start harmony-ap
```

If you want `harmony-ap` service start at system startup issue the following command:
```bash
sudo systemctl enable harmony-ap
```

### 2.7 Post-Installation Checks

Ensure that the `harmony-ap` service is in the `running` state (example output follows):
  ```bash
  sudo systemctl list-units "harmony-ap*"

  UNIT                           LOAD   ACTIVE SUB     DESCRIPTION
  harmony-ap.service             loaded active running Harmony eDelivery Access - Access Point
  ```

Ensure that the administrative user interface at `https://<host>:8443/` can be opened in a Web browser. To log in, use the administrative username and password chosen during the installation. While the user interface is still starting up, the Web browser may display a connection refused -error.

### 2.8 Installing Custom Plugins

The Access Point comes with one default plugin - the Web Service (WS) Plugin. See the WS Plugin documentation \[[WS_PLUGIN](#Ref_WS_PLUGIN)\] for more details.

Custom plugins can be installed by following the steps below:

1. stop the `harmony-ap` service (`sudo systemctl stop harmony-ap`);
2. copy the custom plugin `jar` file to the plugins folder (`/etc/harmony-ap/plugins/lib`);
3. copy the custom plugin configuration files to the config folder (`/etc/harmony-ap/plugins/lib/config`);
4. start the `harmony-ap` service (`sudo systemctl start harmony-ap`).

See the Domibus Plugin Cookbook \[[PLUGIN_COOKBOOK](#Ref_PLUGIN_COOKBOOK)\] for more information on developing custom plugins.

### 2.9 Changes Made to System During Installation

In addition to installing required dependencies, the installation process completes the following steps:
- creates linux user `harmony-ap` that is used to run the Access Point service;
- creates MySQL database user `harmony_ap` and generates random password for it;
- creates MySQL database schema `harmony_ap` and populates it with needed metadata;
- loads initial configuration into database;
- generates self-signed certificates for content encryption and transport encryption;
- configures One-Way SSL between two Access Points;
- installs the `harmony-ap` systemd service but does not enable or start it.

### 2.10 Location of Configuration and Generated Passwords 

All Access Point configuration files are located in the `/etc/harmony-ap` directory. See the Domibus Administration Guide \[[DOMIBUS_ADMIN_GUIDE](#Ref_DOMIBUS_ADMIN_GUIDE)\] for more details.

During the installation process, multiple random passwords are generated.

| **Password purpose** | **Password location** |
|---|---|
| Password for `harmony-ap` MySQL user | Configuration file: `/etc/harmony-ap/domibus.properties`<br /><br />Properties: `domibus.datasource.xa.property.password` and `domibus.datasource.password`. |
| Content encryption keystore (`/etc/harmony-ap/ap-keystore.jks`) password | Configuration file: `/etc/harmony-ap/domibus.properties`<br /><br />Properties: `domibus.security.keystore.password` and `domibus.security.key.private.password`. Content of this keystore can be changed using the administrative UI. |
| Content encryption truststore (`/etc/harmony-ap/ap-truststore.jks`) password | Configuration file: `/etc/harmony-ap/domibus.properties`<br /><br />Properties: `domibus.security.truststore.password`. Content of this keystore can be changed using the administrative UI. |
| TLS keystore (`/etc/harmony-ap/tls-keystore.jks`) password | Configuration file: `/etc/harmony-ap/conf/server.xml`<br /><br />Property: `keystorePass` |
| TLS truststore (`/etc/harmony-ap/tls-truststore.jks`) password | Configuration file: `/etc/harmony-ap/conf/server.xml`<br /><br />Property: `truststorePass` |