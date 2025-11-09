#!/bin/bash

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Hardcoded values - Update these for your organization
DOMAIN="www.pps.go.ug"
COMPANY="Parliamentary Pension Scheme"
COUNTRY="UG"
STATE="Kampala"
CITY="Kampala"
DEPARTMENT="IT Department"
OUTPUT_DIR="/home/$(whoami)/stanchart_bank_certs"

# Generate filenames based on domain - Using PEM format for all files
PRIVATE_KEY_FILE="$OUTPUT_DIR/${DOMAIN}_private.pem"
CSR_FILE="$OUTPUT_DIR/${DOMAIN}.csr"
PUBLIC_KEY_FILE="$OUTPUT_DIR/${DOMAIN}_public.pem"
CERT_FILE="$OUTPUT_DIR/${DOMAIN}_cert.pem"
P12_FILE="$OUTPUT_DIR/${DOMAIN}.p12"

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Check if files already exist and prompt for regeneration
handle_existing_files() {
    local files_exist=false

    if [[ -f "$PRIVATE_KEY_FILE" ]] || [[ -f "$CSR_FILE" ]] || [[ -f "$PUBLIC_KEY_FILE" ]]; then
        print_warning "Existing certificate files found:"
        [[ -f "$PRIVATE_KEY_FILE" ]] && echo "  - $PRIVATE_KEY_FILE"
        [[ -f "$CSR_FILE" ]] && echo "  - $CSR_FILE"
        [[ -f "$PUBLIC_KEY_FILE" ]] && echo "  - $PUBLIC_KEY_FILE"
        echo

        read -p "Do you want to REGENERATE and OVERWRITE all files? (yes/no): " regenerate
        case "$regenerate" in
            [yY]|[yY][eE][sS])
                print_status "Backing up existing files..."
                local backup_dir="$OUTPUT_DIR/backup_$(date +%Y%m%d_%H%M%S)"
                mkdir -p "$backup_dir"
                [[ -f "$PRIVATE_KEY_FILE" ]] && cp "$PRIVATE_KEY_FILE" "$backup_dir/"
                [[ -f "$CSR_FILE" ]] && cp "$CSR_FILE" "$backup_dir/"
                [[ -f "$PUBLIC_KEY_FILE" ]] && cp "$PUBLIC_KEY_FILE" "$backup_dir/"
                [[ -f "$P12_FILE" ]] && cp "$P12_FILE" "$backup_dir/" 2>/dev/null || true
                [[ -f "$CERT_FILE" ]] && cp "$CERT_FILE" "$backup_dir/" 2>/dev/null || true
                print_status "Backup created in: $backup_dir"

                print_status "Removing existing files..."
                rm -f "$PRIVATE_KEY_FILE" "$CSR_FILE" "$PUBLIC_KEY_FILE" "$P12_FILE" "$CERT_FILE"
                echo
                ;;
            *)
                print_status "Using existing files. Exiting."
                exit 0
                ;;
        esac
    fi
}

# Main certificate generation function
generate_certificates() {
    print_status "Generating RSA private key (2048-bit) in PEM format..."
    openssl genrsa -out "$PRIVATE_KEY_FILE" 2048

    print_status "Generating Certificate Signing Request (PEM format)..."
    openssl req -new -key "$PRIVATE_KEY_FILE" -out "$CSR_FILE" \
      -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$COMPANY/OU=$DEPARTMENT/CN=$DOMAIN"

    print_status "Extracting public key in PEM format..."
    openssl rsa -in "$PRIVATE_KEY_FILE" -pubout -out "$PUBLIC_KEY_FILE"

    # Also create a copy of private key with .key extension for compatibility
    cp "$PRIVATE_KEY_FILE" "$OUTPUT_DIR/${DOMAIN}.key"

    # Set secure permissions
    chmod 600 "$PRIVATE_KEY_FILE" "$OUTPUT_DIR/${DOMAIN}.key"
    chmod 644 "$CSR_FILE" "$PUBLIC_KEY_FILE"
}

# Verification function
verify_certificates() {
    print_status "Verifying generated files..."

    # Verify private key
    if openssl rsa -in "$PRIVATE_KEY_FILE" -check -noout > /dev/null 2>&1; then
        print_status "Private key is valid"
    else
        print_error "Private key verification failed!"
        exit 1
    fi

    # Verify CSR
    if openssl req -in "$CSR_FILE" -noout -subject > /dev/null 2>&1; then
        print_status "CSR is valid"
    else
        print_error "CSR verification failed!"
        exit 1
    fi

    # Verify public key
    if openssl rsa -pubin -in "$PUBLIC_KEY_FILE" -noout > /dev/null 2>&1; then
        print_status "Public key is valid"
    else
        print_error "Public key verification failed!"
        exit 1
    fi
}

