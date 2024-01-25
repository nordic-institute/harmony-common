--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml
--  Ran at: 11/21/23 7:33 PM
--  Against: null@offline:mysql?changeLogFile=/Users/dragusa/work/domibus_sql/domibus-sql/domibus-msh-sql-scripts/target/liquibase/changelog-5.1-to-5.1.1-delta.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::EDELIVERY-12048-mysql::Gabriel Maier
DROP INDEX IDX_SIG_MESS_SIGNAL_MESS_ID ON TB_SIGNAL_MESSAGE;

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::EDELIVERY-11903::Cosmin Baciu
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000003', 'keystore-synchronization.lock');

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::EDELIVERY-12066::Cosmin Baciu
DROP TABLE IF EXISTS TB_FINAL_RECIPIENT_URL;

CREATE TABLE TB_DDC_LOOKUP (ID_PK BIGINT AUTO_INCREMENT NOT NULL, FINAL_RECIPIENT VARCHAR(255) NULL, ENDPOINT_URL VARCHAR(1000) NULL, PARTY_NAME VARCHAR(255) NOT NULL, PARTY_TYPE VARCHAR(255) NOT NULL, PARTY_PROCESSES VARCHAR(500) NOT NULL, CERT_ISSUER_SUBJECT VARCHAR(500) NOT NULL, CERT_CN VARCHAR(255) NOT NULL, CERT_SUBJECT VARCHAR(255) NOT NULL, CERT_SERIAL VARCHAR(255) NOT NULL, CERT_FINGERPRINT VARCHAR(255) NOT NULL, DDC_LOOKUP_TIME timestamp NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_DDC_CERTIFICATE PRIMARY KEY (ID_PK));

ALTER TABLE TB_DDC_LOOKUP ADD CONSTRAINT UK_DDC_FINAL_RECIPIENT UNIQUE (FINAL_RECIPIENT);

CREATE INDEX IDX_DDC_PARTY_NAME ON TB_DDC_LOOKUP(PARTY_NAME);

CREATE INDEX IDX_DDC_CERT_CN ON TB_DDC_LOOKUP(CERT_CN);

CREATE INDEX IDX_DDC_DDC_LOOKUP_TIME ON TB_DDC_LOOKUP(DDC_LOOKUP_TIME);

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::12066-1::Cosmin Baciu
CREATE INDEX IDX_DDC_FINAL_RECIPIENT ON TB_DDC_LOOKUP(FINAL_RECIPIENT);

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::EDELIVERY-11796-add-pks-common::Gabriel Maier
ALTER TABLE TB_USER_ROLES DROP FOREIGN KEY FK_USER_ROLES_ROLE;

ALTER TABLE TB_USER_ROLES DROP FOREIGN KEY FK_USER_ROLES_USER;

ALTER TABLE TB_USER_ROLES DROP PRIMARY KEY;

CREATE UNIQUE INDEX IDX_TB_USER_ROLES_USER_ROLE_UK ON TB_USER_ROLES(USER_ID, ROLE_ID);

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT FK_USER_ROLES_ROLE FOREIGN KEY (USER_ID) REFERENCES TB_USER (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT FK_USER_ROLES_USER FOREIGN KEY (ROLE_ID) REFERENCES TB_USER_ROLE (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::EDELIVERY-11796-add-pks-mysql::Gabriel Maier
ALTER TABLE TB_COMMAND_PROPERTY ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_PM_JOIN_PAYLOAD_PROFILE ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_PM_JOIN_PROCESS_INIT_PARTY ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_PM_JOIN_PROCESS_LEG ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_PM_JOIN_PROCESS_RESP_PARTY ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_PM_JOIN_PROPERTY_SET ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_USER_ROLES ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

ALTER TABLE TB_VERSION ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY;

--  Changeset src/main/resources/db/upgrade/5.1.1/changelog-5.1-to-5.1.1-delta.xml::EDELIVERY-10284_mysql::Gabriel Maier
ALTER TABLE WS_PLUGIN_TB_BACKEND_MSG_LOG MODIFY MESSAGE_ENTITY_ID BIGINT NOT NULL;

--  Changeset src/main/resources/db/upgrade/5.1.1/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.1.1', '2023-11-21 17:32', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='2023-11-21 17:32', CREATION_TIME = (UTC_TIMESTAMP);