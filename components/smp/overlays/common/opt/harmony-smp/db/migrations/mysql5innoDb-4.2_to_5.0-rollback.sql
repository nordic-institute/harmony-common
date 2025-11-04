alter table SMP_ALERT_AUD drop foreign key FKrw0qnto448ojlirpfmfntd8v2;
alter table SMP_ALERT_PROPERTY drop foreign key FK15r37w3r5ty5f6074ykr2o4i6;
alter table SMP_ALERT_PROPERTY_AUD drop foreign key FKod33qjx87ih1a0skxl2sgddar;
alter table SMP_CERTIFICATE drop foreign key FK25b9apuupvmjp18wnn2b2gfg8;
alter table SMP_CERTIFICATE_AUD drop foreign key FKnrwm8en8vv10li8ihwnurwd9e;
alter table SMP_CONFIGURATION_AUD drop foreign key FKd4yhbdlusovfbdti1fjkuxp9m;
alter table SMP_CREDENTIAL drop foreign key FK89it2lyqvi2bl9bettx66n8n1;
alter table SMP_CREDENTIAL_AUD drop foreign key FKqjh6vxvb5tg0tvbkvi3k3xhe6;
alter table SMP_DOCUMENT_AUD drop foreign key FKh9epnme26i271eixtvrpqejvi;
alter table SMP_DOCUMENT_VERSION drop foreign key FKalsuoqx4csyp9mygvng911do;
alter table SMP_DOCUMENT_VERSION_AUD drop foreign key FK4glqiu73939kpyyb6bhw822k3;
alter table SMP_DOMAIN_AUD drop foreign key FK35qm8xmi74kfenugeonijodsg;
alter table SMP_DOMAIN_MEMBER drop foreign key FK1tdwy9oiyrk6tl4mk0fakhkf5;
alter table SMP_DOMAIN_MEMBER drop foreign key FKino2nvj74wc755nyn5mo260qi;
alter table SMP_DOMAIN_MEMBER_AUD drop foreign key FKijiv1avufqo9iu5u0cj4v3pv7;
alter table SMP_DOMAIN_RESOURCE_DEF drop foreign key FK563xw5tjw4rlr32va9g17cdsq;
alter table SMP_DOMAIN_RESOURCE_DEF drop foreign key FKtppp16v40ll2ch3ly8xusb8hi;
alter table SMP_DOMAIN_RESOURCE_DEF_AUD drop foreign key FKpujj9vb097i5w4loa3dxww2nj;
alter table SMP_EXTENSION_AUD drop foreign key FKke7f9wbwvp1bmnlqh9hrfm0r;
alter table SMP_GROUP drop foreign key FKjeomxyxjueaiyt7f0he0ls7vm;
alter table SMP_GROUP_AUD drop foreign key FKeik3quor2dxho7bmyoxc2ug9o;
alter table SMP_GROUP_MEMBER drop foreign key FK3y21chrphgx1dytux0p19btxe;
alter table SMP_GROUP_MEMBER drop foreign key FK8ue5gj1rx6gyiqp19dscp85ut;
alter table SMP_GROUP_MEMBER_AUD drop foreign key FK5pmorcyhwkaysh0a8xm99x6a8;
alter table SMP_RESOURCE drop foreign key FKkc5a6okrvq7dv87itfp7i1vmv;
alter table SMP_RESOURCE drop foreign key FK24mw8fiua39nh8rnobhgmujri;
alter table SMP_RESOURCE drop foreign key FKft55kasui36i77inf0wh8utv5;
alter table SMP_RESOURCE_AUD drop foreign key FKlbbfltxw6qmph5w3i8c9qf6kb;
alter table SMP_RESOURCE_DEF drop foreign key FKruu7v6uig9h333ihv34haw3ob;
alter table SMP_RESOURCE_DEF_AUD drop foreign key FKapswkgbdm9s4wwhx2cjduoniw;
alter table SMP_RESOURCE_MEMBER drop foreign key FKrci5jlgnckwo1mhq2rvmfaptw;
alter table SMP_RESOURCE_MEMBER drop foreign key FKs6jx68jxlx4xfdtxy20f3s6lu;
alter table SMP_RESOURCE_MEMBER_AUD drop foreign key FKknykp2wcby9fxk234yaaix1pe;
alter table SMP_SUBRESOURCE drop foreign key FK7y1ydnq350mbs3c8yrq2fhnsk;
alter table SMP_SUBRESOURCE drop foreign key FK7clbsapruvhkcqgekfxs8prex;
alter table SMP_SUBRESOURCE drop foreign key FKq3wmyy4ieoenuu1s55237qu9k;
alter table SMP_SUBRESOURCE_AUD drop foreign key FKffihyo233ldee8nejbkyclrov;
alter table SMP_SUBRESOURCE_DEF drop foreign key FKbjqilcym6p3pptva2s4d1gw8o;
alter table SMP_SUBRESOURCE_DEF_AUD drop foreign key FK1dd2l0ujtncg9u7hl3c4rte63;
alter table SMP_USER_AUD drop foreign key FK2786r5minnkai3d22b191iiiq;
-- drop new tables
drop table if exists SMP_ALERT;
drop table if exists SMP_ALERT_AUD;
drop table if exists SMP_ALERT_PROPERTY;
drop table if exists SMP_ALERT_PROPERTY_AUD;
drop table if exists SMP_CERTIFICATE;
drop table if exists SMP_CERTIFICATE_AUD;
drop table if exists SMP_CONFIGURATION;
drop table if exists SMP_CONFIGURATION_AUD;
drop table if exists SMP_CREDENTIAL;
drop table if exists SMP_CREDENTIAL_AUD;
drop table if exists SMP_DOCUMENT;
drop table if exists SMP_DOCUMENT_AUD;
drop table if exists SMP_DOCUMENT_VERSION;
drop table if exists SMP_DOCUMENT_VERSION_AUD;
drop table if exists SMP_DOMAIN;
drop table if exists SMP_DOMAIN_AUD;
drop table if exists SMP_DOMAIN_MEMBER;
drop table if exists SMP_DOMAIN_MEMBER_AUD;
drop table if exists SMP_DOMAIN_RESOURCE_DEF;
drop table if exists SMP_DOMAIN_RESOURCE_DEF_AUD;
drop table if exists SMP_EXTENSION;
drop table if exists SMP_EXTENSION_AUD;
drop table if exists SMP_GROUP;
drop table if exists SMP_GROUP_AUD;
drop table if exists SMP_GROUP_MEMBER;
drop table if exists SMP_GROUP_MEMBER_AUD;
drop table if exists SMP_RESOURCE;
drop table if exists SMP_RESOURCE_AUD;
drop table if exists SMP_RESOURCE_DEF;
drop table if exists SMP_RESOURCE_DEF_AUD;
drop table if exists SMP_RESOURCE_MEMBER;
drop table if exists SMP_RESOURCE_MEMBER_AUD;
drop table if exists SMP_REV_INFO;
drop table if exists SMP_SUBRESOURCE;
drop table if exists SMP_SUBRESOURCE_AUD;
drop table if exists SMP_SUBRESOURCE_DEF;
drop table if exists SMP_SUBRESOURCE_DEF_AUD;
drop table if exists SMP_USER;
drop table if exists SMP_USER_AUD;
-- rename backup tables
RENAME TABLE BCK_ALERT TO SMP_ALERT;
RENAME TABLE BCK_ALERT_AUD TO SMP_ALERT_AUD;
RENAME TABLE BCK_ALERT_PROPERTY TO SMP_ALERT_PROPERTY;
RENAME TABLE BCK_ALERT_PROPERTY_AUD TO SMP_ALERT_PROPERTY_AUD;
RENAME TABLE BCK_CERTIFICATE TO SMP_CERTIFICATE;
RENAME TABLE BCK_CERTIFICATE_AUD TO SMP_CERTIFICATE_AUD;
RENAME TABLE BCK_CONFIGURATION TO SMP_CONFIGURATION;
RENAME TABLE BCK_CONFIGURATION_AUD TO SMP_CONFIGURATION_AUD;
RENAME TABLE BCK_DOMAIN TO SMP_DOMAIN;
RENAME TABLE BCK_DOMAIN_AUD TO SMP_DOMAIN_AUD;
RENAME TABLE BCK_OWNERSHIP TO SMP_OWNERSHIP;
RENAME TABLE BCK_OWNERSHIP_AUD TO SMP_OWNERSHIP_AUD;
RENAME TABLE BCK_REV_INFO TO SMP_REV_INFO;
RENAME TABLE BCK_SERVICE_GROUP TO SMP_SERVICE_GROUP;
RENAME TABLE BCK_SERVICE_GROUP_AUD TO SMP_SERVICE_GROUP_AUD;
RENAME TABLE BCK_SERVICE_GROUP_DOMAIN TO SMP_SERVICE_GROUP_DOMAIN;
RENAME TABLE BCK_SERVICE_GROUP_DOMAIN_AUD TO SMP_SERVICE_GROUP_DOMAIN_AUD;
RENAME TABLE BCK_SERVICE_METADATA TO SMP_SERVICE_METADATA;
RENAME TABLE BCK_SERVICE_METADATA_AUD TO SMP_SERVICE_METADATA_AUD;
RENAME TABLE BCK_SERVICE_METADATA_XML TO SMP_SERVICE_METADATA_XML;
RENAME TABLE BCK_SERVICE_METADATA_XML_AUD TO SMP_SERVICE_METADATA_XML_AUD;
RENAME TABLE BCK_SG_EXTENSION TO SMP_SG_EXTENSION;
RENAME TABLE BCK_SG_EXTENSION_AUD TO SMP_SG_EXTENSION_AUD;
RENAME TABLE BCK_USER TO SMP_USER;
RENAME TABLE BCK_USER_AUD TO SMP_USER_AUD;
-- setup indexes and constraints
alter table SMP_CERTIFICATE
   add constraint UK_3x3rvf6hkim9fg16caurkgg6f unique (CERTIFICATE_ID);

