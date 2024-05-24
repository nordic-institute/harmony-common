--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/upgrade/5.1.4/changelog-5.1.4-delta.xml
--  Ran at: 24/05/2024, 8.30
--  Against: null@offline:mysql?changeLogFile=/Users/diego.martin/Projects/domibus-sql/domibus-msh-sql-scripts/target/liquibase/changelog-5.1.4-delta.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/upgrade/5.1.4/../../common/changelog-before-migration-statements-v2.xml::EDELIVERY-12287_assert_previous_migration_succeeded-v2-mysql::Gabriel Maier
-- DELIMITER //

--  Changeset src/main/resources/db/upgrade/5.1.4/changelog-5.1.4-delta.xml::EDELIVERY-12977-tb_command_property::perpeio
ALTER TABLE TB_COMMAND_PROPERTY MODIFY PROPERTY_VALUE VARCHAR(2048);
//

--  Changeset src/main/resources/db/upgrade/5.1.4/changelog-5.1.4-delta.xml::EDELIVERY-12669::idragusa
ALTER TABLE WS_PLUGIN_TB_MESSAGE_LOG ADD CONSTRAINT UK_WS_MESSAGE_ID UNIQUE (MESSAGE_ID);
//

--  Changeset src/main/resources/db/upgrade/5.1.4/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.1.4', '2024-05-24 06:30', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='2024-05-24 06:30', CREATION_TIME = (UTC_TIMESTAMP);
//
