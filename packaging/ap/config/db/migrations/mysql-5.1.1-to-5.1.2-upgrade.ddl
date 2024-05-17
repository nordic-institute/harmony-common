--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/upgrade/5.1.2/changelog-5.1.2-delta.xml
--  Ran at: 1/12/24, 6:08 PM
--  Against: null@offline:mysql?changeLogFile=/Users/dragusa/domibus_release_512/sql/domibus-sql/domibus-msh-sql-scripts/target/liquibase/changelog-5.1.2-delta.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/upgrade/5.1.2/../../common/changelog-before-migration-statements-v2.xml::EDELIVERY-12287_assert_previous_migration_succeeded-v2-mysql::Gabriel Maier
-- DELIMITER //

DROP PROCEDURE IF EXISTS ASSERT_DB_VERSION_IS
//

DROP FUNCTION IF EXISTS STRING_TO_JSON_ARRAY
//

--  Changeset src/main/resources/db/upgrade/5.1.2/changelog-5.1.2-delta.xml::EDELIVERY-11796-add-pks-mysql1::Gabriel Maier
ALTER TABLE TB_USER_ROLES_AUD ADD ID_PK BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY
//

--  Changeset src/main/resources/db/upgrade/5.1.2/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.1.2', '2024-01-12 16:08', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='2024-01-12 16:08', CREATION_TIME = (UTC_TIMESTAMP)
//
