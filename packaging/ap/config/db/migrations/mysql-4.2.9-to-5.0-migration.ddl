--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/archive/5.0/changelog-5.0-delta.xml
--  Ran at: 5/15/23 12:13 PM
--  Against: null@offline:mysql?changeLogFile=/home/hyoty/projects/niis/harmony-access-point/Core/Domibus-MSH-db/target/liquibase/changelog-5.0-delta.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-7836::gautifr
--  create DOMIBUS_SCALABLE_SEQUENCE sequence
CREATE TABLE DOMIBUS_SCALABLE_SEQUENCE (sequence_name VARCHAR(255) NOT NULL, next_val BIGINT NULL, CONSTRAINT PK_DOMIBUS_SCALABLE_SEQUENCE PRIMARY KEY (sequence_name));

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-2427::Francois Gautier
--  WS Plugin specific tables for backend dispatch
CREATE TABLE WS_PLUGIN_TB_BACKEND_MSG_LOG (ID_PK BIGINT AUTO_INCREMENT NOT NULL COMMENT 'Primary key identifying the record of the table', CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, MESSAGE_ID VARCHAR(255) NOT NULL, FINAL_RECIPIENT VARCHAR(255) NULL, ORIGINAL_SENDER VARCHAR(255) NULL, BACKEND_MESSAGE_STATUS VARCHAR(255) NULL, MESSAGE_STATUS VARCHAR(255) NULL, BACKEND_MESSAGE_TYPE VARCHAR(255) NULL, RULE_NAME VARCHAR(255) NULL COMMENT 'Name of the rule used for dispatching', SENT datetime NOT NULL COMMENT 'DateTime when the message had been dispatched', FAILED datetime NULL COMMENT 'DateTime when the message had failed', SEND_ATTEMPTS INT NULL COMMENT 'Number of attempts sent', SEND_ATTEMPTS_MAX INT NULL COMMENT 'Number of attempts maximal', NEXT_ATTEMPT datetime NULL COMMENT 'DateTime for the next attempt', SCHEDULED BIT(1) NULL COMMENT 'true if the backend message is already scheduled to be sent', CONSTRAINT PK_WS_PLUGIN_BACKEND_MSG_LOG PRIMARY KEY (ID_PK));

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-4808::Catalin Enache
ALTER TABLE WS_PLUGIN_TB_MESSAGE_LOG ADD CONVERSATION_ID VARCHAR(255) NULL, ADD REF_TO_MESSAGE_ID VARCHAR(255) NULL, ADD FROM_PARTY_ID VARCHAR(255) NULL, ADD ORIGINAL_SENDER VARCHAR(255) NULL;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-7472::Ion Perpegel
--  split plugin user unique constraints in 2
ALTER TABLE TB_AUTHENTICATION_ENTRY DROP KEY UQ_PLUGIN_USER;

ALTER TABLE TB_AUTHENTICATION_ENTRY ADD CONSTRAINT UK_PLUGIN_USER_NAME UNIQUE (USERNAME);

ALTER TABLE TB_AUTHENTICATION_ENTRY ADD CONSTRAINT UK_PLUGIN_USER_CERT UNIQUE (CERTIFICATE_ID);

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8384-temporary-drop-ui-replication-objects::Sebastian-Ion TINCU
DROP TABLE TB_MESSAGE_UI;

CREATE OR REPLACE VIEW V_MESSAGE_UI_DIFF AS SELECT 'Recreate view to prevent issues when dropping below caused by the old definer value of root@localhost';

DROP VIEW V_MESSAGE_UI_DIFF;

--  Changeset src/main/resources/db/archive/5.0/migration/common/changelog-5.0-alter-delta.xml::EDELIVERY-7840-Rename-unique-key-constraints-common::Soumya
ALTER TABLE TB_COMMAND_PROPERTY RENAME INDEX UNI_COMMAND_PROP_NAME TO UK_COMMAND_PROP_NAME;

ALTER TABLE TB_USER RENAME INDEX USER_NAME TO UK_USER_NAME;

ALTER TABLE TB_USER_ROLE RENAME INDEX ROLE_NAME TO UK_ROLE_NAME;

--  Changeset src/main/resources/db/archive/5.0/migration/common/changelog-5.0-alter-delta.xml::EDELIVERY-8384-primary-key-index-names-common::Sebastian-Ion TINCU
--  Changeset src/main/resources/db/archive/5.0/migration/single-tenancy/changelog-5.0-alter-delta.xml::EDELIVERY-7840-Rename-unique-key-constraints-single-tenancy::Soumya
ALTER TABLE TB_CERTIFICATE RENAME INDEX CERTIFICATE_ALIAS TO UK_CERTIFICATE_ALIAS;

ALTER TABLE TB_ENCRYPTION_KEY RENAME INDEX KEY_USAGE TO UK_KEY_USAGE;

-- TODO: DOES NOT EXIST?
-- ALTER TABLE TB_PM_LEG_MPC RENAME INDEX UK_7H5NW411791GF4LG1YH6SI1WD TO UK_LEG_MPC_PARTYMPCMAP_ID;

