# 🔑 Bank Certificate & Private Key Onboarding Guide

This document provides step-by-step instructions for commercial bank engineers on how to generate the bank's RSA Private Key (`private.pem`), generate a Certificate Signing Request (`request.csr`), submit it to the **Central Bank PKI Service**, obtain the signed Leaf Certificate (`certificate.pem`) and CA Trust Chain (`chain.pem`), and configure them inside the **Bank Payment Adapter**.

---

## 📋 Overview of Required Files

Before starting the Bank Payment Adapter, the `./certs/` directory inside `bank-payment-adapter` must contain the following 3 files:

| File Name | Format | Description | Source |
| :--- | :--- | :--- | :--- |
| **`private.pem`** | RSA PEM Private Key | Bank's secret key used to digitally sign outgoing ISO 20022 messages. | Generated locally by commercial bank |
| **`certificate.pem`** | X.509 PEM Certificate | Bank's leaf certificate containing public key & BIC (`CN=PMRBSOMM`). | Issued by Central Bank PKI Service |
| **`chain.pem`** | X.509 PEM Certificate | Central Bank Root CA Certificate (Trust Anchor). | Downloaded from Central Bank PKI Service |

---

## 🚀 Step-by-Step Onboarding Instructions

### Step 1: Generate Bank Private Key & CSR (OpenSSL)

On the commercial bank server, generate a 2048-bit RSA private key (`private.pem`) and a Certificate Signing Request (`request.csr`) with Subject DN set to the bank's BIC code (e.g. `CN=PMRBSOMM` for Premier Bank):

```bash
# 1. Create certs directory in bank-payment-adapter
mkdir -p certs
cd certs

# 2. Generate RSA 2048-bit Private Key (private.pem)
openssl genrsa -out private.pem 2048

# 3. Generate CSR (request.csr) with Bank BIC (CN=PMRBSOMM)
openssl req -new -key private.pem -out request.csr -subj "/C=SO/O=Premier Bank/CN=PMRBSOMM"
```

---

### Step 2: Submit CSR to Central Bank PKI Service

Submit the generated `request.csr` text content to the Central Bank PKI Service (`POST /api/v1/csrs` on port `8080`):

```bash
curl -X POST "http://localhost:8080/api/v1/csrs" \
  -H "Content-Type: application/json" \
  -d '{
    "bic": "PMRBSOMM",
    "csrPem": "-----BEGIN CERTIFICATE REQUEST-----\nMIICvDCCAaQCAQAwdzELMAkGA1UEBhMCU08xHzAdBgNVBAoMFkNlbnRyYWwgQmFu...\n-----END CERTIFICATE REQUEST-----"
  }'
```

*Response*:
```json
{
  "id": "csr-12345",
  "bic": "PMRBSOMM",
  "status": "PENDING",
  "createdAt": "2026-08-06T10:00:00Z"
}
```

---

### Step 3: Central Bank Administrator Approves CSR

The Central Bank Administrator reviews and approves the pending CSR (`PATCH /api/v1/csrs/{id}/review`):

```bash
curl -X PATCH "http://localhost:8080/api/v1/csrs/csr-12345/review" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED"
  }'
```

*Upon approval, the Central Bank PKI engine automatically publishes `CsrApprovedEvent` and issues the signed X.509 X.509 certificate.*

---

### Step 4: Download `chain.pem` and `certificate.pem`

Download the Central Bank Root CA Certificate (`chain.pem`) and the bank's issued certificate (`certificate.pem`):

```bash
# 1. Download Central Bank Root CA Trust Anchor (chain.pem)
curl -s "http://localhost:8080/api/v1/certificates/chain.pem" -o chain.pem

# 2. Download Issued Certificate for Institution PMRBSOMM
curl -s "http://localhost:8080/api/v1/certificates/institution/PMRBSOMM"
```

Save the `certificatePem` field from the JSON response into `certificate.pem`.

---

### Step 5: Verify Directory Placement

Place all 3 files inside `bank-payment-adapter/certs/`:

```text
bank-payment-adapter/
├── certs/
│   ├── chain.pem         <-- Central Bank Root CA Trust Anchor
│   ├── certificate.pem   <-- Commercial Bank Leaf Certificate
│   └── private.pem       <-- Commercial Bank RSA Private Key
├── pom.xml
└── src/
```

---

### Step 6: Launch Bank Payment Adapter

Start the adapter. The `AdapterStartupValidator` will automatically execute the 4 local in-memory cryptographic boot checks:

```bash
./mvnw.cmd spring-boot:run
```

*Expected Startup Output*:
```text
INFO  Executing Bank Payment Adapter Cryptographic Startup Security Check...
INFO  X.509 Certificate parsing & validity date check passed.
INFO  CA Trust Verification passed: Bank certificate was issued and signed by Central Bank Root CA (CN=Central Bank CA)
INFO  RSA Keypair match verification passed: private.pem matches certificate.pem public key.
INFO  =========================================================================================
INFO  SUCCESS: Bank Payment Adapter Cryptographic Startup Security Check PASSED!
INFO  Bank Subject DN  : C=SO,O=Premier Bank,CN=PMRBSOMM
INFO  Root CA Issuer   : C=SO,O=Central Bank,CN=Central Bank CA
INFO  Cert Valid From  : Wed Aug 05 11:28:31 EAT 2026
INFO  Cert Valid To    : Fri Aug 06 11:28:31 EAT 2027
INFO  =========================================================================================
```
