Mock certificate authority for NEDS environments.

Configuration is based on https://roll.urown.net/ca/ca_root_setup.html.

CA private key in neds-ca.key.pem and all generated keys are not password protected.

Generate new private key with generate_cert script with FQDN as parameter. NEDS AWS environment uses "neds" as top level
domain. Local LXC environment uses "lxd" as top level domain.

Example:

    bash generate_cert.sh cef_ap.neds

will generate cef_ap.neds.key.pem and cef_ap.neds.crt.pem into host_certc folder.