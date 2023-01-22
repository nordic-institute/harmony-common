# Harmony eDelivery Access - Dynamic Discovery Configuration Guide <!-- omit in toc -->

Version: 1.4  
Doc. ID: UG-DDCG

---

## Version history <!-- omit in toc -->

 Date       | Version | Description                                                     | Author
 ---------- |---------| --------------------------------------------------------------- | --------------------
 03.12.2021 | 1.0     | Initial version                                                 |
 22.01.2022 | 1.1     | Add information about keys and certficates. Add more configuration examples | Petteri Kivimäki
 06.01.2022 | 1.2     | Minor updates                                                   | Petteri Kivimäki
 16.02.2022 | 1.3     | Minor updates                                                   | Petteri Kivimäki
 22.01.2023 | 1.4     | Update SMP Admin Guide link                                     | Petteri Kivimäki
  
## License <!-- omit in toc -->

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>
 
## Table of Contents <!-- omit in toc -->

- [License](#license)
- [1 Introduction](#1-introduction)
  - [1.1 Target Audience](#11-target-audience)
  - [1.2 Terms and abbreviations](#12-terms-and-abbreviations)
  - [1.3 References](#13-references)
  - [1.4 Prerequisites](#14-prerequisites)
- [2. Dynamic Discovery for Sending Parties](#2-dynamic-discovery-for-sending-parties)
  - [2.1 Prerequisites](#21-prerequisites)
  - [2.2 Configuring Dynamic Discovery in Sender AP](#22-configuring-dynamic-discovery-in-sender-ap)
  - [2.3 TLS Configuration and Certificates](#23-tls-configuration-and-certificates)
- [3. Dynamic Discovery for Receiving Parties](#3-dynamic-discovery-for-receiving-parties)
  - [3.1 Prerequisites](#31-prerequisites)
  - [3.2 Keys and Certificates](#32-keys-and-certificates)
  - [3.3 Registering SMP in SML](#33-registering-smp-in-sml)
  - [3.4 Registering Final Recipient in SML](#34-registering-final-recipient-in-sml)
  - [3.5 Registering Services in SMP](#35-registering-services-in-smp)
  - [3.6 Configuring Dynamic Discovery in Receiving AP](#36-configuring-dynamic-discovery-in-receiving-ap)
    - [3.6.1 TLS Configuration and Certificates](#361-tls-configuration-and-certificates)
    - [3.6.2 Final Recipient in Plugin Configuration and AS4 Message](#362-final-recipient-in-plugin-configuration-and-as4-message)
    - [3.6.3 Specifying Document Identifier Scheme and Document Identifier in AP PMode and AS4 Message](#363-specifying-document-identifier-scheme-and-document-identifier-in-ap-pmode-and-as4-message)
    - [3.6.4 Specifying Process Scheme and Process Identifier in AP PMode and in AS4 Message](#364-specifying-process-scheme-and-process-identifier-in-ap-pmode-and-in-as4-message)
 
## 1 Introduction

Harmony eDelivery Access supports static and dynamic discovery. This document describes configuration of participants and services for dynamic discovery. The configuration process differs between sending and receiving parties and therefore, this guide is divided in two different sections.

![network diagram](img/four_corner_model_dynamic.svg)

The Static Discovery Configuration Guide \[[UG-SDCG](static_discovery_configuration_guide.md)\] sections 2.2-2.6 explain
how to configure TLS connection between Access Points, import/export certificates and configure plugin users. Those 
sections apply to dynamic discovery too and completing the described configuration is a requirement for successful
communication between different components. Please see the Static Discovery Configuration Guide 
\[[UG-SDCG](static_discovery_configuration_guide.md)\] for details.

### 1.1 Target Audience

This guide describes the configuration steps that are required for enabling dynamic discovery on Harmony eDelivery Access Access Point and SMP.

The intended audience of this Dynamic Discovery Configuration Guide are Access Point and SMP system administrators responsible for configuring the Access Point and SMP software.

The document is intended for readers with a moderate knowledge of Linux server management, computer networks, and the eDelivery working principles.

### 1.2 Terms and abbreviations

The main terms used in this document are:

- SML - Service Metadata Locator, a component that manages DNS records for dynamic discovery;
- SMP - Service Metadata Provider, a component that provides a public API for service discovery that interested parties can use to access service metadata;
- AP - Access Point, a component that participants use to send and receive messages in an eDelivery network.

See eDelivery definitions documentation \[[TERMS](#Ref_TERMS)\].

### 1.3 References

1. <a id="Ref_TERMS" class="anchor"></a>\[TERMS\] CEF Definitions - eDelivery Definitions, <https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/CEF+Definitions#CEFDefinitions-eDeliveryDefinitions>
2. <a id="Ref_IG-AP" class="anchor"></a>\[IG-AP\] Harmony eDelivery Access - Access Point Installation Guide. Document ID: [IG-AS](harmony-ap_installation_guide.md)
3. <a id="Ref_IG-SMP" class="anchor"></a>\[IG-SMP\] Harmony eDelivery Access - Service Metadata Publisher Installation Guide. Document ID: [IG-SMP](harmony-smp_installation_guide.md)
4. <a id="Ref_DOMIBUS_ADMIN_GUIDE" class="anchor"></a>\[DOMIBUS_ADMIN_GUIDE\] Access Point Administration Guide - Domibus 4.2.5, <https://ec.europa.eu/cefdigital/wiki/download/attachments/447677321/%28eDelivery%29%28AP%29%28AG%29%284.2.5%29%288.9.6%29.pdf>
5. <a id="Ref_SMP_ADMIN_GUIDE" class="anchor"></a>\[SMP_ADMIN_GUIDE\] SMP Administration Guide - SMP 4.2, <https://ec.europa.eu/digital-building-blocks/wikis/download/attachments/551518321/%28eDelivery%29%28SMP%29%28AG%29%28SMP%204.2-FR%29%283.5%29.pdf>
6. <a id="Ref_UG-SDCG" class="anchor"></a>\[UG-SDCG\] Harmony eDelivery Access - Static Discovery Configuration Guide. Document ID: [UG-SDCG](static_discovery_configuration_guide.md)

## 2. Dynamic Discovery for Sending Parties

### 2.1 Prerequisites

Before starting the dynamic discovery configuration process, please complete the Access Point installation according to the installation guide:

- Harmony eDelivery Access - Access Point Installation Guide \[[IG-AS](harmony-ap_installation_guide.md)\].
  - **Note:** During the installation, when the system asks do you want the Access Point installation to use dynamic discovery, please answer **Yes**.

### 2.2 Configuring Dynamic Discovery in Sender AP

Sending parties (corners `C1` and `C2`) do not have to be registered in SMP or SML, only receiving parties do. Instead, 
to enable dynamic discovery, sending parties must configure the values of the following properties in the 
`/etc/harmony-ap/domibus.properties` configuration file. The values are configured automatically during the Access Point
installation process, but they can be changed manually afterwards.

```properties
# Whether to use dynamic discovery or not
domibus.dynamicdiscovery.useDynamicDiscovery=true

# The SML zone
domibus.smlzone=<SML_ZONE>

# The dynamic discovery client to be used for the dynamic process. 
# Possible values: OASIS and PEPPOL. Defaults to OASIS.
domibus.dynamicdiscovery.client.specification=OASIS

# Specifies the PEPPOL dynamic discovery client mode: PRODUCTION or TEST mode.
# Defaults to TEST.
#domibus.dynamicdiscovery.peppolclient.mode=TEST
```

If you're not sure about the correct values of the `domibus.smlzone`, `domibus.dynamicdiscovery.client.specification` and
`domibus.dynamicdiscovery.peppolclient.mode` properties, please contact the domain authority of the policy domain where
the Access Point is registered.

In addition, some changes are required in the sending party's PMode. Since the receiver of the message isn't known beforehand,
the `PMode.Responder` parameter should not be set. In practice, the `process` element in PMode must not contain 
`responderParties` element.

```xml
...
<process name="tc1Process" initiatorRole="defaultInitiatorRole" responderRole="defaultResponderRole" mep="oneway" binding="push">
    <initiatorParties>
        <initiatorParty name="senderalist"/>
    </initiatorParties>
    <!-- no responderParties element -->
    <legs>
        <leg name="pushTestcase1tc1Action"/>
        <leg name="testServiceCase"/>
    </legs>
</process>
...
``` 

Similarly, in the AS4 message that's sent the `/UserMessage/PartyInfo/To` element must be empty. Instead, the final 
recipient (`C4`) party is identified using a property named `finalRecipient`. The original sender (`C1`) party is identified 
using a property named `originalSender`.

```xml
<ns:UserMessage>
    <ns:PartyInfo>
        <ns:From>
            <ns:PartyId type="urn:fdc:peppol.eu:2017:identifiers:ap">senderalias</ns:PartyId>
            <ns:Role> http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator</ns:Role>
        </ns:From>
        <ns:To>
        </ns:To>
    </ns:PartyInfo>
    <ns:MessageProperties>
        <ns:Property name="originalSender" type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">C1</ns:Property>
        <ns:Property name="finalRecipient" type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">C4</ns:Property>
    </ns:MessageProperties>
...
<ns:UserMessage>
```

**Note:** Also, the following elements in the PMode and AS4 message must match with the values defined by the receiving
party (`C3`):

- document identifier scheme;
- document identifier;
- process scheme;
- process identifier.

See sections [3.6.3](#363-specifying-document-identifier-scheme-and-document-identifier-in-ap-pmode-and-as4-message)
and [3.6.4](#364-specifying-process-scheme-and-process-identifier-in-ap-pmode-and-in-as4-message) for details.
              
See the Domibus Administration Guide \[[DOMIBUS_ADMIN_GUIDE](#Ref_DOMIBUS_ADMIN_GUIDE])\] for more details.

### 2.3 TLS Configuration and Certificates

Public certificates of trusted data exchange parties must be imported to sign and TLS truststores. When dynamic discovery 
is used, the public certificates of trusted data exchange parties include sign and TLS certificates of receiving party's 
Access Point (`C3`) and SMP. See sections 2.2-2.6 of the Static Discovery Configuration Guide \[[UG-SDCG](static_discovery_configuration_guide.md)\] 
for details on managing the configuration. Please note that SMP is not mentioned in the Static Discovery Configuration 
Guide, because it's not used with static discovery. However, with dynamic discovery also the SMP sign and TLS certificates
must be imported following the same principles.

## 3. Dynamic Discovery for Receiving Parties

### 3.1 Prerequisites

Before starting the dynamic discovery configuration process, please complete the Access Point and SMP installation according to the corresponding installation guide:

- Harmony eDelivery Access - Access Point Installation Guide \[[IG-AS](harmony-ap_installation_guide.md)\];
  - **Note:** During the installation, when the system asks do you want the Access Point installation to use dynamic discovery, 
  please answer **No** if the Access Point is going to be used for receiving messages only. If the Access Point is going to 
  be used for sending messages too, then please answer **Yes**.
- Harmony eDelivery Access - Service Metadata Publisher Installation Guide \[[IG-SMP](harmony-smp_installation_guide.md)\].
  - **Note:** During the installation, when the system asks do you want the SMP installation to publish information to 
  some Service Metadata Locator (SML), please answer **Yes**.

### 3.2 Keys and Certificates

To register SMP to SML the following certificates with private keys are needed and must be present in the SMP sign keystore (`/etc/harmony-smp/smp-keystore.jks`):

- Client certificate and private key for TLS connections to SML ("SML ClientCert" in the SMP UI).
- Metadata signing certificate and private key for signing service metadata ("Response signature Certificate" in the SMP UI).

Technically, it's possible to use the same key as an SML client certificate and a response signature certificate. By default, 
a self-signed sign certificate with a private key that are automatically generated during the installation process are
available in the SMP sign keystore. Also, a self-signed TLS certificate with a private key are automatically generated
during the installation process. They're available in the TLS keystore (`/etc/harmony-smp/tls-keystore.jks`).

Certificates used by the SMP must be trusted by the SML and Access Points using the SMP (=Access Points in a sending role). 
The SMP sign keystore can be managed in the SMP admin UI by clicking the "Edit keystore" button under the "Domain" section. 
In case the sign keystore and/or sign trustore need to be accessed on command line, their passwords can be queried from 
the configuration database using the following command:

```bash
sudo mysql -e "use harmony_smp; select PROPERTY, VALUE from SMP_CONFIGURATION where PROPERTY in('smp.keystore.password', 'smp.truststore.password');"
```

The value of the properties is in format `{DEC}{$PASSWORD}` where `$PASSWORD` is the sign keystore/truststore password.
The Static Discovery Configuration Guide \[[UG-SDCG](static_discovery_configuration_guide.md)\] contains a lot of examples
how to manage keystores and truststores on command line.

Instead, the SMP TLS keystore can be managed on command line only. The password of the TLS keystore can be found in 
the `/etc/harmony-smp/tomcat-conf/server.xml` file in the `keystorePass` property. 

The TLS certificate used by the SML must be trusted by the SMP. It means that the SML's public TLS certificate 
must be imported to the SMP's TLS truststore (`/etc/harmony-smp/tls-truststore.jks`). The password of the TLS truststore 
can be found in the `/etc/harmony-smp/tomcat-conf/server.xml` file in the `truststorePass` property.

**Note:** The certificate requirements depend on the eDelivery policy domain. If you're not sure about the requirements, please 
contact the domain authority of the policy domain where the SMP is registered.

### 3.3 Registering SMP in SML

First, the SMP server must be registered in SML. Please note that a single SMP instance can provide metadata for multiple SML domains. 

An SMP server is registered in SML by completing the steps below:

- Log in to the SMP UI using a user with the `SYSTEM_ADMIN` role.
  - Only a user with the `SYSTEM_ADMIN` role can register, unregister and change the SMP registration.
- Open the "Domain" section.
- Click the "New" button and the "New Domain" dialog appears.
- Provide the following information:
  - Domain properties:
      - Domain Code
      - SML domain
      - Response signature Certificate (Signature CertAlias)
  - SML integration data:
    - SML SMP identifier
    - SML ClientCert Alias
      - SML clientCert Header is populated automatically once SML ClientCert Alias is selected.
- Click the "OK" button to close the "New Domain" dialog.
- Click the "Save" button to save the changes.
  - **Note:** Changes are not saved if the "Save" button is not clicked.
- Select the new domain and click the "Register" button to send a registration request to SML.
  - If sending the registration request fails because of an authentication issue, please contact the SML admin for details.

See the SMP Administration Guide \[[SMP_ADMIN_GUIDE](#Ref_SMP_ADMIN_GUIDE])\] for more details.

### 3.4 Registering Final Recipient in SML

Only the final recipient, corner `C4` in four corner topology, is registered in SML. In other words, the other parties - 
original sender (`C1`), initiator (`C2`) and responder (`C3`) - don't need to be registered in SML. Note that `C1` and
`C2`, as well as `C3` and `C4` can be the same organisation.

A final recipient is registered using its identifier. Allowed identifier types and values depend on the eDelivery 
policy domain. Identifiers consist of identifier type and identifier value. These values may be represented as two separate 
fields or as a single field where type and value are concatenated.

In the SMP UI and documentation a final recipient is represented by "ServiceGroup". The final recipient (`C4`) party is 
identified by the fields "Participant identifier" and "Participant scheme".

A final recipient (`C4`) party is registered in SML by completing the steps below:

- Log in to the SMP UI using a user with the `SMP_ADMIN` role.
  - Only a user with the `SMP_ADMIN` role can add, delete and modify final recipients.
- Open the "Edit" section.
- Click the "New" button and the "New ServiceGroup" dialog appears.
- Provide the following information:
  - Participant identifier
  - Participant scheme. There are two supported alternatives a) and b):
    1. Starts with `urn:oasis:names:tc:ebcore:partyid-type:(iso6523:|unregistered:)`.
    2. Is up to 25 characters long with form `[domain]-[identifierArea]-[identifierType]` (e.g.: `busdox-actorid-upis`) and may only contain the following characters: `[a-z0-9]`. 
  - Owners
  - Domains
- Click the "OK" button to close the "New ServiceGroup" dialog.
- Click the "Save" button to save the changes.
  - **Note:** Changes are not saved if the "Save" button is not clicked.

After saving, it's possible to review the new `ServiceGroup` record by clicking the "OASIS ServiceGroup URL" link. The
record should look like this:

```xml
<ServiceGroup>
    <ParticipantIdentifier scheme="urn:oasis:names:tc:ebcore:partyid-type:unregistered">c4</ParticipantIdentifier>
    <ServiceMetadataReferenceCollection/>
</ServiceGroup>
```

See the SMP Administration Guide \[[SMP_ADMIN_GUIDE](#Ref_SMP_ADMIN_GUIDE])\] for more details.

### 3.5 Registering Services in SMP

Services provided by the final recipient are registered in SMP where interested parties can retrieve metadata about them.

A service is registered in SMP by completing the steps below:

- Log in to the SMP UI using a user with the `SMP_ADMIN` or `SERVICE_GROUP_ADMIN` (with permission to specific ServiceGroup) role.
  - Only a user with the `SMP_ADMIN` or `SERVICE_GROUP_ADMIN` (with permission to specific ServiceGroup) role can add, delete and modify services.
- Open the "Edit" section.
- On the row of the participant who owns the service, click the "Add service metadata" icon in the "Actions" column and the "New ServiceMetada" dialog appears.
- Click the "Metadata wizard" button and the "ServiceMetadaWizard" appears.
- Provide the following information:
  - Document identifier scheme (*optional*).
  - Document identifier.
- Click the "Generate XML" button and an XML template appears.
- Update the following elements:
  - Process scheme (`[enterProcessType]`).
  - Process identifier (`[enterProcessName]`).
  - Transport profile.
    - The default is `bdxr-transport-ebms3-as4-v1p0`.
  - Endpoint URL.
    - The URL of the Access Point (`C3`) where the AS4 requests are sent, e.g., `https://<HOST>:8443/services/msh`.
  - Upload certificate.
    - Certificate for encrypting messages. Note that this is the content encryption certificate of Access Point (`C3`), not the certificate of the final recipient.
  - Service description.
  - Technical Contact URL.
- Click the "OK" button to close the "ServiceMetadaWizard" dialog.
- Click the "OK" button to close the "New ServiceMetada" dialog.
- Click the "Save" button to save the changes.
  - **Note:** Changes are not saved if the "Save" button is not clicked.

After saving, it's possible to review the new `ServiceMetadata` record by clicking the "URL" link. The
record should look like this:

```xml
<SignedServiceMetadata>
   <ServiceMetadata>
      <ServiceInformation>
         <ParticipantIdentifier scheme="urn:oasis:names:tc:ebcore:partyid-type:unregistered">c4</ParticipantIdentifier>
         <DocumentIdentifier scheme="docidscheme">TC1Leg1</DocumentIdentifier>
         <ProcessList>
            <Process>
               <ProcessIdentifier scheme="testscheme">bdx:noprocess</ProcessIdentifier>
               <ServiceEndpointList>
                  <Endpoint transportProfile="bdxr-transport-ebms3-as4-v1p0">
                     <EndpointURI>https://<HOST>:8443/services/msh</EndpointURI>
                     <Certificate>...</Certificate>
                     <ServiceDescription />
                     <TechnicalContactUrl />
                  </Endpoint>
               </ServiceEndpointList>
            </Process>
         </ProcessList>
      </ServiceInformation>
   </ServiceMetadata>
   <Signature>
      <SignedInfo>
         <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315" />
         <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256" />
         <Reference URI="">
            <Transforms>
               <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature" />
            </Transforms>
            <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256" />
            <DigestValue>...</DigestValue>
         </Reference>
      </SignedInfo>
      <SignatureValue>...</SignatureValue>
      <KeyInfo>
         <X509Data>
            <X509SubjectName>...</X509SubjectName>
            <X509Certificate>...</X509Certificate>
         </X509Data>
      </KeyInfo>
   </Signature>
</SignedServiceMetadata>
```

See the SMP Administration Guide \[[SMP_ADMIN_GUIDE](#Ref_SMP_ADMIN_GUIDE])\] for more details.

### 3.6 Configuring Dynamic Discovery in Receiving AP

To use dynamic discovery, some changes are required in the receiving party's (`C3`) PMode. Since the sender of the message 
is not known beforehand, the `PMode.Initiator` parameter must not be set. In practice, the `process` element in PMode 
must not contain `initiatorParties` element.

```xml
...
<process name="tc1Process" initiatorRole="defaultInitiatorRole" responderRole="defaultResponderRole" mep="oneway" binding="push">
    <!-- no initiatorParties element -->
    <responderParties>
        <responderParty name="receiveralias"/>
    </responderParties>
    <legs>
        <leg name="pushTestcase1tc1Action"/>
        <leg name="testServiceCase"/>
    </legs>
</process>
...
``` 

See the Domibus Administration Guide \[[DOMIBUS_ADMIN_GUIDE](#Ref_DOMIBUS_ADMIN_GUIDE])\] for more details.

#### 3.6.1 TLS Configuration and Certificates

Public certificates of trusted data exchange parties must be imported to sign and TLS truststores. When dynamic discovery 
is used, the public certificates of trusted data exchange parties include sign and TLS certificates of sending party's 
Access Point (`C2`). See sections 2.2-2.6 of the Static Discovery Configuration Guide \[[UG-SDCG](static_discovery_configuration_guide.md)\] 
for details on managing the configuration.

#### 3.6.2 Final Recipient in Plugin Configuration and AS4 Message

In the Access Point (`C3`) configuration, the final recipient (`C4`) is represented as a plugin user. The recipient is 
identified by single field `Original user`. This field must contain identifier type concatenated with identifier 
value, separated by `:`.

In AS4 messages the final recipient (and also original sender) can be represented in two ways:

- as a single identifier value, like in Access Point plugin user configuration;
- as a identifier value and corresponding type attribute.

Both these excerpts are valid and equal representations of the final recipient:

With type attribute:

    <Property name="finalRecipient" type="urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088">8735822991022</Property>

As a single identifier value without type attribute:

    <Property name="finalRecipient">urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088:8735822991022</Property>

#### 3.6.3 Specifying Document Identifier Scheme and Document Identifier in AP PMode and AS4 Message

In PMode the following excerpt corresponds to document with scheme `docidscheme` and identifier `documentidvalue`:

		<actions>
			<action name="someAction" value="docidscheme::documentidvalue"/>
               
            ...

		</actions>

In AS4 message the same document type is referenced as:

        <CollaborationInfo>
            <Action>docidscheme::documentidvalue</Action>

            ...

        </CollaborationInfo>

**Note:** The document scheme is not mandatory and can be omitted. In that case, also `::` can be omitted.

#### 3.6.4 Specifying Process Scheme and Process Identifier in AP PMode and in AS4 Message

In PMode the following excerpt corresponds to process with scheme `servicetype` and identifier `bdx:noprocess`:

        <services>
            <service name="someservicename" value="bdx:noprocess" type="servicetype"/>
        </services>

In AS4 message the same process is specified as:

        <CollaborationInfo>
            <Service type="servicetype">bdx:noprocess</Service>

             ...

        </CollaborationInfo>

