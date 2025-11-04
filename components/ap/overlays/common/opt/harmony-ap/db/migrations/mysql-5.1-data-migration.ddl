--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/changelog-5.1-data-migration.xml
--  Ran at: 5/15/23 12:13 PM
--  Against: null@offline:mysql?changeLogFile=/home/hyoty/projects/niis/harmony-access-point/Core/Domibus-MSH-db/target/liquibase/changelog-2.0.0-SNAPSHOT-data-migration.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/changelog-5.1-data-migration.xml::EDELIVERY-10284::Razvan Cretu
--  update table WS_PLUGIN_TB_BACKEND_MSG_LOG column MESSAGE_ENTITY_ID with TB_USER_MESSAGE.ID_PK
UPDATE WS_PLUGIN_TB_BACKEND_MSG_LOG LG
            JOIN TB_USER_MESSAGE MSG ON LG.MESSAGE_ID=MSG.MESSAGE_ID
            JOIN TB_D_MSH_ROLE R ON MSG.MSH_ROLE_ID_FK=R.ID_PK AND R.ROLE='RECEIVING'
            SET LG.MESSAGE_ENTITY_ID=MSG.ID_PK;

--  Changeset src/main/resources/db/changelog-5.1-data-migration.xml::EDELIVERY-10284-make-notnull::Razvan Cretu
UPDATE WS_PLUGIN_TB_BACKEND_MSG_LOG SET MESSAGE_ENTITY_ID = '19700101' WHERE MESSAGE_ENTITY_ID IS NULL;

ALTER TABLE WS_PLUGIN_TB_BACKEND_MSG_LOG MODIFY MESSAGE_ENTITY_ID BIGINT NOT NULL;