alter table SMP_DOMAIN
   add constraint UK_djrwqd4luj5i7w4l7fueuaqbj unique (DOMAIN_CODE);

alter table SMP_DOMAIN
   add constraint UK_likb3jn0nlxlekaws0xx10uqc unique (SML_SUBDOMAIN);
create index SMP_SG_PART_ID_IDX on SMP_SERVICE_GROUP (PARTICIPANT_IDENTIFIER);
create index SMP_SG_PART_SCH_IDX on SMP_SERVICE_GROUP (PARTICIPANT_SCHEME);

alter table SMP_SERVICE_GROUP
   add constraint SMP_SG_UNIQ_PARTC_IDX unique (PARTICIPANT_SCHEME, PARTICIPANT_IDENTIFIER);
create index SMP_SMD_DOC_ID_IDX on SMP_SERVICE_METADATA (DOCUMENT_IDENTIFIER);
create index SMP_SMD_DOC_SCH_IDX on SMP_SERVICE_METADATA (DOCUMENT_SCHEME);

alter table SMP_SERVICE_METADATA
   add constraint SMP_MT_UNIQ_SG_DOC_IDX unique (FK_SG_DOM_ID, DOCUMENT_IDENTIFIER, DOCUMENT_SCHEME);

alter table SMP_USER
   add constraint UK_tk9bjsmd2mevgt3b997i6pl27 unique (ACCESS_TOKEN_ID);