# Display generated files info
display_file_info() {
    echo
    echo "=== FILES GENERATED SUCCESSFULLY ==="
    echo "Private Key (PEM): $PRIVATE_KEY_FILE"
    echo "Private Key (.key): $OUTPUT_DIR/${DOMAIN}.key"
    echo "CSR: $CSR_FILE"
    echo "Public Key (PEM): $PUBLIC_KEY_FILE"
    echo

    print_status "CSR Subject Details:"
    openssl req -in "$CSR_FILE" -noout -subject

    echo
    print_status "File formats (checking headers):"
    echo -n "Private Key: " && head -1 "$PRIVATE_KEY_FILE"
    echo -n "CSR: " && head -1 "$CSR_FILE"
    echo -n "Public Key: " && head -1 "$PUBLIC_KEY_FILE"

    echo
    print_warning "IMPORTANT: Standard Chartered requires PEM format files"
    echo
    print_warning "UPLOAD TO BANK:"
    echo "1. Upload this CSR file to Standard Chartered: $CSR_FILE"
    echo "2. They will provide you with a signed certificate"
    echo "3. Save their certificate as: $CERT_FILE"
    echo
    print_warning "SECURITY NOTES:"
    echo "1. Keep $PRIVATE_KEY_FILE secure and never share it!"
    echo "2. The .key file is a copy of the private key in traditional format"
    echo
}

# Function to handle bank certificate
handle_bank_certificate() {
    local bank_cert_path="$1"

    if [[ ! -f "$bank_cert_path" ]]; then
        print_error "Bank certificate not found: $bank_cert_path"
        exit 1
    fi

    print_status "Processing bank certificate..."

    # Check if it's in PEM format
    if head -1 "$bank_cert_path" | grep -q "BEGIN CERTIFICATE"; then
        print_status "Certificate is already in PEM format"
        cp "$bank_cert_path" "$CERT_FILE"
    else
        print_status "Converting certificate to PEM format..."
        openssl x509 -in "$bank_cert_path" -inform DER -out "$CERT_FILE" -outform PEM
    fi

    print_status "Bank certificate saved as: $CERT_FILE"

    # Verify the certificate matches our private key
    print_status "Verifying certificate and private key match..."
    local cert_modulus=$(openssl x509 -in "$CERT_FILE" -noout -modulus | openssl md5)
    local key_modulus=$(openssl rsa -in "$PRIVATE_KEY_FILE" -noout -modulus | openssl md5)

    if [[ "$cert_modulus" == "$key_modulus" ]]; then
        print_status "✓ Certificate matches private key"
    else
        print_error "✗ Certificate does not match private key!"
        print_error "The certificate provided by the bank doesn't match your private key."
        exit 1
    fi
}

# Function to create PKCS12 after receiving bank certificate
create_pkcs12() {
    local signed_cert="$1"
    local ca_bundle="$2"
    local p12_password="$3"

    # Process bank certificate first
    handle_bank_certificate "$signed_cert"

    # Check if private key exists
    if [[ ! -f "$PRIVATE_KEY_FILE" ]]; then
        print_error "Private key not found: $PRIVATE_KEY_FILE"
        print_error "Cannot create PKCS12 without the original private key!"
        exit 1
    fi

    print_status "Creating PKCS12 file..."

    if [[ -n "$ca_bundle" && -f "$ca_bundle" ]]; then
        print_status "Including CA bundle in PKCS12 file..."
        openssl pkcs12 -export -out "$P12_FILE" \
            -inkey "$PRIVATE_KEY_FILE" \
            -in "$CERT_FILE" \
            -certfile "$ca_bundle" \
            -password "pass:$p12_password"
    else
        openssl pkcs12 -export -out "$P12_FILE" \
            -inkey "$PRIVATE_KEY_FILE" \
            -in "$CERT_FILE" \
            -password "pass:$p12_password"
    fi

    if [ $? -eq 0 ]; then
        print_status "PKCS12 file created: $P12_FILE"
        echo "Use this file for HTTPS calls to the Bank's APIs"
    else
        print_error "Failed to create PKCS12 file"
        return 1
    fi
}

# Main execution
if [[ "$1" != "create_p12" && "$1" != "process_cert" ]]; then
    handle_existing_files
    generate_certificates
    verify_certificates
    display_file_info

    echo "=== NEXT STEPS ==="
    echo "1. Upload the CSR to Standard Chartered: $CSR_FILE"
    echo "2. After receiving their certificate, run:"
    echo "   $0 process_cert /path/to/bank/certificate.crt"
    echo "3. To create PKCS12 file:"
    echo "   $0 create_p12 /path/to/bank/certificate.crt mypassword [ca_bundle]"
    echo
fi

# If script is called with process_cert command
if [[ "$1" == "process_cert" ]]; then
    if [[ $# -lt 2 ]]; then
        echo "Usage: $0 process_cert <bank_certificate>"
        echo "Process and verify the certificate received from Standard Chartered"
        exit 1
    fi
    handle_bank_certificate "$2"
fi

# If script is called with create_p12 command
if [[ "$1" == "create_p12" ]]; then
    if [[ $# -lt 3 ]]; then
        echo "Usage: $0 create_p12 <bank_certificate> <password> [ca_bundle]"
        echo "Example: $0 create_p12 ./bank_cert.crt mypassword123 ./ca_bundle.crt"
        exit 1
    fi

    SIGNED_CERT="$2"
    P12_PASSWORD="$3"
    CA_BUNDLE="$4"

    create_pkcs12 "$SIGNED_CERT" "$CA_BUNDLE" "$P12_PASSWORD"
fi

echo
echo "=== QUICK COMMANDS FOR VERIFICATION ==="
echo "View private key: openssl rsa -in $PRIVATE_KEY_FILE -check -noout"
echo "View CSR details: openssl req -in $CSR_FILE -text -noout"
echo "View public key: openssl rsa -pubin -in $PUBLIC_KEY_FILE -text -noout"
echo "View certificate: openssl x509 -in $CERT_FILE -text -noout"