#!/usr/bin/env bash
set -euo pipefail

PASSWORD="changeme"
CERTS_DIR="./certs"
ENV_FILE="./.env"

ALIAS="harmony-ap"
KEY_FILE="${CERTS_DIR}/${ALIAS}-tls.key"
CRT_FILE="${CERTS_DIR}/${ALIAS}-tls.crt"
KEYSTORE_FILE="${CERTS_DIR}/${ALIAS}-tls-keystore.p12"

mkdir -p "$CERTS_DIR"

# Generates private key
openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:3072 \
  -out "$KEY_FILE"

# Generates self-signed certificate
openssl req \
  -new -x509 \
  -key "$KEY_FILE" \
  -days 3650 \
  -subj "/CN=${ALIAS}" \
  -out "$CRT_FILE"

# Generates PKCS12 keystore with the private key and certificate
openssl pkcs12 -export \
  -inkey "$KEY_FILE" \
  -in    "$CRT_FILE" \
  -name  "$ALIAS" \
  -passout pass:"${PASSWORD}" \
  -out   "$KEYSTORE_FILE"

cat > "$ENV_FILE" <<EOF
TLS_KEYSTORE_B64=$(openssl base64 -in "${KEYSTORE_FILE}" -A)
EOF

echo "Done"