--  Changeset src/main/resources/db/archive/5.0/migration/single-tenancy/changelog-5.0-alter-delta.xml::EDELIVERY-7840-Rename-foreign-key-constraints-single-tenancy::Soumya
ALTER TABLE TB_REV_CHANGES
            DROP FOREIGN KEY FK_REV_CHANGES_REV,
            ADD CONSTRAINT FK_REV_CHANGES_REV_INFO FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO (`ID`);

ALTER TABLE TB_REV_CHANGES RENAME INDEX FK_REV_CHANGES_REV TO IDX_FK_REV_CHANGES_REV_INFO;

ALTER TABLE TB_PM_ACTION
            DROP FOREIGN KEY FK_6HLDNNQKMH6LUI555T4N9FJIV,
            ADD CONSTRAINT FK_ACTION_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_ACTION RENAME INDEX FK_6HLDNNQKMH6LUI555T4N9FJIV TO IDX_FK_ACTION_BP;

ALTER TABLE TB_PM_AGREEMENT
            DROP FOREIGN KEY FK_70M9KQPQAB3RGK90WSUV9VN64,
            ADD CONSTRAINT FK_AGREEMENT_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_AGREEMENT RENAME INDEX FK_70M9KQPQAB3RGK90WSUV9VN64 TO IDX_FK_AGREEMENT_BP;

ALTER TABLE TB_PM_PARTY
            DROP FOREIGN KEY FK_CLOYY9K391VHSUP85IWR8IXIV,
            ADD CONSTRAINT FK_PARTY_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_PARTY RENAME INDEX FK_CLOYY9K391VHSUP85IWR8IXIV TO IDX_FK_PARTY_BP;

ALTER TABLE TB_PM_CONFIGURATION
            DROP FOREIGN KEY FK_Q3GAPFC1E7HFBGMO0NEBJ4K1N,
            ADD CONSTRAINT FK_CONFIG_PARTY FOREIGN KEY (`FK_PARTY`) REFERENCES TB_PM_PARTY (`ID_PK`);

ALTER TABLE TB_PM_CONFIGURATION RENAME INDEX FK_Q3GAPFC1E7HFBGMO0NEBJ4K1N TO IDX_FK_CONFIG_PARTY;

