# Generate private key
- openssl genrsa -out app.key 2048

# Generate public key
- openssl rsa -in ./app.key -pubout -out ./app.pub


# Dependencies
- wkhtmltopdf