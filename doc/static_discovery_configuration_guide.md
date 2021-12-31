# Harmony eDelivery Access - Static Discovery Configuration Guide <!-- omit in toc -->

Version: 1.0  
Doc. ID: UG-SDCG

---

## Version history <!-- omit in toc -->

 Date       | Version | Description                                                     | Author
 ---------- | ------- | --------------------------------------------------------------- | --------------------
 30.12.2021 | 1.0     | Initial version                                                 | Petteri Kivimäki
 
## License <!-- omit in toc -->

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>
 
## Table of Contents <!-- omit in toc -->

- [License](#license)
- [1 Introduction](#1-introduction)
  - [1.1 Target Audience](#11-target-audience)
  - [1.2 Terms and abbreviations](#12-terms-and-abbreviations)
  - [1.3 References](#13-references)
- [2. Configure Static Discovery](#2-configure-static-discovery)
  - [2.1 Prerequisites](#21-prerequisites)
  - [2.2 Change the Sign and TLS Key Alias](#22-change-the-sign-and-tls-key-alias)
  - [2.3 Create TLS Truststore](#23-create-tls-truststore)
  - [2.4 TLS Configuration](#24-tls-configuration)
    - [2.4.1 One-Way SSL](#241-one-way-ssl)
    - [2.4.2 Two-Way SSL](#242-two-way-ssl)
  - [2.5 Import Trusted Certificates to Truststores](#25-import-trusted-certificates-to-truststores)
    - [2.5.1 Import Trusted Sign Certificates](#251-import-trusted-sign-certificates)
    - [2.5.2 Import Trusted TLS Certificates](#252-import-trusted-tls-certificates)
  - [2.6 Export Certificates from Keystores](#26-export-certificates-from-keystores)
    - [2.6.1 Export Sign Certificates](#261-export-sign-certificates)
    - [2.6.2 Export TLS Certificates](#262-export-tls-certificates)
  - [2.7 Create Plugin Users](#27-create-plugin-users)
- [3. Example Configuration](#3-example-configuration)
  - [3.1 Prerequisites](#31-prerequisites)
  - [3.2 PMode Configuration](#32-pmode-configuration)
  - [3.3 Create Plugin Users](#33-create-plugin-users)
    - [3.3.1 Access Point 1](#331-access-point-1)
    - [3.3.2 Access Point 2](#332-access-point-2)
  - [3.4 Change the Sign and TLS Key Alias](#34-change-the-sign-and-tls-key-alias)
    - [3.4.1 Access Point 1](#341-access-point-1)
    - [3.4.2 Access Point 2](#342-access-point-2)
  - [3.5 Export Certificates](#35-export-certificates)
    - [3.5.1 Access Point 1](#351-access-point-1)
    - [3.5.1 Access Point 2](#351-access-point-2)
  - [3.6 Import Certificates](#36-import-certificates)
    - [3.6.1 Access Point 1](#361-access-point-1)
    - [3.6.2 Access Point 2](#362-access-point-2)
  - [3.7 Configure One-Way SSL](#37-configure-one-way-ssl)
  - [3.8 Apply Changes](#38-apply-changes)
  - [3.9 Send Test Message](#39-send-test-message)
## 1 Introduction

Harmony eDelivery Access supports static and dynamic discovery. This document describes configuration of participants and services for static discovery. This guide is divided in two different sections, a configuration guide and a practical configuration example.

![static discovery](img/four_corner_model_static.svg)

### 1.1 Target Audience

This guide describes the configuration steps that are required for configuring static discovery on Harmony eDelivery Access Access Point.

The intended audience of this Static Discovery Configuration Guide are Access Point system administrators responsible for configuring the Access Point software.

The document is intended for readers with a moderate knowledge of Linux server management, computer networks, and the eDelivery working principles.

### 1.2 Terms and abbreviations

The main terms used in this document are:

- AP - Access Point, a component that participants use to send and receive messages in an eDelivery network.

See eDelivery definitions documentation \[[TERMS](#Ref_TERMS)\].

### 1.3 References

1. <a id="Ref_TERMS" class="anchor"></a>\[TERMS\] CEF Definitions - eDelivery Definitions, <https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/CEF+Definitions#CEFDefinitions-eDeliveryDefinitions>
2. <a id="Ref_IG-AP" class="anchor"></a>\[IG-AP\] Harmony eDelivery Access - Access Point Installation Guide. Document ID: [IG-AS](harmony-ap_installation_guide.md)
3. <a id="Ref_DOMIBUS_ADMIN_GUIDE" class="anchor"></a>\[DOMIBUS_ADMIN_GUIDE\] Access Point Administration Guide - Domibus 4.2.5, <https://ec.europa.eu/cefdigital/wiki/download/attachments/447677321/%28eDelivery%29%28AP%29%28AG%29%284.2.5%29%288.9.6%29.pdf>
4. <a id="Ref_WS_PLUGIN" class="anchor"></a>\[WS_PLUGIN\] Access Point Interface Control Document - WS Plugin, <https://ec.europa.eu/cefdigital/wiki/download/attachments/447677321/%28eDelivery%29%28AP%29%28ICD%29%28WS%20plugin%29%281.7%29.pdf>

## 2. Configure Static Discovery

### 2.1 Prerequisites

Before starting the static discovery configuration process, please complete the Access Point installation according to the installation guide:

- Harmony eDelivery Access - Access Point Installation Guide \[[IG-AS](harmony-ap_installation_guide.md)\].

Also, completing the configuration steps require command line access with root permissions to the host server.

The table below gives an overview of different keystore and truststore files that are accessed during the configuration process.

| **Keystore/truststore file** | **Password location** | **Password property** | **Description** |
|---|---|---|---|
| `/etc/harmony-ap/ap-keystore.jks` | `/etc/harmony-ap/domibus.properties` | `domibus.security.keystore.password` | Keystore for sign key and certificate. |
| `/etc/harmony-ap/ap-truststore.jks` | `/etc/harmony-ap/domibus.properties` | `domibus.security.truststore.password` | Truststore for trusted public sign certificates. |
| `/etc/harmony-ap/tls-keystore.jks` | `/etc/harmony-ap/tomcat-conf/server.xml` | `keystorePass` | Keystore for TLS key and certificate. |
| `/etc/harmony-ap/tls-truststore.jks` |  |  | Truststore for trusted public TLS certificates. **Note:** The truststore and its password are created during the configuration process. They don't exist by default. |

### 2.2 Change the Sign and TLS Key Alias

Sign and TLS keys are automatically created during the Access Point installation process. By default, the 
alias of both is `selfsigned`. However, their alias must be manually updated to match the party name of the sender. 
The party name is defined in the `PMode` configuration file. For example, this block from a `PMode` file is taken from
an Access Point owned by a party whose party name is `org1_gw`. In this case, the alias of sign and TLS keys should be 
changed from `selfsigned` to `org1_gw`.

```
<?xml version="1.0" encoding="UTF-8"?>
<db:configuration xmlns:db="http://domibus.eu/configuration" party="org1_gw">
.
.
<parties>
    <partyIdTypes>
        <partyIdType name="partyTypeUrn" value="urn:oasis:names:tc:ebcore:partyid-type:unregistered"/>
    </partyIdTypes>
    <party name="org2_gw"
            endpoint="https://<HOST_OR_IP>:8443/services/msh">
        <identifier partyId="harmony-org2" partyIdType="partyTypeUrn"/>
    </party>
    <party name="org1_gw"
            endpoint="https://HOST_OR_IP:8443/services/msh">
        <identifier partyId="harmony-org1" partyIdType="partyTypeUrn"/>
    </party>
</parties>
```

The sign key is stored in `/etc/harmony-ap/ap-keystore.jks`. The password of the keystore can be found in the
`/etc/harmony-ap/domibus.properties` file in the `domibus.security.keystore.password` property. 

```
sudo keytool -changealias -alias "selfsigned" -destalias "<party_name>" -keypass <ap_keystore_password> -keystore /etc/harmony-ap/ap-keystore.jks -storepass <ap_keystore_password>
```

The TLS key is stored in `/etc/harmony-ap/tls-keystore.jks`. The password of the keystore can be found in the
`/etc/harmony-ap/tomcat-conf/server.xml` file in the `keystorePass` property. 

```
sudo keytool -changealias -alias "selfsigned" -destalias "<party_name>" -keypass <tls_keystore_password> -keystore /etc/harmony-ap/tls-keystore.jks -storepass <tls_keystore_password>
```

Also, the `domibus.security.key.private.alias` property in the `/etc/harmony-ap/domibus.properties` file must be updated
with the new alias. Open the file with a text editor and update the property value:

```
#Private key
#The alias from the keystore of the private key
domibus.security.key.private.alias=selfsigned
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

### 2.3 Create TLS Truststore

A truststore for sign certificates is created automatically during the installation process. Instead, a truststore for
TLS certificates must be created manually. First, generate a secure password for the TLS truststore (`tls_truststore_password`):

```
openssl rand -base64 12
```

Then, generate the TLS truststore with a mock key pair and remove the mock key pair right after:

```
sudo keytool -genkeypair -alias mock -keystore /etc/harmony-ap/ap-truststore.jks -storepass <tls_truststore_password> -keypass <tls_truststore_password> -dname "CN=mock"
sudo keytool -delete -alias mock -keystore /etc/harmony-ap/ap-truststore.jks -storepass <tls_truststore_password>
```

Set the file permissions of the new TLS truststore file:

```
sudo chown harmony-ap:harmony-ap /etc/harmony-ap/tls-truststore.jks
sudo chmod 751 /etc/harmony-ap/tls-truststore.jks
```

### 2.4 TLS Configuration

Harmony Access Points supports two possible configurations, One-Way SSL and Two-Way SSL. See the Domibus Administration 
Guide \[[DOMIBUS_ADMIN_GUIDE](#Ref_DOMIBUS_ADMIN_GUIDE])\] for more details.

The TLS configuration is read from the `/etc/harmony-ap/clientauthentication.xml` file. The content of the file depends
on the configuration that's used.

**Note:** In the `/etc/harmony-ap/clientauthentication.xml` configuration examples, the `disableCNCheck` attribute specifies 
whether it is checked if the host name specified in the URL matches the host name specified in the Common Name (CN) of
the server's certificate. In the examples the value is `true` which means that the check is disabled and self-signed
certificates can be used. However, in production environment the value should be set to `false` which means that the
check is enabled.

#### 2.4.1 One-Way SSL

When One-Way SSL is used, the sender validates the signature of the receiver using the public certificate of the receiver.
The public certificate of the receiver is expected to be present in the `/etc/harmony-ap/tls-truststore.jks` file. The 
`/etc/harmony-ap/clientauthentication.xml` file should look like this:

```
<http-conf:tlsClientParameters disableCNCheck="true" secureSocketProtocol="TLSv1.2"
                               xmlns:http-conf="http://cxf.apache.org/transports/http/configuration"
                               xmlns:security="http://cxf.apache.org/configuration/security">
    <security:trustManagers>
        <security:keyStore type="JKS" password="<tls_truststore_password>"
                           file="/etc/harmony-ap/tls-truststore.jks"/>
    </security:trustManagers>
</http-conf:tlsClientParameters>
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

#### 2.4.2 Two-Way SSL

In Two-Way SSL, both the sender and the receiver sign the request and validate the trust of the other party.
The public certificate of the receiver is expected to be present in the `/etc/harmony-ap/tls-truststore.jks` file. Also,
the private key of the sender that's stored in the `/etc/harmony-ap/tls-keystore.jks` file is configured. The 
`/etc/harmony-ap/clientauthentication.xml` file should look like this:

```
<http-conf:tlsClientParameters disableCNCheck="true" secureSocketProtocol="TLSv1.2"
                               xmlns:http-conf="http://cxf.apache.org/transports/http/configuration"
                               xmlns:security="http://cxf.apache.org/configuration/security">
    <security:trustManagers>
        <security:keyStore type="JKS" password="<tls_truststore_password>"
                           file="/etc/harmony-ap/tls-truststore.jks"/>
    </security:trustManagers>
    <security:keyManagers keyPassword="<tls_keystore_password>">
        <security:keyStore type="JKS" password="<tls_keystore_password>"
                           file="/etc/harmony-ap/tls-keystore.jks"/>
    </security:keyManagers>
</http-conf:tlsClientParameters>
```

Also, the Tomcat connector defined in the `/etc/harmony-ap/tomcat-conf/server.xml` must be updated. The `truststoreFile` 
and `truststorePass` properties must be added to the configuration. In addition, the `clientAuth` property must be set
to `true`.

**Note:** Setting `clientAuth` to `true` affects all the Access Point's HTTP interfaces - including the admin UI and 
backend interface. In practise, after the change Two-Way SSL is required for the admin UI and backend interface too.

```
<Connector
           SSLEnabled="true"
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           port="8443"
           maxThreads="200"
           scheme="https"
           secure="true"
           keystoreFile="/etc/harmony-ap/tls-keystore.jks"
           keystorePass="<tls_keystore_password>"
           truststoreFile="/etc/harmony-ap/tls-truststore.jks"
           truststorePass="<tls_truststore_password>"
           clientAuth="true"
           sslProtocol="TLS"
    />
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

### 2.5 Import Trusted Certificates to Truststores

Public certificates of trusted data exhange parties must be imported to sign and TLS truststores. The certificates 
may be self-signed or issued by a trusted certification authority. Self-signed certificates must be
imported to the truststore directly. Instead, certificates issued by a trusted certificate authority may be imported
directly or alternatively, the root certificate of the certificate authority may be imported.

Depending on the trust model of the eDelivery policy domain where the Access Point is connected, certificate validation and
checks can be adjusted using the following properties in the `/etc/harmony-ap/domibus.properties` file:

| **Property** | **Default value** | **Description** |
|---|---|---|
| `domibus.sender.trust.validation.truststore_alias` | `true` | When enabled, Domibus will verify before receiving a message, that the sender's certificate matches the certificate in the truststore, loaded based on the alias (party name). |
| `domibus.sender.trust.validation.expression` | ` ` | When this property is not empty Domibus will verify before receiving a message, that the subject of the sender's certificate matches the regular expression. |
| `domibus.sender.certificate.subject.check` | `false` | When enabled, Domibus will verify before receiving a message, that the alias (party name) is present in the signing certificate subject. |
| `domibus.sender.trust.validation.onreceiving` | `true` | If activated Domibus will verify before receiving a message, the validity and authorization on the sender's certificate. When disabled, none of the other checks are performed on the sender's certificate. |

See the Domibus Administration Guide \[[DOMIBUS_ADMIN_GUIDE](#Ref_DOMIBUS_ADMIN_GUIDE])\] for more details regarding different configuration alternatives.

### 2.5.1 Import Trusted Sign Certificates

The channel where trusted sign certificates of data exchange parties are distributed or published varies between 
different eDelivery policy domains. If you don't know where to get them, please contact the domain authority of 
the policy domain where the Access Point is registered.

The sign certificate truststore located in `/etc/harmony-ap/ap-truststore.jks` can be updated through the admin UI. If 
you receive a JKS bundle containing all the trusted sign certificates, you can update the truststore using the admin UI. 
Instead, if you receive separate certificate files, you have to import manually on the command line.

If the certificate to be imported is a sign certificate of a data exchange party, the party name of the other party must
be used as an alias for the certificate. Instead, if the certificate to be imported is a root certificate of a trusted 
certificate authority, the certificate alias can be chosen freely.

The password of the sign certificate truststore can be found in the `/etc/harmony-ap/domibus.properties` file in the `domibus.security.truststore.password` property.

The following command can be used to import a trusted sign certificate to the sign certificate truststore:

```
sudo keytool -import -alias <party_name> -file </path/to/sign_certificate.crt> -keystore /etc/harmony-ap/ap-truststore.jks -storepass <ap_truststore_password>
```

All the trusted sign certificates can be listed using the following command:

```
keytool -list -v /etc/harmony-ap/ap-truststore.jks -storepass <ap_truststore_password>
```

A trusted sign certificate can be deleted using the following command:

```
sudo keytool -delete -noprompt -alias <party_name> /etc/harmony-ap/ap-truststore.jks -storepass <ap_truststore_password>
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

### 2.5.2 Import Trusted TLS Certificates

The channel where trusted TLS certificates of data exchange parties are distributed or published varies between 
different eDelivery policy domains. If you don't know where to get them, please contact the domain authority of 
the policy domain where the Access Point is registered.

The TLS certificate truststore located in `/etc/harmony-ap/tls-truststore.jks` can not be updated through the admin UI.

If the certificate to be imported is a TLS certificate of a data exchange party, it's recommended to use the party name 
of the other party as an alias for the certificate. Instead, if the certificate to be imported is a root certificate 
of a trusted certificate authority, the certificate alias can be chosen freely.

The following command can be used to import a trusted TLS certificate to the TLS certificate truststore:

```
sudo keytool -import -alias <party_name> -file </path/to/tls_certificate.crt> -keystore /etc/harmony-ap/tls-truststore.jks -storepass <tls_truststore_password>
```

All the trusted TLS certificates can be listed using the following command:

```
keytool -list -v /etc/harmony-ap/tls-truststore.jks -storepass <tls_truststore_password>
```

A trusted TLS certificate can be deleted using the following command:

```
sudo keytool -delete -noprompt -alias <party_name> /etc/harmony-ap/tls-truststore.jks -storepass <tls_truststore_password>
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

### 2.6 Export Certificates from Keystores

The channel where sign and TLS certificates of data exchange parties are distributed or published varies between 
different eDelivery policy domains. If you don't know where to publish them, please contact the domain authority of 
the policy domain where the Access Point is registered. Whatever the channel is, the first step is to export the certificates
from sign and TLS keystores.

### 2.6.1 Export Sign Certificates

A sign certificate belonging to a specific party can be exported using the following command:

```
sudo keytool -export -keystore /etc/harmony-ap/ap-keystore.jks -alias <party_name> -file </path/to/exported_sign_certificate.cer> -storepass <ap_keystore_password>
```

All the available sign keys can be listed using the following command:

```
keytool -list -v /etc/harmony-ap/ap-keystore.jks -storepass <ap_keystore_password>
```

A sign key can be deleted using the following command:

```
sudo keytool -delete -noprompt -alias <party_name> /etc/harmony-ap/ap-keystore.jks -storepass <ap_keystore_password>
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

### 2.6.2 Export TLS Certificates

A TLS certificate belonging to a specific party can be exported using the following command:

```
sudo keytool -export -keystore /etc/harmony-ap/tls-keystore.jks -alias <party_name> -file </path/to/exported_tls_certificate.cer> -storepass <tls_keystore_password>
```

All the available TLS keys can be listed using the following command:

```
keytool -list -v /etc/harmony-ap/tls-keystore.jks -storepass <tls_keystore_password>
```

A TLS key can be deleted using the following command:

```
sudo keytool -delete -noprompt -alias <party_name> /etc/harmony-ap/tls-keystore.jks -storepass <tls_keystore_password>
```

Restart the `harmony-ap` service to apply the changes:

```
sudo systemctl restart harmony-ap
```

### 2.7 Create Plugin Users

Creating plugin users is not part of the actual static discovery configuration. However, configuring at least one plugin user
is required in order to be able to send or receive messages using the Harmony Access Point default configuration. By default,
the plugin security is activated and every request needs to be authenticated. The plugin security can be disabled by
setting the `domibus.auth.unsecureLoginAllowed` to `true` in the `/etc/harmony-ap/domibus.properties` configuration file.

A plugin must use a configured plugin user that represents an original user that is either `originalSender` or 
`finalRecipient`. `originalSender` sender is `C1` and `finalRecipient` is `C4` in the four corner model. In practice, 
the plugin user is used by the backend system connected to the Access Point. The backend system can be a message sender 
(`originalSender`) or a message receiver (`finalRecipient`). 

The management of the plugin users is implemented in the Plugin Users page in the admin UI. The value of the `Original User`
field must be the same that is used in the `originalSender` or `finalRecipient` field in an actual message. If the backend
system is in the sender role, the value of the `originalSender` field must be used. Instead, if the backend system is in 
the receiver role, the value of the `finalRecipient` field must be used.

For example, a backend system in the sender role would use `urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1` in the
`Original User` field while a backend system in the receiver role would use `urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4`.

```
.
.
<ns:MessageProperties>
   <ns:Property name="originalSender">urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1</ns:Property>
   <ns:Property name="finalRecipient">urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4</ns:Property>
</ns:MessageProperties>
.
.
```

The user name and password are used to authenticate requests to the plugin interface. With the default WS Plugin HTTP basic
authentication is used.

See the WS Plugin documentation \[[WS_PLUGIN](#Ref_WS_PLUGIN)\] for more details.

## 3. Example Configuration

This chapter includes a sample configuration that consists of two Harmony Access Points. After completing the configuration
steps described in this chapter, you should have two working Access Points that are able to exchange messages with 
each other.

![static discovery example configuration](img/static_discovery_configuration_example.svg)
 
The Access Points are owned by Organisation 1 and Organisation 2. The party name of the Access Point owned by Organisation 1 
is `org1_gw` and the party name of the Access Point owned by Organisation 2 is `org2_gw`. Organisation 1 acts as a 
sender (`originalSender`) and Organisation 2 acts as a recipient (`finalRecipient`). In the end, Organisation 1 will send
a push message to Organisation 2.

### 3.1 Prerequisites
    
Before starting the static discovery configuration process, please complete the Access Point installation according to the installation guide:

- Harmony eDelivery Access - Access Point Installation Guide \[[IG-AS](harmony-ap_installation_guide.md)\].

Also, completing the configuration steps require command line access with root permissions to the host server.

### 3.2 PMode Configuration

The PMode configuration files for the Access Points can be downloaded here:

- [Access Point 1 (org1_gw)](configuration_examples/static_discovery/pmode_org1.xml);
- [Access Point 2 (org2_gw)](configuration_examples/static_discovery/pmode_org2.xml).

Upload the PMode files to the Access Points using the admin UI. Then, replace `AP2_IP_OR_HOST` in row 24 and 
`AP1_IP_OR_HOST` in row 28 with the correct host names or IP addresses of the Access Points:

```
<party name="org2_gw"
        endpoint="https://AP2_IP_OR_HOST:8443/services/msh">
    <identifier partyId="harmony-org2" partyIdType="partyTypeUrn"/>
</party>
<party name="org1_gw"
        endpoint="https://AP1_IP_OR_HOST:8443/services/msh">
    <identifier partyId="harmony-org1" partyIdType="partyTypeUrn"/>
</party>
```

### 3.3 Create Plugin Users

Plugin users are created using the admin UI.

#### 3.3.1 Access Point 1

On the Access Point (`org1_gw`) create a plugin user with the following information:

- User Name: `org1`
- Original User: `urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1`
- Role: `ROLE_USER`
- Password: choose a secure password
- Active: `checked`

Click OK and then Save.

#### 3.3.2 Access Point 2

On the Access Point (`org2_gw`) create a plugin user with the following information:

- User Name: `org2`
- Original User: `urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4`
- Role: `ROLE_USER`
- Password: choose a secure password
- Active: `checked`

Click OK and then Save.

### 3.4 Change the Sign and TLS Key Alias

The sign key is stored in `/etc/harmony-ap/ap-keystore.jks`. The password of the keystore can be found in the
`/etc/harmony-ap/domibus.properties` file in the `domibus.security.keystore.password` property. 

The TLS key is stored in `/etc/harmony-ap/tls-keystore.jks`. The password of the keystore can be found in the
`/etc/harmony-ap/tomcat-conf/server.xml` file in the `keystorePass` property. 

This step requires shell access to the host.

#### 3.4.1 Access Point 1

On the Access Point `org1_gw` change the sign and TLS key alias: 

```
sudo keytool -changealias -alias "selfsigned" -destalias "org1_gw" -keypass <ap_keystore_password> -keystore /etc/harmony-ap/ap-keystore.jks -storepass <ap_keystore_password>
sudo keytool -changealias -alias "selfsigned" -destalias "org1_gw" -keypass <tls_keystore_password> -keystore /etc/harmony-ap/tls-keystore.jks -storepass <tls_keystore_password>
```

Also, the `domibus.security.key.private.alias` property in the `/etc/harmony-ap/domibus.properties` file must be updated
with the new alias. Open the file with a text editor and update the property value:

```
#Private key
#The alias from the keystore of the private key
domibus.security.key.private.alias=org1_gw
```

#### 3.4.2 Access Point 2

On the Access Point `org2_gw` change the sign and TLS key alias: 

```
sudo keytool -changealias -alias "selfsigned" -destalias "org2_gw" -keypass <ap_keystore_password> -keystore /etc/harmony-ap/ap-keystore.jks -storepass <ap_keystore_password>
sudo keytool -changealias -alias "selfsigned" -destalias "org2_gw" -keypass <tls_keystore_password> -keystore /etc/harmony-ap/tls-keystore.jks -storepass <tls_keystore_password>
```

Also, the `domibus.security.key.private.alias` property in the `/etc/harmony-ap/domibus.properties` file must be updated
with the new alias. Open the file with a text editor and update the property value:

```
#Private key
#The alias from the keystore of the private key
domibus.security.key.private.alias=org2_gw
```

### 3.5 Export Certificates

The sign certificate is stored in `/etc/harmony-ap/ap-keystore.jks`. The password of the keystore can be found in the
`/etc/harmony-ap/domibus.properties` file in the `domibus.security.keystore.password` property. 

The TLS certificate is stored in `/etc/harmony-ap/tls-keystore.jks`. The password of the keystore can be found in the
`/etc/harmony-ap/tomcat-conf/server.xml` file in the `keystorePass` property. 

This step requires shell access to the host.

#### 3.5.1 Access Point 1

Export the sign certificate using the following command:

```
sudo keytool -export -keystore /etc/harmony-ap/ap-keystore.jks -alias org1_gw -file org1_sign_certificate.cer -storepass <ap_keystore_password>
```

Export the TLS certificate using the following command:

```
sudo keytool -export -keystore /etc/harmony-ap/tls-keystore.jks -alias org1_gw -file org1_tls_certificate.cer -storepass <tls_keystore_password>
```

#### 3.5.1 Access Point 2

Export the sign certificate using the following command:

```
sudo keytool -export -keystore /etc/harmony-ap/ap-keystore.jks -alias org2_gw -file org2_sign_certificate.cer -storepass <ap_keystore_password>
```

Export the TLS certificate using the following command:

```
sudo keytool -export -keystore /etc/harmony-ap/tls-keystore.jks -alias org2_gw -file org2_tls_certificate.cer -storepass <tls_keystore_password>
```

### 3.6 Import Certificates

In order to establish a trusted relationship between the Access Points, they must import each others certificates. 
Therefore, the certificates exported in section 3.6 must be copied from Access Point 1 to Access Point 2 and vice versa.

The password of the sign certificate truststore can be found in the `/etc/harmony-ap/domibus.properties` file in the `domibus.security.truststore.password` property.

This step requires shell access to the host.

#### 3.6.1 Access Point 1

Import the sign certificate of Access Point 2:

```
sudo keytool -import -alias org2_gw -file org2_sign_certificate.cer -keystore /etc/harmony-ap/ap-truststore.jks -storepass <ap_truststore_password>
```

The TLS truststore doesn't exist yet and it will be created when the TLS certificate of the Access Point 2 is imported. 
Therefore, generate a secure password for the TLS truststore (`tls_truststore_password`):

```
openssl rand -base64 12
```

Then, import the TLS certificate of Access Point 2:

```
sudo keytool -import -alias org2_gw -file org2_tls_certificate.cer -keystore /etc/harmony-ap/tls-truststore.jks -storepass <tls_truststore_password>
```

Set the file permissions of the new TLS truststore file:

```
sudo chown harmony-ap:harmony-ap /etc/harmony-ap/tls-truststore.jks
sudo chmod 751 /etc/harmony-ap/tls-truststore.jks
```

#### 3.6.2 Access Point 2

Import the sign certificate of Access Point 1:

```
sudo keytool -import -alias org1_gw -file org1_sign_certificate.cer -keystore /etc/harmony-ap/ap-truststore.jks -storepass <ap_truststore_password>
```

The TLS truststore doesn't exist yet and it will be created when the TLS certificate of the Access Point 1 is imported. 
Therefore, generate a secure password for the TLS truststore (`tls_truststore_password`):

```
openssl rand -base64 12
```

Then, import the TLS certificate of Access Point 1:

```
sudo keytool -import -alias org1_gw -file org1_tls_certificate.cer -keystore /etc/harmony-ap/tls-truststore.jks -storepass <tls_truststore_password>
```

Set the file permissions of the new TLS truststore file:

```
sudo chown harmony-ap:harmony-ap /etc/harmony-ap/tls-truststore.jks
sudo chmod 751 /etc/harmony-ap/tls-truststore.jks
```

### 3.7 Configure One-Way SSL

This step requires shell access to the host.
    
On Access Points 1 and 2, create the `/etc/harmony-ap/clientauthentication.xml` file with the following content:

```
<http-conf:tlsClientParameters disableCNCheck="true" secureSocketProtocol="TLSv1.2"
                               xmlns:http-conf="http://cxf.apache.org/transports/http/configuration"
                               xmlns:security="http://cxf.apache.org/configuration/security">
    <security:trustManagers>
        <security:keyStore type="JKS" password="<tls_truststore_password>"
                           file="/etc/harmony-ap/tls-truststore.jks"/>
    </security:trustManagers>
</http-conf:tlsClientParameters>
```

Set the file permissions of the new `/etc/harmony-ap/clientauthentication.xml` file:

```
sudo chown harmony-ap:harmony-ap /etc/harmony-ap/clientauthentication.xml
sudo chmod 751 /etc/harmony-ap/clientauthentication.xml
```

### 3.8 Apply Changes

This step requires shell access to the host.

On Access Points 1 and 2, restart the `harmony-ap` service to apply the configuration changes:

```
sudo systemctl restart harmony-ap
```

### 3.9 Send Test Message

Send a request to Access Point 1 using the curl command below. The request (`submitRequest.xml`) can be downloaded [here](configuration_examples/static_discovery/submitRequest.xml). The content inside the payload's `value` element must be [base64 encoded](https://www.base64encode.org/).

```
curl -u org1:<org1_plugin_user_password> --header "Content-Type: text/xml;charset=UTF-8" --data @submitRequest.xml https://<AP1_IP_OR_HOST>:8443/services/backend -v -k
```

A successful response looks like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
   <soap:Body>
      <ns2:submitResponse xmlns:ns2="http://org.ecodex.backend/1_1/" xmlns:ns3="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/" xmlns:ns4="http://www.w3.org/2003/05/soap-envelope" xmlns:xmime="http://www.w3.org/2005/05/xmlmime">
         <messageID>bd3ea132-6a0d-11ec-9789-0af9f1d3371a@edelivery.digital</messageID>
      </ns2:submitResponse>
   </soap:Body>
</soap:Envelope>
```

List received and pending messages on the Access Point 2. The request (`listPendingMessagesRequest.xml`) can be downloaded [here](configuration_examples/static_discovery/listPendingMessagesRequest.xml).

```
curl -u org2:<org2_plugin_user_password> --header "Content-Type: text/xml;charset=UTF-8" --data @listPendingMessagesRequest.xml https://<AP2_IP_OR_HOST>:8443/services/backend -v -k
```

A successful response looks like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
   <soap:Body>
      <ns2:listPendingMessagesResponse xmlns:ns2="http://org.ecodex.backend/1_1/" xmlns:ns3="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/" xmlns:ns5="http://www.w3.org/2003/05/soap-envelope" xmlns:xmime="http://www.w3.org/2005/05/xmlmime">
         <messageID>bd3ea132-6a0d-11ec-9789-0af9f1d3371a@edelivery.digital</messageID>
      </ns2:listPendingMessagesResponse>
   </soap:Body>
</soap:Envelope>
```

Retrieve the test message from Access Point 2. The request (`retrieveMessageRequest.xml`) can be downloaded [here](configuration_examples/static_discovery/retrieveMessageRequest.xml). Before sending the message, replace the `MESSAGE_ID` placeholder with the ID (`messageID`) of the test message.

```
curl -u org2:<org2_plugin_user_password> --header "Content-Type: text/xml;charset=UTF-8" --data @retrieveMessageRequest.xml https://<AP2_IP_OR_HOST>:8443/services/backend -v -k
```

A successful response looks like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
   <soap:Header>
      <ns5:Messaging xmlns:ns5="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/" xmlns:ns4="http://org.ecodex.backend/1_1/" xmlns:xmime="http://www.w3.org/2005/05/xmlmime" mustUnderstand="false">
         <ns5:UserMessage mpc="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC">
            <ns5:MessageInfo>
               <ns5:Timestamp>2021-12-31T07:58:07.003</ns5:Timestamp>
               <ns5:MessageId>bd3ea132-6a0d-11ec-9789-0af9f1d3371a@edelivery.digital</ns5:MessageId>
            </ns5:MessageInfo>
            <ns5:PartyInfo>
               <ns5:From>
                  <ns5:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">harmony-org1</ns5:PartyId>
                  <ns5:Role>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator</ns5:Role>
               </ns5:From>
               <ns5:To>
                  <ns5:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">harmony-org2</ns5:PartyId>
                  <ns5:Role>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder</ns5:Role>
               </ns5:To>
            </ns5:PartyInfo>
            <ns5:CollaborationInfo>
               <ns5:Service type="tc1">bdx:noprocess</ns5:Service>
               <ns5:Action>TC1Leg1</ns5:Action>
               <ns5:ConversationId>bd3ea133-6a0d-11ec-9789-0af9f1d3371a@edelivery.digital</ns5:ConversationId>
            </ns5:CollaborationInfo>
            <ns5:MessageProperties>
               <ns5:Property name="originalSender">urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1</ns5:Property>
               <ns5:Property name="finalRecipient">urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4</ns5:Property>
            </ns5:MessageProperties>
            <ns5:PayloadInfo>
               <ns5:PartInfo href="cid:message">
                  <ns5:PartProperties>
                     <ns5:Property name="MimeType">text/xml</ns5:Property>
                  </ns5:PartProperties>
               </ns5:PartInfo>
            </ns5:PayloadInfo>
         </ns5:UserMessage>
      </ns5:Messaging>
   </soap:Header>
   <soap:Body>
      <ns2:retrieveMessageResponse xmlns:ns2="http://org.ecodex.backend/1_1/" xmlns:ns3="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/" xmlns:ns5="http://www.w3.org/2003/05/soap-envelope" xmlns:xmime="http://www.w3.org/2005/05/xmlmime">
         <payload payloadId="cid:message">
            <value>PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=</value>
         </payload>
      </ns2:retrieveMessageResponse>
   </soap:Body>
</soap:Envelope>
```

The content inside the payload's `value` element is base64 encoded. After [decoding](https://www.base64decode.org/), the value looks like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<hello>world</hello>
```