ALTER TABLE TB_PM_CONFIGURATION
            DROP FOREIGN KEY FK_62DT1Y6O7OD1T0IQN7DOW8UQ5,
            ADD CONSTRAINT FK_CONFIG_BP FOREIGN KEY (`FK_BUSINESSPROCESSES`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_CONFIGURATION RENAME INDEX FK_62DT1Y6O7OD1T0IQN7DOW8UQ5 TO IDX_FK_CONFIG_BP;

ALTER TABLE TB_PM_CONFIGURATION_AUD
            DROP FOREIGN KEY FK_CONFIGURATION_AUD_REV,
            ADD CONSTRAINT FK_CONFIG_AUD_REV FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO (`ID`);

ALTER TABLE TB_PM_CONFIGURATION_AUD RENAME INDEX FK_CONFIGURATION_AUD_REV TO IDX_FK_CONFIG_AUD_REV;

ALTER TABLE TB_PM_CONFIGURATION_RAW_AUD
            DROP FOREIGN KEY FK_CONFIGURATION_RAW_AUD,
            ADD CONSTRAINT FK_CONFIG_RAW_AUD FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO (`ID`);

ALTER TABLE TB_PM_CONFIGURATION_RAW_AUD RENAME INDEX FK_CONFIGURATION_RAW_AUD TO IDX_FK_CONFIG_RAW_AUD;

ALTER TABLE TB_PM_ERROR_HANDLING
            DROP FOREIGN KEY FK_L1MMHS1TBT8PW7VX5TEUYTJVJ,
            ADD CONSTRAINT FK_ERROR_HANDLING_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_ERROR_HANDLING RENAME INDEX FK_L1MMHS1TBT8PW7VX5TEUYTJVJ TO IDX_FK_ERROR_HANDLING_BP;

ALTER TABLE TB_PM_PAYLOAD
            DROP FOREIGN KEY FK_D9EL0L8U1GM5OEU67NQRKHERQ,
            ADD CONSTRAINT FK_PAYLOAD_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_PAYLOAD RENAME INDEX FK_D9EL0L8U1GM5OEU67NQRKHERQ TO IDX_FK_PAYLOAD_BP;

ALTER TABLE TB_PM_JOIN_PAYLOAD_PROFILE
            DROP FOREIGN KEY FK_Q0L3EJ6RUQCFUTRU2SECURQ9L,
            ADD CONSTRAINT FK_JP_PROFILE FOREIGN KEY (`FK_PAYLOAD`) REFERENCES TB_PM_PAYLOAD_PROFILE (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PAYLOAD_PROFILE RENAME INDEX FK_Q0L3EJ6RUQCFUTRU2SECURQ9L TO IDX_FK_JP_PROFILE;

ALTER TABLE TB_PM_JOIN_PAYLOAD_PROFILE
            DROP FOREIGN KEY FK_G20EOW5F2CKE3AVSRG6QPKUXV,
            ADD CONSTRAINT FK_JP_PAYLOAD FOREIGN KEY (`FK_PROFILE`) REFERENCES TB_PM_PAYLOAD (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PAYLOAD_PROFILE RENAME INDEX FK_G20EOW5F2CKE3AVSRG6QPKUXV TO IDX_FK_JP_PAYLOAD;

ALTER TABLE TB_PM_MEP_BINDING
            DROP FOREIGN KEY FK_277J83LGSQ3NMWYHSW637YND8,
            ADD CONSTRAINT FK_MEP_BINDING_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_MEP_BINDING RENAME INDEX FK_277J83LGSQ3NMWYHSW637YND8 TO IDX_FK_MEP_BINDING_BP;

ALTER TABLE TB_PM_ROLE
            DROP FOREIGN KEY FK_2SW6KNALOR7RPR04YE0O2R9AP,
            ADD CONSTRAINT FK_ROLE_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_ROLE RENAME INDEX FK_2SW6KNALOR7RPR04YE0O2R9AP TO IDX_FK_ROLE_BP;

ALTER TABLE TB_PM_MEP
            DROP FOREIGN KEY FK_EPMXKH8U7JNW5PBYJ47SIRMLH,
            ADD CONSTRAINT FK_MEP_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_MEP RENAME INDEX FK_EPMXKH8U7JNW5PBYJ47SIRMLH TO IDX_FK_MEP_BP;

ALTER TABLE TB_PM_PROCESS
            DROP FOREIGN KEY FK_TP11D8FG7CV1FWF5XKVWQPP34,
            ADD CONSTRAINT FK_PROCESS_MEP_BINDING FOREIGN KEY (`FK_MEP_BINDING`) REFERENCES TB_PM_MEP_BINDING (`ID_PK`);

ALTER TABLE TB_PM_PROCESS RENAME INDEX FK_TP11D8FG7CV1FWF5XKVWQPP34 TO IDX_FK_PROCESS_MEP_BINDING;

ALTER TABLE TB_PM_PROCESS
            DROP FOREIGN KEY FK_Q1JFSXFOJ3NL7HII3CO7BU0FR,
            ADD CONSTRAINT FK_PROCESS_INITIATOR_ROLE FOREIGN KEY (`FK_INITIATOR_ROLE`) REFERENCES TB_PM_ROLE (`ID_PK`);

ALTER TABLE TB_PM_PROCESS RENAME INDEX FK_Q1JFSXFOJ3NL7HII3CO7BU0FR TO IDX_FK_PROCESS_INIT_ROLE;

ALTER TABLE TB_PM_PROCESS
            DROP FOREIGN KEY FK_J7LAB5N5SUKLCLDQHXN8JL2JO,
            ADD CONSTRAINT FK_PROCESS_MEP FOREIGN KEY (`FK_MEP`) REFERENCES TB_PM_MEP (`ID_PK`);

ALTER TABLE TB_PM_PROCESS RENAME INDEX FK_J7LAB5N5SUKLCLDQHXN8JL2JO TO IDX_FK_PROCESS_MEP;

ALTER TABLE TB_PM_PROCESS
            DROP FOREIGN KEY FK_7P1G7SQVLI1SJ6K7VPHJO9IRC,
            ADD CONSTRAINT FK_PROCESS_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_PROCESS RENAME INDEX FK_7P1G7SQVLI1SJ6K7VPHJO9IRC TO IDX_FK_PROCESS_BP;

ALTER TABLE TB_PM_PROCESS
            DROP FOREIGN KEY FK_3EMT6XAGLH7676LPCKIY6R1VB,
            ADD CONSTRAINT FK_PROCESS_RESPONDER_ROLE FOREIGN KEY (`FK_RESPONDER_ROLE`) REFERENCES TB_PM_ROLE (`ID_PK`);

ALTER TABLE TB_PM_PROCESS RENAME INDEX FK_3EMT6XAGLH7676LPCKIY6R1VB TO IDX_FK_PROCESS_RESP_ROLE;

ALTER TABLE TB_PM_PROCESS
            DROP FOREIGN KEY FK_KJANLCDEAP7NIRDIGR7RT7P4V,
            ADD CONSTRAINT FK_PROCESS_AGREEMENT FOREIGN KEY (`FK_AGREEMENT`) REFERENCES TB_PM_AGREEMENT (`ID_PK`);

ALTER TABLE TB_PM_PROCESS RENAME INDEX FK_KJANLCDEAP7NIRDIGR7RT7P4V TO IDX_FK_PROCESS_AGRMNT;

ALTER TABLE TB_PM_JOIN_PROCESS_INIT_PARTY
            DROP FOREIGN KEY FK_OT8JFKOTD6QU7JRDAPTROHBN8,
            ADD CONSTRAINT FK_JOIN_INIT_PARTY_PRS FOREIGN KEY (`PROCESS_FK`) REFERENCES TB_PM_PROCESS (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROCESS_INIT_PARTY RENAME INDEX FK_OT8JFKOTD6QU7JRDAPTROHBN8 TO
            IDX_FK_JOIN_INIT_PARTY_PRS;

ALTER TABLE TB_PM_JOIN_PROCESS_INIT_PARTY
            DROP FOREIGN KEY FK_ASJDL57BUDMUWMJ3611969JPP,
            ADD CONSTRAINT FK_JOIN_INIT_PARTY FOREIGN KEY (`PARTY_FK`) REFERENCES TB_PM_PARTY (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROCESS_INIT_PARTY RENAME INDEX FK_ASJDL57BUDMUWMJ3611969JPP TO
            IDX_FK_JOIN_INIT_PARTY;

ALTER TABLE TB_PM_SECURITY
            DROP FOREIGN KEY FK_FWRQ81CCTU2NH0QEPRMPVKER9,
            ADD CONSTRAINT FK_SECURITY_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_SECURITY RENAME INDEX FK_FWRQ81CCTU2NH0QEPRMPVKER9 TO IDX_FK_SECURITY_BP;

ALTER TABLE TB_PM_SERVICE
            DROP FOREIGN KEY FK_KKHXS36RW15AYGPN00NVVGYXG,
            ADD CONSTRAINT FK_SERVICE_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_SERVICE RENAME INDEX FK_KKHXS36RW15AYGPN00NVVGYXG TO IDX_FK_SERVICE_BP;

ALTER TABLE TB_PM_RECEPTION_AWARENESS
            DROP FOREIGN KEY FK_3SGAXWFR5KOE3OLDBBD62SIWQ,
            ADD CONSTRAINT FK_REC_AWRNS_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_RECEPTION_AWARENESS RENAME INDEX FK_3SGAXWFR5KOE3OLDBBD62SIWQ TO IDX_FK_REC_AWRNS_BP;

ALTER TABLE TB_PM_RELIABILITY
            DROP FOREIGN KEY FK_LNNPLHIAWXX7WLT43YE3PEJ00,
            ADD CONSTRAINT FK_RELIABILITY_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_RELIABILITY RENAME INDEX FK_LNNPLHIAWXX7WLT43YE3PEJ00 TO IDX_FK_RELIABILITY_BP;

ALTER TABLE TB_PM_MPC
            DROP FOREIGN KEY FK_8Q319STWM1NOIJPB3JFL8JRI8,
            ADD CONSTRAINT FK_MPC_CONFIGURATION FOREIGN KEY (`FK_CONFIGURATION`) REFERENCES TB_PM_CONFIGURATION
            (`ID_PK`);

ALTER TABLE TB_PM_MPC RENAME INDEX FK_8Q319STWM1NOIJPB3JFL8JRI8 TO IDX_FK_MPC_CONFIGURATION;

ALTER TABLE TB_PM_MESSAGE_PROPERTY_SET
            DROP FOREIGN KEY FK_EIY3F9AHX0KDX4WXOI6PRYTN9,
            ADD CONSTRAINT FK_PROPERTY_SET_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_MESSAGE_PROPERTY_SET RENAME INDEX FK_EIY3F9AHX0KDX4WXOI6PRYTN9 TO IDX_FK_PROPERTY_SET_BP;

ALTER TABLE TB_PM_SPLITTING RENAME INDEX FK_SPLT_BP TO IDX_FK_SPLT_BP;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY UK_LEG_SPLT,
            ADD CONSTRAINT FK_LEG_SPLT FOREIGN KEY (`FK_SPLITTING`) REFERENCES TB_PM_SPLITTING
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_LEG_SPLT TO IDX_FK_LEG_SPLT;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_LE4RXNKI51EAK6XETC7FQ8SRI,
            ADD CONSTRAINT FK_LEG_PROPERTY_SET FOREIGN KEY (`FK_PROPERTY_SET`) REFERENCES TB_PM_MESSAGE_PROPERTY_SET
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_LE4RXNKI51EAK6XETC7FQ8SRI TO IDX_FK_LEG_PROPERTY_SET;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_J0Y3FCEDIQX5BUT8JBSCY59KW,
            ADD CONSTRAINT FK_LEG_ERROR_HANDLING FOREIGN KEY (`FK_ERROR_HANDLING`) REFERENCES TB_PM_ERROR_HANDLING
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_J0Y3FCEDIQX5BUT8JBSCY59KW TO IDX_FK_LEG_ERROR_HANDLING;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_DGTGTG41YRFEBYYSMKYI1GCAA,
            ADD CONSTRAINT FK_LEG_ACTION FOREIGN KEY (`FK_ACTION`) REFERENCES TB_PM_ACTION
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_DGTGTG41YRFEBYYSMKYI1GCAA TO IDX_FK_LEG_ACTION;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_AM7BWSM92SE3NVCLBEIEP5VWG,
            ADD CONSTRAINT FK_LEG_PAYLOAD_PROFILE FOREIGN KEY (`FK_PAYLOAD_PROFILE`) REFERENCES TB_PM_PAYLOAD_PROFILE
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_AM7BWSM92SE3NVCLBEIEP5VWG TO IDX_FK_LEG_PAYLOAD_PROFILE;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_9HPMOEWLITIVCL8RW1JOCCUAU,
            ADD CONSTRAINT FK_LEG_SECURITY FOREIGN KEY (`FK_SECURITY`) REFERENCES TB_PM_SECURITY
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_9HPMOEWLITIVCL8RW1JOCCUAU TO IDX_FK_LEG_SECURITY;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_KV2C5K41APDLVC6I18AR57ABE,
            ADD CONSTRAINT FK_LEG_SERVICE FOREIGN KEY (`FK_SERVICE`) REFERENCES TB_PM_SERVICE
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_KV2C5K41APDLVC6I18AR57ABE TO IDX_FK_LEG_SERVICE;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_RLBJSTVO4GPDCP6YSV9MOFQXJ,
            ADD CONSTRAINT FK_LEG_REC_AWRNS FOREIGN KEY (`FK_RECEPTION_AWARENESS`) REFERENCES TB_PM_RECEPTION_AWARENESS
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_RLBJSTVO4GPDCP6YSV9MOFQXJ TO IDX_FK_LEG_REC_AWRNS;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_R2SWPQOF7636VVEQT0DXL89DP,
            ADD CONSTRAINT FK_LEG_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_R2SWPQOF7636VVEQT0DXL89DP TO IDX_FK_LEG_BP;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_PPVXYURCVFY71FVEJW9BOSGHW,
            ADD CONSTRAINT FK_LEG_MPC FOREIGN KEY (`FK_MPC`) REFERENCES TB_PM_MPC
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_PPVXYURCVFY71FVEJW9BOSGHW TO IDX_FK_LEG_MPC;

ALTER TABLE TB_PM_LEG
            DROP FOREIGN KEY FK_13K2EPTP1EJP5OTHY1NJRG3P8,
            ADD CONSTRAINT FK_LEG_RELIABILITY FOREIGN KEY (`FK_RELIABILITY`) REFERENCES TB_PM_RELIABILITY
            (`ID_PK`);

ALTER TABLE TB_PM_LEG RENAME INDEX FK_13K2EPTP1EJP5OTHY1NJRG3P8 TO IDX_FK_LEG_RELIABILITY;

ALTER TABLE TB_PM_JOIN_PROCESS_LEG
            DROP FOREIGN KEY FK_GH98Q07KCJL7WDAON9GGIFQUX,
            ADD CONSTRAINT FK_JOIN_PROCESS_LEG FOREIGN KEY (`LEG_FK`) REFERENCES TB_PM_LEG
            (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROCESS_LEG RENAME INDEX FK_GH98Q07KCJL7WDAON9GGIFQUX TO IDX_FK_JOIN_PROCESS_LEG;

ALTER TABLE TB_PM_JOIN_PROCESS_LEG
            DROP FOREIGN KEY FK_FPXBDC63GIFACRD4QB6AFRQYW,
            ADD CONSTRAINT FK_JOIN_PROC_LEG_PROC FOREIGN KEY (`PROCESS_FK`) REFERENCES TB_PM_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROCESS_LEG RENAME INDEX FK_FPXBDC63GIFACRD4QB6AFRQYW TO IDX_FK_JOIN_PROC_LEG_PROC;

ALTER TABLE TB_PM_JOIN_PROCESS_RESP_PARTY
            DROP FOREIGN KEY FK_KYN19SWM143M96IN317SR97H2,
            ADD CONSTRAINT FK_JOIN_PROC_RESP_PARTY FOREIGN KEY (`PARTY_FK`) REFERENCES TB_PM_PARTY
            (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROCESS_RESP_PARTY RENAME INDEX FK_KYN19SWM143M96IN317SR97H2 TO
            IDX_FK_JOIN_PROC_RESP_PARTY;

ALTER TABLE TB_PM_JOIN_PROCESS_RESP_PARTY
            DROP FOREIGN KEY FK_HLB2Y2PRPM52SSYNN22H3SWSR,
            ADD CONSTRAINT FK_JN_PROC_RESP_PRT_PROC FOREIGN KEY (`PROCESS_FK`) REFERENCES TB_PM_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROCESS_RESP_PARTY RENAME INDEX FK_HLB2Y2PRPM52SSYNN22H3SWSR TO
            IDX_FK_JN_PROC_RESP_PRT_PROC;

ALTER TABLE TB_PM_MESSAGE_PROPERTY
            DROP FOREIGN KEY FK_RJ8H1B65VNJJGYFCJNVSWKGUH,
            ADD CONSTRAINT FK_MESSAGE_PROPERTY_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_MESSAGE_PROPERTY RENAME INDEX FK_RJ8H1B65VNJJGYFCJNVSWKGUH TO IDX_FK_MESSAGE_PROPERTY_BP;

ALTER TABLE TB_PM_JOIN_PROPERTY_SET
            DROP FOREIGN KEY FK_EKK8PN89Y50G22KD3GPJA7J39,
            ADD CONSTRAINT FK_JOIN_PROP_SET_PROP FOREIGN KEY (`PROPERTY_FK`) REFERENCES TB_PM_MESSAGE_PROPERTY_SET
            (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROPERTY_SET RENAME INDEX FK_EKK8PN89Y50G22KD3GPJA7J39 TO IDX_FK_JOIN_PROP_SET_PROP;

ALTER TABLE TB_PM_JOIN_PROPERTY_SET
            DROP FOREIGN KEY FK_MK54XE43F3HOKH7FJL3W66EFK,
            ADD CONSTRAINT FK_JOIN_PROPERTY_SET FOREIGN KEY (`SET_FK`) REFERENCES TB_PM_MESSAGE_PROPERTY
            (`ID_PK`);

ALTER TABLE TB_PM_JOIN_PROPERTY_SET RENAME INDEX FK_MK54XE43F3HOKH7FJL3W66EFK TO IDX_FK_JOIN_PROPERTY_SET;

ALTER TABLE TB_PM_LEG_MPC
            DROP FOREIGN KEY FK_NYLCXSY1F9CN3TDSKH97H8PT1,
            ADD CONSTRAINT FK_LEG_MPC_LEG FOREIGN KEY (`LEGCONFIGURATION_ID_PK`) REFERENCES TB_PM_LEG
            (`ID_PK`);

ALTER TABLE TB_PM_LEG_MPC
            DROP FOREIGN KEY FK_ORDDLTV2G3LQ79EU48C2MC2FY,
            ADD CONSTRAINT FK_LEG_MPC_PARTY FOREIGN KEY (`PARTYMPCMAP_KEY`) REFERENCES TB_PM_PARTY
            (`ID_PK`);

ALTER TABLE TB_PM_LEG_MPC RENAME INDEX FK_ORDDLTV2G3LQ79EU48C2MC2FY TO IDX_FK_LEG_MPC_PARTY;

ALTER TABLE TB_PM_LEG_MPC
            DROP FOREIGN KEY FK_7H5NW411791GF4LG1YH6SI1WD,
            ADD CONSTRAINT FK_LEG_MPC_MPC FOREIGN KEY (`PARTYMPCMAP_ID_PK`) REFERENCES TB_PM_MPC
            (`ID_PK`);

ALTER TABLE TB_PM_PARTY_AUD RENAME INDEX FK_PARTY_AUD_REV TO IDX_FK_PARTY_AUD_REV;

ALTER TABLE TB_PM_PARTY_ID_TYPE
            DROP FOREIGN KEY FK_ADVGUDIX024IRQPUGE4DL9IQF,
            ADD CONSTRAINT FK_PARTY_ID_TYPE_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_PARTY_ID_TYPE RENAME INDEX FK_ADVGUDIX024IRQPUGE4DL9IQF TO IDX_FK_PARTY_ID_TYPE_BP;

ALTER TABLE TB_PM_PARTY_IDENTIFIER
            DROP FOREIGN KEY FK_DESR6XTDP1LP41D5VENLHF4BC,
            ADD CONSTRAINT FK_PARTY_IDENT_PARTY FOREIGN KEY (`FK_PARTY`) REFERENCES TB_PM_PARTY
            (`ID_PK`);

ALTER TABLE TB_PM_PARTY_IDENTIFIER RENAME INDEX FK_DESR6XTDP1LP41D5VENLHF4BC TO IDX_FK_PARTY_IDENT_PARTY;

ALTER TABLE TB_PM_PARTY_IDENTIFIER
            DROP FOREIGN KEY FK_E7EHGHIFRNS83W6V3TPS7VPII,
            ADD CONSTRAINT FK_PARTY_IDENT_ID_TYPE FOREIGN KEY (`FK_PARTY_ID_TYPE`) REFERENCES TB_PM_PARTY_ID_TYPE
            (`ID_PK`);

ALTER TABLE TB_PM_PARTY_IDENTIFIER RENAME INDEX FK_E7EHGHIFRNS83W6V3TPS7VPII TO IDX_FK_PARTY_IDENT_ID_TYPE;

ALTER TABLE TB_PM_PARTY_IDENTIFIER_AUD
            DROP FOREIGN KEY FK_PARTY_IDENTIFIER_AUD_REV,
            ADD CONSTRAINT FK_PARTY_IDENT_AUD_REV FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO
            (`ID`);

ALTER TABLE TB_PM_PARTY_IDENTIFIER_AUD RENAME INDEX FK_PARTY_IDENTIFIER_AUD_REV TO
            IDX_FK_PARTY_IDENT_AUD_REV;

ALTER TABLE TB_PM_PARTY_ID_TYPE_AUD RENAME INDEX FK_PARTY_ID_TYPE_AUD_REV TO IDX_FK_PARTY_ID_TYPE_AUD_REV;

ALTER TABLE TB_PM_PAYLOAD_PROFILE
            DROP FOREIGN KEY FK_LYNOM7RXKC0T1XFL0NOWXPEUJ,
            ADD CONSTRAINT FK_PAYLOAD_PROFILE_BP FOREIGN KEY (`FK_BUSINESSPROCESS`) REFERENCES TB_PM_BUSINESS_PROCESS
            (`ID_PK`);

ALTER TABLE TB_PM_PAYLOAD_PROFILE RENAME INDEX FK_LYNOM7RXKC0T1XFL0NOWXPEUJ TO IDX_FK_PAYLOAD_PROFILE_BP;

ALTER TABLE TB_BACKEND_FILTER_AUD RENAME INDEX FK_BACKEND_FILTER_AUD_REV TO IDX_FK_BACKEND_FTR_AUD_REV;

ALTER TABLE TB_ROUTING_CRITERIA
            DROP FOREIGN KEY FK_I7GEI6BDR2CDN61HDOPLXBU7P,
            ADD CONSTRAINT FK_ROUT_CRIT_BACK_FTR FOREIGN KEY (`FK_BACKEND_FILTER`) REFERENCES TB_BACKEND_FILTER
            (`ID_PK`);

ALTER TABLE TB_ROUTING_CRITERIA RENAME INDEX FK_I7GEI6BDR2CDN61HDOPLXBU7P TO IDX_FK_ROUT_CRIT_BACK_FTR;

ALTER TABLE TB_ROUTING_CRITERIA_AUD
            DROP FOREIGN KEY FK_ROUTING_CRITERIA_AUD_REV,
            ADD CONSTRAINT FK_ROUT_CRIT_AUD_REV FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO
            (`ID`);

ALTER TABLE TB_ROUTING_CRITERIA_AUD RENAME INDEX FK_ROUTING_CRITERIA_AUD_REV TO IDX_FK_ROUT_CRIT_AUD_REV;

ALTER TABLE TB_BACK_RCRITERIA_AUD
            DROP FOREIGN KEY FK_BACK_RCRITERIA_AUD,
            ADD CONSTRAINT FK_BCK_RCR_AUD_REV_INFO FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO
            (`ID`);

ALTER TABLE TB_BACK_RCRITERIA_AUD RENAME INDEX FK_BACK_RCRITERIA_AUD TO IDX_FK_BCK_RCR_AUD_REV_INFO;

ALTER TABLE TB_USER_AUD RENAME INDEX FK_USER_AUD_REV TO IDX_FK_USER_AUD_REV;

ALTER TABLE TB_USER_ROLE_AUD
            DROP FOREIGN KEY FK_USER_ROLE_AUD,
            ADD CONSTRAINT FK_USR_ROL_AUD_REV_INFO FOREIGN KEY (`REV`) REFERENCES TB_REV_INFO
            (`ID`);

ALTER TABLE TB_USER_ROLE_AUD RENAME INDEX FK_USER_ROLE_AUD TO IDX_FK_USR_ROL_AUD_REV_INFO;

--  Changeset src/main/resources/db/archive/5.0/migration/single-tenancy/changelog-5.0-alter-delta.xml::EDELIVERY-8384-primary-key-index-names-single-tenancy::Sebastian-Ion TINCU
--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-7668::Catalin Enache
CREATE TABLE TB_VERSION (VERSION VARCHAR(30) NULL, BUILD_TIME VARCHAR(30) NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL) COMMENT='Stores Domibus version and build time';

ALTER TABLE TB_VERSION ADD CONSTRAINT UK_VERSION UNIQUE (VERSION);

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-4669::dussath
--  Create pull request table
CREATE TABLE TB_PULL_REQUEST (PULL_REQUEST_UUID VARCHAR(255) NOT NULL, MPC VARCHAR(255) NOT NULL, CONSTRAINT PK_PULL_REQUEST PRIMARY KEY (PULL_REQUEST_UUID));

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8384-longblob-data-type::Sebastian-Ion TINCU
ALTER TABLE TB_ENCRYPTION_KEY MODIFY SECRET_KEY LONGBLOB;

ALTER TABLE TB_ENCRYPTION_KEY MODIFY SECRET_KEY LONGBLOB NOT NULL;

ALTER TABLE TB_ENCRYPTION_KEY MODIFY INIT_VECTOR LONGBLOB;

ALTER TABLE TB_ENCRYPTION_KEY MODIFY INIT_VECTOR LONGBLOB NOT NULL;

ALTER TABLE TB_PM_CONFIGURATION_RAW MODIFY XML LONGBLOB;

ALTER TABLE TB_PM_CONFIGURATION_RAW_AUD MODIFY XML LONGBLOB;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8503_1::ion perpegel
CREATE TABLE TB_LOCK (ID_PK BIGINT AUTO_INCREMENT NOT NULL, LOCK_KEY VARCHAR(255) NOT NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_LOCK PRIMARY KEY (ID_PK)) COMMENT='Stores keys used for locking/synchronizing in cluster';

ALTER TABLE TB_LOCK ADD CONSTRAINT UK_LOCK_KEY UNIQUE (LOCK_KEY);

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8503_2::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000001', 'bootstrap-synchronization.lock');

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-9451::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000002', 'scheduler-synchronization.lock');

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8532::ion perpegel
CREATE TABLE TB_TRUSTSTORE (ID_PK BIGINT AUTO_INCREMENT NOT NULL, NAME VARCHAR(100) NOT NULL, TYPE VARCHAR(50) NOT NULL, PASSWORD VARCHAR(100) NOT NULL, CONTENT LONGBLOB NOT NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_TRUSTSTORE PRIMARY KEY (ID_PK));

ALTER TABLE TB_TRUSTSTORE ADD CONSTRAINT UK_NAME UNIQUE (NAME);

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8450::Francois Gautier
CREATE TABLE TB_EARCHIVE_BATCH (ID_PK BIGINT AUTO_INCREMENT NOT NULL, BATCH_ID VARCHAR(255) NULL, ORIGINAL_BATCH_ID VARCHAR(255) NULL COMMENT 'The original BATCH_ID from which this batch was generated/re-exported', REEXPORTED BIT(1) DEFAULT 0 NOT NULL COMMENT 'true if this batch messages were re-exported by a new batch', REQUEST_TYPE VARCHAR(255) NULL, BATCH_STATUS VARCHAR(255) NULL, DATE_REQUESTED timestamp NULL, BATCH_SIZE INT NULL, LAST_PK_USER_MESSAGE BIGINT NULL, FIRST_PK_USER_MESSAGE BIGINT NULL, ERROR_CODE VARCHAR(255) NULL, ERROR_DETAIL VARCHAR(255) NULL, MANIFEST_CHECK_SUM VARCHAR(255) NULL, STORAGE_LOCATION VARCHAR(255) NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_EARCHIVE_BATCH PRIMARY KEY (ID_PK));

CREATE UNIQUE INDEX IDX_EARCHIVE_BATCH_UNIQUE_ID ON TB_EARCHIVE_BATCH(BATCH_ID);

CREATE INDEX IDX_EARCHIVE_BATCH_STATUS ON TB_EARCHIVE_BATCH(BATCH_STATUS);

CREATE TABLE TB_EARCHIVEBATCH_UM (ID_PK BIGINT AUTO_INCREMENT NOT NULL, FK_EARCHIVE_BATCH_ID BIGINT NOT NULL, FK_USER_MESSAGE_ID BIGINT NOT NULL, MESSAGE_ID VARCHAR(255) NOT NULL, CONSTRAINT PK_EARCHIVEBATCH_UML PRIMARY KEY (ID_PK));

ALTER TABLE TB_EARCHIVEBATCH_UM ADD CONSTRAINT FK_EARCHIVE_BATCH_ID_FTR FOREIGN KEY (FK_EARCHIVE_BATCH_ID) REFERENCES TB_EARCHIVE_BATCH (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX IDX_TB_EARCHIVEBATCH_UM_ID ON TB_EARCHIVEBATCH_UM(FK_USER_MESSAGE_ID);

CREATE INDEX IDX_TB_EARCHIVEBATCH_UM_B_ID ON TB_EARCHIVEBATCH_UM(FK_EARCHIVE_BATCH_ID);

CREATE TABLE TB_EARCHIVE_START (ID_PK BIGINT AUTO_INCREMENT NOT NULL, LAST_PK_USER_MESSAGE BIGINT NULL, `DESCRIPTION` VARCHAR(255) NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_EARCHIVE_START PRIMARY KEY (ID_PK));

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8595::idragusa
DROP INDEX IDX_FK_EVENT ON TB_EVENT_ALERT;

DROP INDEX IDX_USER_ID ON TB_USER_ROLES;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8602-recreate-audit-views-with-rev-id-and-id-nullability-off::Sebastian-Ion TINCU
CREATE OR REPLACE VIEW V_AUDIT_DETAIL AS SELECT
            DISTINCT rc.GROUP_NAME as AUDIT_TYPE ,
            rc.MODIFICATION_TYPE as ACTION_TYPE,
            ri.USER_NAME as USER_NAME ,
            ri.REVISION_DATE as AUDIT_DATE,
            COALESCE(TRIM(CAST(rc.ENTITY_ID AS CHAR(255))), '') AS ID,
            COALESCE(TRIM(CAST(ri.ID AS CHAR(19))), '') AS REV_ID
            FROM TB_REV_INFO ri, TB_REV_CHANGES rc
            WHERE ri.ID=rc.REV
            UNION
            SELECT aa.AUDIT_TYPE,aa.MODIFICATION_TYPE,aa.USER_NAME,aa.REVISION_DATE,aa.ENTITY_ID,'1' FROM
            TB_ACTION_AUDIT aa;

CREATE OR REPLACE VIEW V_AUDIT AS SELECT *
            FROM V_AUDIT_DETAIL VAD
            ORDER BY VAD.AUDIT_DATE DESC;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8654::Cosmin Baciu
ALTER TABLE TB_PART_INFO ADD COMPRESSED BIT(1) DEFAULT 0 NULL;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-8895-single-tenancy-performance::Sebastian-Ion TINCU
ALTER TABLE TB_REV_CHANGES CHANGE ENTIY_NAME ENTITY_NAME VARCHAR(255);

DROP TABLE TB_USER_MSG_DELETION_JOB;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-delta.xml::EDELIVERY-4609::Soumya Chandran
ALTER TABLE TB_PART_INFO ADD PART_LENGTH BIGINT DEFAULT -1 NOT NULL;

--  Changeset src/main/resources/db/archive/5.0/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.0', '${DomibusBuildTime}', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='${DomibusBuildTime}', CREATION_TIME = (UTC_TIMESTAMP);

