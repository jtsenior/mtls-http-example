#!/usr/bin/env bash
# Generates a throwaway CA, server cert, and client cert for the mTLS demo.
# Produces PKCS12 keystores consumed directly by MtlsServer and MtlsHttpClient.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PASSWORD="changeit"
DAYS=825

rm -f ./*.key ./*.crt ./*.csr ./*.p12 ./*.srl

echo "==> Generating CA"
openssl req -x509 -newkey rsa:2048 -days "$DAYS" -nodes \
  -keyout ca.key -out ca.crt \
  -subj "/O=Mtls Example/CN=Mtls Example CA"

echo "==> Generating server key + cert (CN=localhost)"
openssl req -newkey rsa:2048 -nodes -keyout server.key -out server.csr \
  -subj "/O=Mtls Example/CN=localhost"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -days "$DAYS" -out server.crt \
  -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1")

echo "==> Generating client key + cert (CN=mtls-example-client)"
openssl req -newkey rsa:2048 -nodes -keyout client.key -out client.csr \
  -subj "/O=Mtls Example/CN=mtls-example-client"
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -days "$DAYS" -out client.crt

echo "==> Packaging server identity into server-keystore.p12"
openssl pkcs12 -export -inkey server.key -in server.crt \
  -name server -out server-keystore.p12 -password "pass:$PASSWORD"

echo "==> Packaging client identity into client-keystore.p12"
openssl pkcs12 -export -inkey client.key -in client.crt \
  -name client -out client-keystore.p12 -password "pass:$PASSWORD"

echo "==> Building truststore.p12 (trusts the CA only)"
keytool -importcert -noprompt -alias mtls-example-ca \
  -file ca.crt -keystore truststore.p12 \
  -storetype PKCS12 -storepass "$PASSWORD"

rm -f ./*.csr ./*.srl

echo "==> Done. Files written to $DIR:"
ls -1 ./*.p12 ./*.crt
echo
echo "All keystore/truststore passwords are: $PASSWORD"
