# Standard Chartered Bank Certificates Setup

## 🚀 Generate Certificates

```bash
# 1. Make executable
chmod +x generate-bank-certificates.sh

# 2. Run generation
./generate-bank-certificates.sh

# 3. After bank sends certificate, create PKCS12:
./generate-bank-certificates.sh create_p12 /path/to/stanchart_cert.crt your_password
//bash generate-bank-certificates.sh create_p12 client.cer oracle_4U

```

## 📤 Upload to Standard Chartered
Portal: https://openbanking.sc.com/

### Upload these files:

* api.yourcompany.com.csr (Certificate Signing Request)

* api.yourcompany.com_public.key (Public Key)

### 📁 Output Location
~/stanchart_bank_certs/

## 📋 Standard Chartered Requirements
* RSA 2048-bit keys
* Proper CSR subject fields
* PKCS12 format for client authentication

