--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/archive/changelog-5.0.2-delta.xml
--  Ran at: 5/15/23 12:13 PM
--  Against: null@offline:mysql?changeLogFile=/home/hyoty/projects/niis/harmony-access-point/Core/Domibus-MSH-db/target/liquibase/changelog-5.0.2-delta.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/archive/changelog-5.0.2-delta.xml::EDELIVERY-10221::Cosmin Baciu
CREATE TABLE TB_FINAL_RECIPIENT_URL (ID_PK BIGINT AUTO_INCREMENT NOT NULL, FINAL_RECIPIENT VARCHAR(255) NULL, ENDPOINT_URL VARCHAR(1000) NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_TB_FINAL_RECIPIENT_URL PRIMARY KEY (ID_PK));

CREATE INDEX TB_FINAL_RECIPIENT_URL_IDX ON TB_FINAL_RECIPIENT_URL(FINAL_RECIPIENT);

--  Changeset src/main/resources/db/archive/../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.0.2', '${DomibusBuildTime}', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='${DomibusBuildTime}', CREATION_TIME = (UTC_TIMESTAMP);

