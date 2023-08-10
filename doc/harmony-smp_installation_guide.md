# Harmony eDelivery Access - Service Metadata Publisher Installation Guide <!-- omit in toc -->

Version: 1.12  
Doc. ID: IG-SMP
---

## Version history <!-- omit in toc -->

 Date       | Version | Description                                                                                             | Author
 ---------- |---------|---------------------------------------------------------------------------------------------------------| --------------------
 15.11.2021 | 1.0     | Initial version                                                                                         |
 20.12.2021 | 1.1     | Add section [2.4 Preparing OS](#24-preparing-os)                                                        | Petteri Kivimäki
 21.12.2021 | 1.2     | Add section [2.11 Securing SMP user interface](#211-securing-smp-user-interface)                        | Andres Allkivi
 07.01.2021 | 1.3     | Add language types to code blocks                                                                       | Petteri Kivimäki
 22.01.2021 | 1.4     | Add more information about keystores and trustores. Add information about properties stored in database | Petteri Kivimäki
 06.02.2021 | 1.5     | Add upgrade instructions. Add section about log files                                                   | Petteri Kivimäki
 23.04.2022 | 1.6     | Add port number to the SMP Installation section. Update package repository URL                          | Petteri Kivimäki
 28.04.2022 | 1.7     | Minor changes                                                                                           | Petteri Kivimäki
 22.01.2023 | 1.8     | Update SMP Admin Guide link                                                                             | Petteri Kivimäki
 01.06.2023 | 1.9     | Add more information about allowed characters in certificates                                           | Petteri Kivimäki
 31.07.2023 | 1.10    | Updates for SMP version 2.0                                                                             | Jarkko Hyöty
 04.08.2023 | 1.11    | Update DomiSMP Admin Guide link                                                                         | Petteri Kivimäki
 09.08.2023 | 1.12    | Update section 2.10                                                                                     | Jarkko Hyöty

## License <!-- omit in toc -->

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>
 
## Table of Contents <!-- omit in toc -->

<!-- vim-markdown-toc GFM -->

* [1 Introduction](#1-introduction)
  * [1.1 Target Audience](#11-target-audience)
  * [1.2 Terms and abbreviations](#12-terms-and-abbreviations)
  * [1.3 References](#13-references)
* [2. Installation](#2-installation)
  * [2.1 Prerequisites to Installation](#21-prerequisites-to-installation)
  * [2.2 Network Diagram](#22-network-diagram)
  * [2.3 Requirements for the SMP](#23-requirements-for-the-smp)
  * [2.4 Preparing OS](#24-preparing-os)
  * [2.5 Setup Package Repository](#25-setup-package-repository)
  * [2.6 SMP Installation](#26-smp-installation)
    * [External MySQL 8 database setup (optional)](#external-mysql-8-database-setup-optional)
    * [SMP install](#smp-install)
  * [2.7 Starting harmony-smp Service and Enabling Automatic Startup](#27-starting-harmony-smp-service-and-enabling-automatic-startup)
  * [2.8 Post-Installation Checks](#28-post-installation-checks)
  * [2.9 Changes Made to System During Installation](#29-changes-made-to-system-during-installation)
  * [2.10 Location of Configuration and Generated Passwords](#210-location-of-configuration-and-generated-passwords)
  * [2.11 Securing SMP user interface](#211-securing-smp-user-interface)
  * [2.12 Log Files](#212-log-files)
* [3 Version Upgrade](#3-version-upgrade)

<!-- vim-markdown-toc -->

## 1 Introduction

Harmony eDelivery Access Service Metadata Publisher (SMP) enables dynamic discovery in eDelivery policy domains. Harmony eDelivery Access SMP is based on the DomiSMP open source project by the European Commission.

### 1.1 Target Audience

This guide describes installation and post-installation procedures for Harmony eDelivery Access SMP.

The intended audience of this Installation Guide are SMP system administrators responsible for installing and using the SMP software.

The document is intended for readers with a moderate knowledge of Linux server management, computer networks, and the eDelivery working principles.

### 1.2 Terms and abbreviations

See eDelivery documentation \[[TERMS](#Ref_TERMS)\].

### 1.3 References

1. <a id="Ref_TERMS" class="anchor"></a>\[TERMS\] eDelivery Documentation, <https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/eDelivery>
2. <a id="Ref_SMP_ADMIN_GUIDE" class="anchor"></a>\[SMP_ADMIN_GUIDE\] DomiSMP Administration Guide - DomiSMP 5.0, <https://ec.europa.eu/digital-building-blocks/wikis/download/attachments/674510184/%28eDelivery%29%28SMP%29%28AG%29%28DomiSMP%205.0%29%283.7%29.pdf>
3. <a id="Ref_UG-DDCG" class="anchor"></a>\[UG-DDCG\] Harmony eDelivery Access - Dynamic Discovery Configuration Guide. Document ID: [UG-DDCG](dynamic_discovery_configuration_guide.md)
4. <a id="Ref_RFC5280" class="anchor"></a>\[RFC5280\] RFC 5280: Internet X.509 Public Key Infrastructure Certificate and Certificate Revocation List (CRL) Profile, <https://www.rfc-editor.org/rfc/rfc5280>
5. <a id="Ref_PS" class="anchor"></a>\[PS\] PrintableString, <https://en.wikipedia.org/wiki/PrintableString>

## 2. Installation

### 2.1 Prerequisites to Installation

The SMP is officially supported on the following platforms:

* Ubuntu Server 20.04 Long-Term Support (LTS) operating system on a x86-64 platform.

The software can be installed both on physical and virtualized hardware.

*Note*: If MySQL database engine is already installed then installation requires that `root` user can access database using UNIX
socket peer authentication without password.

*Note*: SMP and Access Point must not be installed on the same host because they both use port `8443`.

### 2.2 Network Diagram

The network diagram below provides an example of an SMP setup when dynamic discovery is used.

![network diagram](img/ig-smp_network_diagram.svg)

The table below lists the required connections between different components.

**Connection Type** | **Source** | **Target** | **Target Ports** | **Protocol** | **Note** |
-----------|------------|-----------|-----------|-----------|-----------|
Out | SMP | SML | 443, 8443, other | tcp | |
In  | Data Exchange Partner Access Point | SMP | 8443* | tcp | URL paths: `/{participantIdentifier}` and `/{participantIdentifier}/services/{documentIdentifier}` |
In | Admin | SMP | 8443* | tcp | Source in the internal network<br /><br />URL paths: `/` and `/ui` |

\* The port number for inbound connections is configurable and the value can be set during the SMP installation process. Port `8443` is used by default.

It is strongly recommended to protect the SMP from unwanted access using a firewall (hardware or software based). The firewall can be applied to both incoming and outgoing connections depending on the security requirements of the environment where the SMP is deployed. It is recommended to allow incoming traffic to specific ports only from explicitly defined sources using IP filtering. **Special attention should be paid with the firewall configuration since incorrect configuration may leave the SMP vulnerable to exploits and attacks.**

In addition, it's strongly recommended to use URL path filtering for the SMP since the admin UI and metadata query interface run on the same port. By default, the port number is `8443`, but it is configurable.

**Port** | **URL Path** | **Description** |
---------|----------|-----------------|
 8443    | `/` | Landing page. |
 8443    | `/ui` | Admin UI for managing the SMP. |
 8443    | `/{participantIdentifier}` |  Service group query interface. |
 8443    | `/{participantIdentifier}/services/{documentIdentifier}` |  Metadata query interface. |

### 2.3 Requirements for the SMP

Minimum recommended hardware parameters:

* the server’s hardware (motherboard, CPU, network interface cards, storage system) must be supported by Ubuntu in general;
* a 64-bit dual-core Intel, AMD or compatible CPU; AES instruction set support is highly recommended;
* 4 GB RAM;
* a 100 Mbps network interface card.

Requirements to software and settings:

* an installed and configured Ubuntu 20.04 LTS x86-64 operating system;
* if the SMP is separated from other networks by a firewall and/or NAT, the necessary connections to and from the SMP are allowed;
* if the SMP has a private IP address, a corresponding NAT record must be created in the firewall;
* enabling auxiliary services which are necessary for the functioning and management of the operating system (such as DNS, NTP, and SSH) stay outside the scope of this guide.

### 2.4 Preparing OS

Some virtual machines may not be able to supply enough entropy that's required in key generation. It's recommended to
check the level of available entropy before the installation. It can be done by issuing the command below:

```bash
cat /proc/sys/kernel/random/entropy_avail
```

If the number returned by the command is less than 200, it indicates that there's not enough entropy available for 
the key generation. In that case, it's strongly recommended to install the `rng-tools` package before installing SMP:

```bash
sudo apt-get install rng-tools
```

### 2.5 Setup Package Repository

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
sudo apt-add-repository -y "deb https://artifactory.niis.org/artifactory/harmony-release-deb $(lsb_release -sc)-current main"
```

Update package repository metadata:
```bash
sudo apt update
```

### 2.6 SMP Installation

#### External MySQL 8 database setup (optional)

When using an _external_ database (MySQL 8 required), it is necessary to create the database schema and user before installing the SMP. 
Please also make sure that the external database accepts connections from the SMP host.

The schema and user can be created using the following SQL DDL statements (adjust user and schema name as needed; the default _harmony_smp_ is used in the example):

```sql
-- mysql
create schema if not exists harmony_smp;
alter schema harmony_smp charset=utf8mb4 collate=utf8mb4_bin;
create user if not exists harmony_smp@'%';
alter user harmony_smp@'%' identified by '<password>';
grant all on harmony_smp.* to harmony_smp@'%';
```
When using a _local database_, the installer handles these additional steps.

#### SMP install

Issue the following command to install the Harmony eDelivery Access SMP:
```bash
sudo apt install harmony-smp
```

Upon the first installation of the SMP, the system asks for the following information.

- Port number that the SMP listens to. The default is `8443`;
  - the SMP admin UI and metadata query interface run on the defined port;
- SMP point database configuration. When using a local database, accept the defaults.
  - Database host. The default is `localhost`.
  - Database port.  The default is `3306`.
  - Database schema name. The default is `harmony_smp`.
  - Database user name. The default is `harmony_smp`.
  - Database password. There is no default. Leave blank to generate a random password when installing a local database.
- `Distinguished Name` for generated self-signed content and transport certificates;
  - for example:
      ```bash
      CN=example.com, O=My Organisation, C=FI
      ```
  - The `Distinguished Name` (`DN`) uniquely identifies an entity in an X.509 certificate \[[RFC5280](#Ref_RFC5280)\]. The following attribute types are commonly found in the `DN`: `CN = Common name, O = Organization name, C = Country code`. It's recommended to use PrintableString characters \[[PS](#Ref_PS)\] in the attribute type values;
  - *Note:* different eDelivery policy domains may have different requirements for the `Distinguished Name`. If you're not sure about the requirements, please contact the domain authority of the policy domain where the SMP is registered.
- Do you want the SMP installation to publish information to some Service Metadata Locator (SML);  
  - if yes then: 
    - full URL of the SML server, including protocol and port, e.g., `https://<HOST>:8443`;
    - full URL of this SMP server as seen from public Internet, including protocol and port, e.g., `https://<HOST>:8443`;
    - public IP address of this SMP server (reachable from public Internet), e.g., `172.2.3.14`;
- Username of the administrative user - username to use to log in to administrative UI;
- Initial password for the administrative user.

See the Dynamic Discovery Configuration Guide \[[UG-DDCG](dynamic_discovery_configuration_guide.md)\] for more information about how to configure dynamic discovery.

### 2.7 Starting harmony-smp Service and Enabling Automatic Startup 

To start `harmony-smp` service issue the following command:
```bash
sudo systemctl start harmony-smp
```

If you want `harmony-smp` service start at system startup issue the following command:
```bash
sudo systemctl enable harmony-smp
```

### 2.8 Post-Installation Checks

Ensure that the `harmony-smp` service is in the `running` state (example output follows):
  ```bash
  sudo systemctl list-units "harmony-smp*"
  ```
  ```bash
  UNIT                           LOAD   ACTIVE SUB     DESCRIPTION
  harmony-smp.service             loaded active running Harmony eDelivery Access - SMP
  ```

Ensure that the administrative user interface at `https://<host>:8443/` can be opened in a Web browser. To log in, use the administrative username and password chosen during the installation. While the user interface is still starting up, the Web browser may display a connection refused -error.

### 2.9 Changes Made to System During Installation

In addition to installing required dependencies, the installation process completes the following steps:
- creates linux user `harmony-smp` that is used to run the SMP service;
- creates MySQL database user `harmony_smp` and generates random password for it;
- creates MySQL database schema `harmony_smp` and populates it with needed metadata;
- creates initial configuration (`/etc/harmony-smp/smp.init.properties`)
- generates self-signed certificates for content encryption and for transport encryption;
- installs `systemd` service `harmony-smp` but does not enable or start it.

### 2.10 Location of Configuration and Generated Passwords 

All SMP configuration files are located in the `/etc/harmony-smp` directory. See the SMP Administration Guide \[[SMP_ADMIN_GUIDE](#Ref_SMP_ADMIN_GUIDE])\] for more details.

During the installation process, multiple random passwords are generated.

| **Password purpose** | **Password location** |
|---|---|
| Password for `harmony-smp` MySQL user  | Configuration file: `/etc/harmony-smp/tomcat-conf/context.xml` |
| Content encryption keystore (`/etc/harmony-smp/smp-keystore.p12`) password | File /etc/harmony-smp/smp.init.properties, property `smp.keystore.password`. The format is `{DEC}{$PASSWORD}` where `$PASSWORD` is the keystore password. Content of this keystore can be changed using UI.|
| Content encryption truststore (`/etc/harmony-smp/smp-truststore.p12`) password | File /etc/harmony-smp/smp.init.properties, property `smp.truststore.password`. The format is `{DEC}{$PASSWORD}` where `$PASSWORD` is the truststore password. |
| TLS keystore (`/etc/harmony-smp/tls-keystore.p12`) password | Configuration file: `/etc/harmony-smp/tomcat-conf/server.xml`<br /><br />Property: `keystorePass` |
| TLS truststore (`/etc/harmony-smp/tls-truststore.p12`) password | Configuration file: `/etc/harmony-smp/tomcat-conf/server.xml`<br /><br />Property: `truststorePass`<br /><br />Also, the password is stored in: `/opt/harmony-smp/bin/setenv.sh` |

Part of the SMP configuration is stored in MySQL database. The following properties are stored in the `SMP_CONFIGURATION` 
table. The values are configured when the SMP is installed for the first time.

| **Property** | **Default** | **Description** |
|---|---|---|
| `bdmsl.integration.enabled` | `false` | Does the SMP installation publish information to some Service Metadata Locator (SML). |
| `bdmsl.integration.url` | ` ` | Full URL of the SML server, including protocol and port. |
| `bdmsl.integration.logical.address` | ` ` | Full URL of this SMP server as seen from public Internet, including protocol and port. |
| `bdmsl.integration.physical.address` | ` ` | Public IP address of this SMP server (reachable from public Internet). |

The current values of the properties can be fetched using the following command:

```bash
sudo mysql -e "use harmony_smp; select * from SMP_CONFIGURATION where PROPERTY in('bdmsl.integration.enabled', 'bdmsl.integration.url', 'bdmsl.integration.logical.address', 'bdmsl.integration.physical.address');"
```


### 2.11 Securing SMP user interface

For security reasons, it is highly recommended not to expose all SMP endpoints to public internet. Since all SMP endpoints
use the port `8443`, an application level firewall or proxy is needed.

To better protect SMP, a reverse proxy can be deployed between SMP and internet. For dynamic discovery purposes only
the endpoint for reading service metadata must be publicly accessible. SMP publishes service metadata from request path
`/{serviceGroupId}/services/{serviceMetadataId}`. Note that only `GET` requests should be publicly accessible. Instead, 
`PUT` and `DELETE` requests to the same path should be protected.

Here's an example configuration excerpt for NGINX HTTP server. The example assumes that `192.168.0.1` is address of SMP 
server behind reverse proxy: 

```
location ~ [^\/]*\/services\/[^\/]*$
{
  # this proxy_pass only applies to GET requests (all others are caught by limit_except below)
  proxy_pass http://192.168.0.1:8080
  
  limit_except GET {
    # deny all EXCEPT GET requests
    deny all;
  }
}
```

When SMP is behind a reverse proxy SSL connections have to be terminated at proxy and SMP reconfigured to accept plain
HTTP connections. Edit `/etc/harmony-smp/tomcat-conf/server.xml` and add additional connector for plain http connections:

```xml
<Connector port="8080" protocol="org.apache.coyote.http11.Http11AprProtocol"
           maxThreads="150"/>
```

For security reasons unencrypted connections should be used only between reverse proxy and SMP. To enforce this add
remote address valve to `Host` element in `server.xml`, enabling HTTP port access only from proxy.

Example assuming proxy IP address is `192.168.1.1`:

```xml
<Valve className="org.apache.catalina.valves.RemoteAddrValve"
  addConnectorPort="true"
  allow="(.*;8443|192\.168\.1\.1;8080)$"
/>
```

Please note that when registering SMP with SML, the externally visible address and hostname have to be used, i.e., the
address and hostname of reverse proxy, not address and hostname of SMP.

### 2.12 Log Files

The SMP application log files are located in the `/var/log/harmony-smp/` directory.

## 3 Version Upgrade

The the `harmony-smp` service is automatically stopped for the upgrade and automatically restarted after the upgrade if
the service has been enabled. Otherwise, the service must be manually restarted after the upgrade.

Update package repository metadata:
```bash
sudo apt update
```

Issue the following command to run the upgrade:
```bash
sudo apt upgrade
```

If starting the service at system startup hasn't been enabled, the `harmony-smp` service must be started manually after
the upgrade:
```bash
sudo systemctl start harmony-smp
```
