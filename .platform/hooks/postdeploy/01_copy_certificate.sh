#!/bin/bash
set -e

# Create the resources directory if it doesn't exist
mkdir -p /var/app/current/src/main/resources

# Download the certificate
curl -o /var/app/current/src/main/resources/AmazonRootCA1.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

# Set proper permissions
chmod 644 /var/app/current/src/main/resources/AmazonRootCA1.pem

# Create a marker file to indicate the certificate is ready
touch /var/app/current/src/main/resources/.certificate_ready

echo "Certificate downloaded and copied to /var/app/current/src/main/resources/" 