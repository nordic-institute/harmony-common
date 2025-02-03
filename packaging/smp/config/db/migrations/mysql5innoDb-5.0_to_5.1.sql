-- update existing tables with new columns
ALTER TABLE SMP_CREDENTIAL
    ADD RESET_EXPIRE_ON datetime comment 'Date time when reset token will expire',
    ADD RESET_TOKEN varchar(256)  CHARACTER SET utf8 COLLATE utf8_bin comment 'Reset token for credential reset';

ALTER TABLE SMP_CREDENTIAL_AUD
    ADD RESET_EXPIRE_ON datetime,
    ADD RESET_TOKEN varchar(256)  CHARACTER SET utf8 COLLATE utf8_bin;

ALTER TABLE SMP_DOCUMENT
    ADD REF_DOCUMENT_URL varchar(1024)  CHARACTER SET utf8 COLLATE utf8_bin,
    ADD SHARING_ENABLED bit,
    ADD FK_REF_DOCUMENT_ID bigint;

ALTER TABLE SMP_DOCUMENT_AUD
    ADD REF_DOCUMENT_URL varchar(1024)  CHARACTER SET utf8 COLLATE utf8_bin,
    ADD SHARING_ENABLED bit,
    ADD FK_REF_DOCUMENT_ID bigint;

ALTER TABLE SMP_DOCUMENT_VERSION
    ADD STATUS varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT 'RETIRED';

ALTER TABLE SMP_DOCUMENT_VERSION_AUD
    ADD STATUS varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin;

ALTER TABLE SMP_RESOURCE
    ADD REVIEW_ENABLED bit;
ALTER TABLE SMP_RESOURCE_AUD
    ADD REVIEW_ENABLED bit;

ALTER TABLE SMP_RESOURCE_MEMBER
    ADD PERMISSION_REVIEW bit comment 'User permission to review the resource document';
ALTER TABLE SMP_RESOURCE_MEMBER_AUD
    ADD PERMISSION_REVIEW bit;

-- Create new tables
create table SMP_DOCUMENT_PROPERTY (
   ID bigint not null auto_increment comment 'Unique document property id',
    CREATED_ON datetime not null,
    LAST_UPDATED_ON datetime not null,
    DESCRIPTION varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin comment 'Property description',
    PROPERTY_NAME varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin,
    PROPERTY_TYPE varchar(64)  CHARACTER SET utf8 COLLATE utf8_bin,
    PROPERTY_VALUE varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin,
    FK_DOCUMENT_ID bigint,
    primary key (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table SMP_DOCUMENT_PROPERTY_AUD (
   ID bigint not null,
    REV bigint not null,
    REVTYPE tinyint,
    CREATED_ON datetime,
    LAST_UPDATED_ON datetime,
    DESCRIPTION varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin,
    PROPERTY_NAME varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin,
    PROPERTY_TYPE varchar(64)  CHARACTER SET utf8 COLLATE utf8_bin,
    PROPERTY_VALUE varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin,
    FK_DOCUMENT_ID bigint,
    primary key (ID, REV)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table SMP_DOCUMENT_VERSION_EVENT (
   ID bigint not null auto_increment comment 'Unique document version event identifier',
    CREATED_ON datetime not null,
    LAST_UPDATED_ON datetime not null,
    DETAILS varchar(1024)  CHARACTER SET utf8 COLLATE utf8_bin comment 'Details of the event',
    EVENT_ON datetime comment 'Date time of the event',
    EVENT_SOURCE varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin not null comment 'Event source UI, API',
    EVENT_TYPE varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin not null comment 'Document version event type',
    EVENT_STATUS varchar(255)  CHARACTER SET utf8 COLLATE utf8_bin not null comment 'Document version event type',
    EVENT_BY_USERNAME varchar(64)  CHARACTER SET utf8 COLLATE utf8_bin comment 'username identifier of the user who triggered the event',
    FK_DOCUMENT_VERSION_ID bigint,
    primary key (ID)
) comment='Document version Events.' ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table SMP_DOMAIN_CONFIGURATION (
   ID bigint not null auto_increment comment 'Unique domain configuration id',
    CREATED_ON datetime not null,
    LAST_UPDATED_ON datetime not null,
    DESCRIPTION varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin comment 'Property description',
    PROPERTY_NAME varchar(512)  CHARACTER SET utf8 COLLATE utf8_bin not null comment 'Property name/key',
    SYSTEM_DEFAULT bit not null comment 'Use system default value',
    PROPERTY_VALUE varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin comment 'Property value',
    FK_DOMAIN_ID bigint not null,
    primary key (ID)
) comment='SMP domain configuration' ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table SMP_DOMAIN_CONFIGURATION_AUD (
   ID bigint not null,
    REV bigint not null,
    REVTYPE tinyint,
    CREATED_ON datetime,
    LAST_UPDATED_ON datetime,
    DESCRIPTION varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin,
    PROPERTY_NAME varchar(512)  CHARACTER SET utf8 COLLATE utf8_bin,
    SYSTEM_DEFAULT bit,
    PROPERTY_VALUE varchar(4000)  CHARACTER SET utf8 COLLATE utf8_bin,
    FK_DOMAIN_ID bigint,
    primary key (ID, REV)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- create constraints and indexes
alter table SMP_DOCUMENT_PROPERTY
       add constraint SMP_DOC_PROP_IDX unique (FK_DOCUMENT_ID, PROPERTY_NAME);

create index SMP_DOCVEREVNT_DOCVER_IDX on SMP_DOCUMENT_VERSION_EVENT (FK_DOCUMENT_VERSION_ID);

alter table SMP_DOMAIN_CONFIGURATION
       add constraint SMP_DOMAIN_CONF_IDX unique (ID, PROPERTY_NAME, FK_DOMAIN_ID);

alter table SMP_DOCUMENT
       add constraint FKbytp2kp8g3pj8qfp1g6a2g7p
       foreign key (FK_REF_DOCUMENT_ID)
       references SMP_DOCUMENT (ID);

alter table SMP_DOCUMENT_PROPERTY
   add constraint FKfag3795e9mrvfvesd00yis9yh
   foreign key (FK_DOCUMENT_ID)
   references SMP_DOCUMENT (ID);

alter table SMP_DOCUMENT_PROPERTY_AUD
       add constraint FK81057kcrugb1cfm0io5vkxtin
       foreign key (REV)
       references SMP_REV_INFO (id);

alter table SMP_DOCUMENT_VERSION_EVENT
        add constraint FK6es2svpoxyrnt1h05c9junmdn
        foreign key (FK_DOCUMENT_VERSION_ID)
        references SMP_DOCUMENT_VERSION (ID);

alter table SMP_DOMAIN_CONFIGURATION
   add constraint FK4303vstoigqtmeo3t2i034gm3
   foreign key (FK_DOMAIN_ID)
   references SMP_DOMAIN (ID);

alter table SMP_DOMAIN_CONFIGURATION_AUD
   add constraint FKkelcga805bleh5x256hy5e1xb
   foreign key (REV)
   references SMP_REV_INFO (id);

-- ----------------------------------------------
-- update SMP_DOCUMENT_VERSION STATUS  to RETIRED and current versions to PUBLISHED AND set it to  NOT NULL
UPDATE SMP_DOCUMENT_VERSION DV
    JOIN SMP_DOCUMENT DOC
       ON DOC.ID = DV.FK_DOCUMENT_ID AND DOC.CURRENT_VERSION = DV.VERSION
    SET STATUS = 'PUBLISHED';

commit;
