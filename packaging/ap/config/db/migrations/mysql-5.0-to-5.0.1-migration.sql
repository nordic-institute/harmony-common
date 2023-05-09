--  Changeset src/main/resources/db/archive/5.0.1/changelog-5.0.1-delta.xml::EDELIVERY-9534 fix java deletion slowness::Thomas Dussart
CREATE INDEX IDX_MSG_DOWNLOADED ON TB_USER_MESSAGE_LOG(DOWNLOADED);

CREATE INDEX IDX_UML_DELETION_COMPOSITE ON TB_USER_MESSAGE_LOG(DOWNLOADED, ID_PK, MESSAGE_STATUS_ID_FK);

CREATE INDEX IDX_UM_DELETION_COMPOSITE ON TB_USER_MESSAGE(MPC_ID_FK, ID_PK);

--  Changeset src/main/resources/db/archive/5.0.1/changelog-5.0.1-delta.xml::EDELIVERY-9732-Not possible to call service ext/messages/acknowledgments/delivered with properties specified::Soumya
ALTER TABLE TB_MESSAGE_ACKNW_PROP RENAME COLUMN FK_MSG_ACKNOWLEDGE TO MESSAGE_ACK_ID_FK;

ALTER TABLE TB_MESSAGE_ACKNW_PROP RENAME INDEX FK_MSG_ACK_PROP_MSG_ACK TO IDX_MSG_ACK_PROP_MSG_ACK;

--  Changeset src/main/resources/db/archive/5.0.1/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.0.1', '${DomibusBuildTime}', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='${DomibusBuildTime}', CREATION_TIME = (UTC_TIMESTAMP);

