# Dynamic discovery configuration guide

This document describes configuration of participants and services for edelivery dynamic discovery. 

## Terms and abbreviations

SML - Service Metadata Locator, program that manages DNS records for dynamic discovery.
SMP - Service Metadata Provider, program that contains public service that interested parties can use to get services metadata.
AP - Access Point, program actually sending and receiving messages in eDelivery network.

## Registering SMP to SML

Before actual services can be registered in SML SMP server itself has to be registered. Note that a single SMP
installation can provide metadata for multiple SML domains.

Only user with SYSTEM role can register, unregister and change SMP registrations in SML.

To register SMP to SML two certificates with private keys are needed and must be present in SMP keystore (/etc/harmony-smp/smp-keystore.jks):

- Client certificate and private key for TLS connections to SML (SML ClientCert in SMP UI)
- Metadata signing certificate and private key for signing service metadata (Response signature Certificate in SMP UI)

SMP keystore can be managed in UI under "Domain" section, button "Edit keystore".

Certificates must be trusted by SML and access points using SMP. Requirements depend on eDelivery network used.

## Registering final recipients in SML

Only message final recipients (corner C4 in four corner topology) are registered in SML. In other words other parties:
initiator (C2), responder (C3) and original sender (C1) can not be and dont need to be registered in SML. Note that C1 and
C2, as well as C3 and C4 can be the same person.

Final recipients can be registered in SML by users with SMP_ADMIN role only.

Final recipient is registered using its identifier. Allowed identifier values depend on eDelivery network used.

Identifiers consist of identifier type and identifier value. In different places these values are represented as two separate fields
or as a single field where type and value are concatenated.

In SMP UI and documentation final recipient is represented by "Servicegroup". Recipient is identified by servicegroup fields
"Participant identifier" and "Participant scheme".

In AP configuration final recipient is represented as plugin user. Recipient is identified by single field "Original user".
This field must contain identifier type concatenated with identifier value, separated by "::".

In actual AS4 messages exchanged recipient (and actually also original sender) can be represented in two ways: as a single 
identifier value, like in AP plugin user configuration, or as a identifier value and corresponding type attribute.

Both these excerpts are valid and equal representations of final recipient:

With type attribute:

    <Property name="finalRecipient" type="urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088">8735822991022</Property>

Without type attribute:

    <Property name="finalRecipient">urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088:8735822991022</Property>

## Registering final recipient services in SMP

Actual services of recipients are not registering in SML. Instead they are registered only in SMP, where interested parties
can retrieve information about them.

Recipients services can be registered by SMP users with roles SMP_ADMIN or SERVICEGROUP_ADMIN (with permission to specific servicegroup).

To register recipient service in SMP following fields have to be filled:

- document identifier scheme and document identifier
- process scheme and process identifier
- service endpoint url - actual AP url where SOAP request must be sent
- certificate for encrypting messages - note that this is the certificate of AP (C3 in four corner model) not the certificate
of final recipient.

### Specifying process scheme and process identifier in AP PMode and in AS4 message

In PMODE the following excerpt corresponds to process with scheme "servicetype" and identifier "bdx:noprocess"

        <services>
            <service name="someservicename" value="bdx:noprocess" type="servicetype"/>
        </services>

In AS4 message the same process is specified as:

        <CollaborationInfo>
            <Service type="servicetype">bdx:noprocess</Service>

             ...

        </CollaborationInfo>

### Specifying document identifier scheme and document identifier in AP PMode and AS4 message

In PMode the following excerpt corresponds to document with scheme "docidscheme" and identifier "documentidvalue":

		<actions>
			<action name="someAction" value="docidscheme::documentidvalue"/>
               
            ...

		</actions>

In AS4 message the same document type is referenced as:

        <CollaborationInfo>
            <Action>docidscheme::documentidvalue</Action>

            ...

        </CollaborationInfo>
