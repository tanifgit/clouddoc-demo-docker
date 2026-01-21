#!/bin/sh
set -e

echo "=== CloudDoc Docker demo entrypoint ==="

CERT_PATH="/app/cert.pem"
KEYSTORE_PATH="/app/keystore.jks"
KEYSTORE_PASSWORD="changeit"
ALIAS_NAME="iris-clouddoc"

if [ ! -f "$CERT_PATH" ]; then
  echo "ERROR: TLS certificate not found at $CERT_PATH"
  echo "Mount your Cloud Document certificate into the container, for example:"
  echo "  -v /path/to/cert-file.pem:/app/cert.pem:ro"
  exit 1
fi

# Always start from a clean keystore to avoid alias-exists errors
if [ -f "$KEYSTORE_PATH" ]; then
  echo "Removing existing keystore at $KEYSTORE_PATH"
  rm -f "$KEYSTORE_PATH"
fi

echo "Importing certificate into keystore..."
keytool -importcert -noprompt -alias "$ALIAS_NAME" -file "$CERT_PATH" -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD"

echo "Keystore created at $KEYSTORE_PATH"

echo "Starting Java demo..."
exec java -jar /app/clouddoc-demo-app.jar
