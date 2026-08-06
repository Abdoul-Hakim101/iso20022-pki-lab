# Central Bank PKI Certificate Management Service

An enterprise-grade **Public Key Infrastructure (PKI) & Certificate Lifecycle Management Service** for Central Bank payment systems and commercial bank ISO 20022 XML digital signatures. Built with **Java 24**, **Spring Boot 4**, **Spring Modulith**, **PostgreSQL 17**, and **BouncyCastle Crypto APIs**.

> [!NOTE]
> **Educational & Practice Lab Project**
> This repository is built strictly for educational, research, and practice purposes to demonstrate real-world implementation of **Central Bank Public Key Infrastructure (PKI)**, **ISO 20022 XML Digital Signatures**, and **Spring Modulith Architecture**.

---

## 🏛️ System Overview

The Certificate Management Service acts as the **Central Bank Certificate Authority (CA)** for commercial bank onboarding, Certificate Signing Request (CSR) verification, X.509 Leaf Certificate issuance, revocation management, and CA trust chain distribution (`chain.pem` & `fullchain.pem`).

```mermaid
flowchart LR
    Bank[Commercial Bank\nGenerates Private Key & request.csr] -->|POST /api/v1/csrs| CSR[csr module\nValidates & Stores PENDING CSR]
    Admin[Central Bank Admin] -->|PATCH /api/v1/csrs/{id}/review| CSR
    CSR -->|Publishes CsrApprovedEvent| NOTIF[notification module\nListens to Event]
    NOTIF -->|Triggers Issuance| CERT[certificate module\nBouncyCastle CA Engine]
    CERT -->|Signs X.509 Cert| DB[(PostgreSQL pki.certificate)]
    Bank -->|GET /api/v1/certificates/chain.pem| CERT
```

---

## 🧩 Modular Monolith Architecture

The application strictly enforces **Spring Modulith** encapsulation boundaries across five core domain modules:

| Module | Schema / Scope | Responsibilities |
| :--- | :--- | :--- |
| **`institution`** | `pki.institution` | Commercial bank registration, status management (`ACTIVE`, `INACTIVE`, `SUSPENDED`), and BIC validation. |
| **`csr`** | `pki.csr` | CSR upload, BouncyCastle PKCS#10 validation, review workflow (`PENDING`, `APPROVED`, `REJECTED`), and domain event publishing (`CsrApprovedEvent`). |
| **`certificate`** | `pki.certificate` | Central Bank CA signing engine, Root CA key provider, X.509 Leaf Certificate issuance, revocation management, and PEM downloads (`chain.pem`, `fullchain.pem`). |
| **`notification`** | Service / Events | Spring Modulith event listener for `CsrApprovedEvent`, audit logging, and `FailedEventRepublisher` retry scheduling. |
| **`shared`** | Utility / DTO | Common REST response wrappers (`Response`), exception handling (`GlobalExceptionHandler`), and `PaginatedResponse`. |

---

## 🚀 Key Features

* **BouncyCastle X.509 CA Engine**: Automatically parses PKCS#10 CSRs, generates 64-bit random serial numbers, sets 1-year validity, and signs Leaf Certificates with RSA 4096-bit signatures (`SHA256withRSA`).
* **Profile-Aware Security Enforcement**:
  * **`dev` / `test` Profiles**: Auto-generates a 4096-bit RSA Root CA keypair and self-signed `chain.pem` in `./keys/ca/` on first startup if missing.
  * **`prod` Profile**: Strictly enforces pre-configured POSIX directory keys (`/etc/pki/ca/private.pem` & `chain.pem`). Startup halts immediately with an exception if keys are missing.
* **Spring Modulith Event-Driven Architecture**: Asynchronous, transactional domain event publishing (`CsrApprovedEvent`) with automatic retry for failed event publications.
* **Flyway Migration & Schema Isolation**: Database migrations (`V1` to `V4`) creating dedicated `pki` schema tables (`pki.institution`, `pki.csr`, `pki.certificate`).
* **Multi-Stage Docker & Compose Support**: Lightweight Alpine-based multi-stage Docker build running under a non-root security context (`10001:10001`).

