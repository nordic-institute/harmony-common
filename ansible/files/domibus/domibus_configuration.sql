INSERT IGNORE INTO TB_USER (ID_PK, USER_NAME, USER_PASSWORD, USER_ENABLED, USER_DELETED, DEFAULT_PASSWORD) VALUES ('1', 'admin', '$2a$10$5uKS72xK2ArGDgb2CwjYnOzQcOmB7CPxK6fz2MGcDBM9vJ4rUql36', 1, 0, 0);

INSERT IGNORE INTO TB_USER (ID_PK, USER_NAME, USER_PASSWORD, USER_ENABLED, USER_DELETED, DEFAULT_PASSWORD) VALUES ('2', 'user', '$2a$10$HApapHvDStTEwjjneMCvxuqUKVyycXZRfXMwjU0rRmaWMsjWQp/Zu', 1, 0, 0);

INSERT IGNORE INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('1', '1');

INSERT IGNORE INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('2', '2');

INSERT IGNORE INTO TB_AUTHENTICATION_ENTRY (USERNAME, PASSWD, AUTH_ROLES, DEFAULT_PASSWORD, PASSWORD_CHANGE_DATE) VALUES ('admin', '$2a$10$5uKS72xK2ArGDgb2CwjYnOzQcOmB7CPxK6fz2MGcDBM9vJ4rUql36', 'ROLE_ADMIN', 0, NOW());

INSERT IGNORE INTO TB_AUTHENTICATION_ENTRY (USERNAME, PASSWD, AUTH_ROLES, ORIGINAL_USER, DEFAULT_PASSWORD, PASSWORD_CHANGE_DATE) VALUES ('user', '$2a$10$HApapHvDStTEwjjneMCvxuqUKVyycXZRfXMwjU0rRmaWMsjWQp/Zu', 'ROLE_USER', 'urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1', 0, NOW());

INSERT IGNORE INTO TB_AUTHENTICATION_ENTRY (CERTIFICATE_ID, AUTH_ROLES) VALUES ('CN=blue_gw,O=eDelivery,C=BE:10370035830817850458', 'ROLE_ADMIN');
