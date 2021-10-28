DELETE FROM bdmsl_configuration;

insert into bdmsl_configuration(property, value, description, created_on, last_updated_on) values
('useProxy','false','true if a proxy is required to connect to the internet. Possible values: true/false', NOW(), NOW()),
('unsecureLoginAllowed','true','true if the use of HTTPS is not required. If the value is set to true, then the user unsecure-http-client is automatically created. Possible values: true/false', NOW(), NOW()),
('signResponse','false','true if the responses must be signed. Possible values: true/false', NOW(), NOW()),
('paginationListRequest','100','Number of participants per page for the list operation of ManageParticipantIdentifier service. This property is used for pagination purposes.', NOW(), NOW()),
('keystorePassword','vXA7JjCy0iDQmX1UEN1Qwg==','Base64 encrypted password for Keystore.', NOW(), NOW()),
('keystoreFileName','keystore.jks','The JKS keystore file. Should be just the filename if the file is in the classpath or in the configurationDir', NOW(), NOW()),
('keystoreAlias','sendercn','The alias in the keystore.', NOW(), NOW()),
('httpProxyUser','user','The proxy user', NOW(), NOW()),
('httpProxyPort','80','The http proxy port', NOW(), NOW()),
('httpProxyPassword','setencPasswd','Base64 encrypted password for Proxy.', NOW(), NOW()),
('httpProxyHost','127.0.0.1','The http proxy host', NOW(), NOW()),
('encriptionPrivateKey','encriptionPrivateKey.private','Name of the 256 bit AES secret key to encrypt or decrypt passwords.', NOW(), NOW()),
('dnsClient.server','127.0.0.1','The DNS server', NOW(), NOW()),
('dnsClient.publisherPrefix','publisher','This is the prefix for the publishers (SMP). This is to be concatenated with the associated DNS domain in the table bdmsl_certificate_domain', NOW(), NOW()),
('dnsClient.enabled','true','true if registration of DNS records is required. Must be true in production. Possible values: true/false', NOW(), NOW()),
('dnsClient.show.entries','true','if true than service ListDNS transfer and show the DNS entries. (Not recommended for large zones)  Possible VALUES: true/false', NOW(), NOW()),
('dnsClient.SIG0PublicKeyName','sig0.acc.edelivery.tech.ec.europa.eu.','The public key name of the SIG0 key', NOW(), NOW()),
('dnsClient.SIG0KeyFileName','SIG0.private','The actual SIG0 key file. Should be just the filename if the file is in the classpath or in the configurationDir', NOW(), NOW()),
('dnsClient.SIG0Enabled','false','true if the SIG0 signing is enabled. Required fr DNSSEC. Possible values: true/false', NOW(), NOW()),
('dataInconsistencyAnalyzer.senderEmail','automated-notifications@nomail.ec.europa.eu','Sender email address for reporting Data Inconsistency Analyzer.', NOW(), NOW()),
('dataInconsistencyAnalyzer.recipientEmail','email@domain.com','Email address to receive Data Inconsistency Checker results', NOW(), NOW()),
('dataInconsistencyAnalyzer.cronJobExpression','0 0 3 ? * *','Cron expression for dataInconsistencyChecker job. Example: 0 0 3 ? * * (everyday at 3:00 am)', NOW(), NOW()),
('configurationDir','/opt/smlconf/','The absolute path to the folder containing all the configuration files (keystore and sig0 key)', NOW(), NOW()),
('certificateChangeCronExpression','0 0 2 ? * *','Cron expression for the changeCertificate job. Example: 0 0 2 ? * * (everyday at 2:00 am)', NOW(), NOW()),
('authorization.smp.certSubjectRegex','^.*$','User with ROOT-CA is granted SMP_ROLE only if its certificates Subject matches configured regexp', NOW(), NOW()),
('authentication.bluecoat.enabled','true','Enables reverse proxy authentication.', NOW(), NOW()),
('authorization.domain.legacy.enabled','true','Enables reverse proxy authentication.', NOW(), NOW()),
('adminPassword','$2a$10$9RzbkquhBYRkHUoKMTNZhOPJmevTbUKWf549MEiCWUd.1LdblMhBi','BCrypt Hashed password to access admin services', NOW(), NOW()),
('mail.smtp.host','smtp.localhost','BCrypt Hashed password to access admin services', NOW(), NOW()),
('mail.smtp.port','25','BCrypt Hashed password to access admin services', NOW(), NOW()),
('sml.property.refresh.cronJobExpression','5 */1 * * * *','Properies update', NOW(), NOW());

DELETE FROM bdmsl_certificate_domain;

INSERT INTO bdmsl_subdomain(subdomain_id, subdomain_name,dns_zone, description, participant_id_regexp, dns_record_types, smp_url_schemas, created_on, last_updated_on) values
(1, 'niis.neds','neds','Domain for smpa.neds','^.*$','all','all', NOW(), NOW())
  ON DUPLICATE KEY UPDATE subdomain_name='niis.neds', dns_zone = 'neds', participant_id_regexp='^.*$', dns_record_types='all', smp_url_schemas='all', last_updated_on=NOW();