---

## 📡 REST API Reference

### 1. Institution Management (`/api/v1/institutions`)
- `POST /api/v1/institutions` — Register new bank institution
- `PATCH /api/v1/institutions/{id}/status?status=ACTIVE` — Update operational status
- `GET /api/v1/institutions/{id}` — Fetch institution details by ID
- `GET /api/v1/institutions` — Search & list institutions (paginated)
- `GET /api/v1/institutions/active?bic=PMRBSOMM` — Check if institution is active

### 2. CSR Management (`/api/v1/csrs`)
- `POST /api/v1/csrs` — Commercial bank uploads CSR (`request.csr`)
- `PATCH /api/v1/csrs/{id}/review` — Admin approves or rejects CSR
- `GET /api/v1/csrs/{id}` — Retrieve CSR details by ID
- `GET /api/v1/csrs/institution/{bic}` — Retrieve bank CSRs by BIC
- `GET /api/v1/csrs` — Filter & list CSRs (paginated)

### 3. Certificate Management (`/api/v1/certificates`)
- `GET /api/v1/certificates/{id}` — Fetch issued certificate details by ID
- `GET /api/v1/certificates/serial/{serialNumber}` — Fetch certificate by Serial Number
- `GET /api/v1/certificates/institution/{bic}` — Fetch certificates by bank BIC
- `GET /api/v1/certificates` — Search certificates by status/query (paginated)
- `PATCH /api/v1/certificates/serial/{serialNumber}/revoke` — Revoke certificate with reason
- `GET /api/v1/certificates/chain.pem` — Download Central Bank Root CA Certificate
- `GET /api/v1/certificates/serial/{serialNumber}/fullchain.pem` — Download `bank_cert.pem` + `chain.pem` bundle

---

## ⚙️ Configuration Properties

| Property | Default Value | Environment Variable | Description |
| :--- | :--- | :--- | :--- |
| `pki.ca.dir` | `./keys/ca` | `PKI_CA_DIR` | Directory containing CA keys on disk |
| `pki.ca.private-key-file` | `private.pem` | `PKI_CA_KEY_FILE` | Central Bank Root CA Private Key file name |
| `pki.ca.certificate-file` | `chain.pem` | `PKI_CA_CERT_FILE` | Central Bank Root CA Public Certificate file name |
| `pki.ca.issuer-dn` | `CN=Central Bank...` | `PKI_CA_ISSUER_DN` | Central Bank Root CA Distinguished Name |
| `pki.ca.validity-years` | `10` | `PKI_CA_VALIDITY_YEARS` | Validity of Root CA certificate in years |
| `application.events.retry.max-attempts` | `5` | `EVENTS_RETRY_MAX_ATTEMPTS` | Max retry attempts for failed Modulith events |
| `application.events.retry.delay` | `PT60S` | `EVENTS_RETRY_DELAY` | ISO-8601 duration between event retries |

---

## 🛠️ Local Development & Build

### Prerequisites
- JDK 24 (OpenJDK 24)
- Maven 3.9+
- PostgreSQL 17

### Build & Run
```bash
# Clone the repository
git clone https://github.com/Abdoul-Hakim101/iso20022-pki-lab.git
cd iso20022-lab/certificate-management-service

# Compile and package
./mvnw.cmd clean compile

# Run tests
./mvnw.cmd test

# Run application locally (Dev Profile)
./mvnw.cmd spring-boot:run
```

---

## 🐳 Docker Deployment

Run the application with PostgreSQL 17 via Docker Compose:

```bash
# From workspace root directory
cp .env.example .env

# Build and start services in background
docker compose up -d

# View container logs
docker compose logs -f certificate-management-service
```

---

## 📜 License
Central Bank of Somalia PKI Lab — All Rights Reserved.
