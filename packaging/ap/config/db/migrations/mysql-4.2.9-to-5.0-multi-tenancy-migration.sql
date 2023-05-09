--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-7836::gautifr
--  create DOMIBUS_SCALABLE_SEQUENCE sequence
CREATE TABLE DOMIBUS_SCALABLE_SEQUENCE (sequence_name VARCHAR(255) NOT NULL, next_val BIGINT NULL, CONSTRAINT PK_DOMIBUS_SCALABLE_SEQUENCE PRIMARY KEY (sequence_name));

--  Changeset src/main/resources/db/archive/5.0/migration/common/changelog-5.0-alter-delta.xml::EDELIVERY-7840-Rename-unique-key-constraints-common::Soumya
ALTER TABLE TB_COMMAND_PROPERTY RENAME INDEX UNI_COMMAND_PROP_NAME TO UK_COMMAND_PROP_NAME;

ALTER TABLE TB_USER RENAME INDEX USER_NAME TO UK_USER_NAME;

ALTER TABLE TB_USER_ROLE RENAME INDEX UQ_ROLE_NAME TO UK_ROLE_NAME;

--  Changeset src/main/resources/db/archive/5.0/migration/common/changelog-5.0-alter-delta.xml::EDELIVERY-8384-primary-key-index-names-common::Sebastian-Ion TINCU
--  Changeset src/main/resources/db/archive/5.0/migration/multitenancy/changelog-5.0-alter-delta.xml::EDELIVERY-8384-primary-key-index-names-multitenancy::Sebastian-Ion TINCU
--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-7668::Catalin Enache
CREATE TABLE TB_VERSION (VERSION VARCHAR(30) NULL, BUILD_TIME VARCHAR(30) NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL) COMMENT='Stores Domibus version and build time';

ALTER TABLE TB_VERSION ADD CONSTRAINT UK_VERSION UNIQUE (VERSION);

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-8503_3::nperpion
CREATE TABLE TB_LOCK (ID_PK BIGINT AUTO_INCREMENT NOT NULL, LOCK_KEY VARCHAR(255) NOT NULL, CREATION_TIME timestamp DEFAULT (UTC_TIMESTAMP) NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_LOCK PRIMARY KEY (ID_PK)) COMMENT='Stores keys used for locking/synchronizing in cluster';

ALTER TABLE TB_LOCK ADD CONSTRAINT UK_LOCK_KEY UNIQUE (LOCK_KEY);

INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000001', 'bootstrap-synchronization.lock');

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-9451::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000002', 'scheduler-synchronization.lock');

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-9028-Audit Table for TB_USER_DOMAIN::Ion Perpegel
CREATE TABLE TB_USER_DOMAIN_AUD (ID_PK BIGINT NOT NULL, REV BIGINT NOT NULL, REVTYPE TINYINT NULL, USER_NAME VARCHAR(255) NULL, USERNAME_MOD BIT(1) NULL, DOMAIN VARCHAR(255) NULL, DOMAIN_MOD BIT(1) NULL, PREFERRED_DOMAIN VARCHAR(255) NULL, PREFERREDDOMAIN_MOD BIT(1) NULL, CONSTRAINT PK_USER_DOMAIN_AUD PRIMARY KEY (ID_PK, REV));

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-8688-General Schema Audit::Ion Perpegel
CREATE OR REPLACE VIEW V_AUDIT_DETAIL AS SELECT
            DISTINCT rc.GROUP_NAME as AUDIT_TYPE ,
            rc.MODIFICATION_TYPE as ACTION_TYPE,
            ri.USER_NAME as USER_NAME ,
            ri.REVISION_DATE as AUDIT_DATE,
            COALESCE(TRIM(CAST(rc.ENTITY_ID AS CHAR(255))), '') AS ID,
            COALESCE(TRIM(CAST(ri.ID AS CHAR(19))), '') AS REV_ID
            FROM TB_REV_INFO ri, TB_REV_CHANGES rc
            WHERE ri.ID=rc.REV;

CREATE OR REPLACE VIEW V_AUDIT AS SELECT *
            FROM V_AUDIT_DETAIL VAD
            ORDER BY VAD.AUDIT_DATE DESC;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::1564496480476-30-new-indices::Catalin Enache
--  create indexes
CREATE INDEX IDX_FK_REV_CHANGES_REV_INFO ON TB_REV_CHANGES(REV);

CREATE INDEX IDX_FK_USR_ROL_AUD_REV_INFO ON TB_USER_ROLE_AUD(REV);

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::1564496480476-56-new-foreign-keys::Catalin Enache
--  create foreign keys
ALTER TABLE TB_REV_CHANGES ADD CONSTRAINT FK_REV_CHANGES_REV_INFO FOREIGN KEY (REV) REFERENCES TB_REV_INFO (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_ROLE_AUD ADD CONSTRAINT FK_USR_ROL_AUD_REV_INFO FOREIGN KEY (REV) REFERENCES TB_REV_INFO (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

--  Changeset src/main/resources/db/archive/5.0/changelog-5.0-multi-tenancy-delta.xml::EDELIVERY-8895-multitenancy-performance::Sebastian-Ion TINCU
ALTER TABLE TB_REV_CHANGES CHANGE ENTIY_NAME ENTITY_NAME VARCHAR(255);

--  Changeset src/main/resources/db/archive/5.0/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.0', '${DomibusBuildTime}', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='${DomibusBuildTime}', CREATION_TIME = (UTC_TIMESTAMP);

