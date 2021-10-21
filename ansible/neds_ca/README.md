Mock certificate authority for NEDS environments.

Configuration is based on https://roll.urown.net/ca/ca_root_setup.html.

CA private key in neds-ca.key.pem and all generated keys are not password protected.

Generate new private keys and certificates in PEM format with generate_cert script certificate Common Name as parameter.
Two pairs of files are generated - one for edelivery keystore and one for tls keystore.

Ansible scripts use this internally - it is not necessary to generate certificates beforehand. 

Example:

    bash generate_cert.sh cef-ap.lxd

will generate cef-ap.lxd.key.pem, cef-ap.lxd.crt.pem, cef-ap.lxd_tls.key.pem and cef-ap.lxd_tls.crt.pem
into host_certs folder.

File neds-ca.cert pem contains moc CA certificate. The same certificate is in file neds-ca-trusted.jks. 