alter table SMP_USER
   add constraint UK_rt1f0anklfo05lt0my05fqq6 unique (USERNAME);

alter table SMP_ALERT_AUD
   add constraint FKrw0qnto448ojlirpfmfntd8v2
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_ALERT_PROPERTY
   add constraint FK15r37w3r5ty5f6074ykr2o4i6
   foreign key (FK_ALERT_ID)
   references SMP_ALERT (ID);

alter table SMP_ALERT_PROPERTY_AUD
   add constraint FKod33qjx87ih1a0skxl2sgddar
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_CERTIFICATE
   add constraint FKayqgpj5ot3o8vrpduul7sstta
   foreign key (ID)
   references SMP_USER (ID);

alter table SMP_CERTIFICATE_AUD
   add constraint FKnrwm8en8vv10li8ihwnurwd9e
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_CONFIGURATION_AUD
   add constraint FKd4yhbdlusovfbdti1fjkuxp9m
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_DOMAIN_AUD
   add constraint FK35qm8xmi74kfenugeonijodsg
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_OWNERSHIP
   add constraint FKrnqwq06lbfwciup4rj8nvjpmy
   foreign key (FK_USER_ID)
   references SMP_USER (ID);

alter table SMP_OWNERSHIP
   add constraint FKgexq5n6ftsid8ehqljvjh8p4i
   foreign key (FK_SG_ID)
   references SMP_SERVICE_GROUP (ID);

alter table SMP_OWNERSHIP_AUD
   add constraint FK1lqynlbk8ow1ouxetf5wybk3k
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_SERVICE_GROUP_AUD
   add constraint FKj3caimhegwyav1scpwrxoslef
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_SERVICE_GROUP_DOMAIN
   add constraint FKo186xtefda6avl5p1tuqchp3n
   foreign key (FK_DOMAIN_ID)
   references SMP_DOMAIN (ID);

alter table SMP_SERVICE_GROUP_DOMAIN
   add constraint FKgcvhnk2n34d3c6jhni5l3s3x3
   foreign key (FK_SG_ID)
   references SMP_SERVICE_GROUP (ID);

alter table SMP_SERVICE_GROUP_DOMAIN_AUD
   add constraint FK6uc9r0eqw16baooxtmqjkih0j
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_SERVICE_METADATA
   add constraint FKfvcml6b8x7kn80m30h8pxs7jl
   foreign key (FK_SG_DOM_ID)
   references SMP_SERVICE_GROUP_DOMAIN (ID);

alter table SMP_SERVICE_METADATA_AUD
   add constraint FKbqr9pdnik1qxx2hi0xn4n7f61
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_SERVICE_METADATA_XML
   add constraint FK4b1x06xlavcgbjnuilgksi7nm
   foreign key (ID)
   references SMP_SERVICE_METADATA (ID);

alter table SMP_SERVICE_METADATA_XML_AUD
   add constraint FKevatmlvvwoxfnjxkvmokkencb
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_SG_EXTENSION
   add constraint FKtf0mfonugp2jbkqo2o142chib
   foreign key (ID)
   references SMP_SERVICE_GROUP (ID);

alter table SMP_SG_EXTENSION_AUD
   add constraint FKmdo9v2422adwyebvl34qa3ap6
   foreign key (REV)
   references SMP_REV_INFO (id);

alter table SMP_USER_AUD
   add constraint FK2786r5minnkai3d22b191iiiq
   foreign key (REV)
   references SMP_REV_INFO (id);
