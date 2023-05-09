--  Changeset src/main/resources/db/changelog-5.1-multi-tenancy-delta.xml::EDELIVERY-9563::Razvan Cretu
CREATE TABLE TB_PARTY_STATUS (ID_PK BIGINT AUTO_INCREMENT NOT NULL, PARTY_NAME VARCHAR(100) NOT NULL, CONNECTIVITY_STATUS VARCHAR(50) NOT NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_PARTY_STATUS PRIMARY KEY (ID_PK));

ALTER TABLE TB_PARTY_STATUS ADD CONSTRAINT UK_PARTY_NAME UNIQUE (PARTY_NAME);

--  Changeset src/main/resources/db/changelog-5.1-multi-tenancy-delta.xml::EDELIVERY-10064-mysql-migration-plans-are-failing::Razvan Cretu
ALTER TABLE TB_COMMAND_PROPERTY MODIFY FK_COMMAND BIGINT NOT NULL;

ALTER TABLE TB_COMMAND MODIFY SERVER_NAME VARCHAR(255) NOT NULL;

