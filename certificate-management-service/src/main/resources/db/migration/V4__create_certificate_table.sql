-- Migration: V4__create_certificate_table.sql
-- Description: Create certificate table in pki schema to store issued X.509 certificates and revocation metadata

CREATE TABLE IF NOT EXISTS pki.certificate (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES pki.institution(id),
    csr_id UUID NOT NULL UNIQUE REFERENCES pki.csr(id),
    bic VARCHAR(11) NOT NULL,
    serial_number VARCHAR(64) NOT NULL UNIQUE,
    certificate_pem TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    revocation_reason TEXT,
    revoked_at TIMESTAMP WITH TIME ZONE,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_certificate_bic ON pki.certificate(bic);
CREATE INDEX IF NOT EXISTS idx_certificate_serial_number ON pki.certificate(serial_number);
CREATE INDEX IF NOT EXISTS idx_certificate_status ON pki.certificate(status);
CREATE INDEX IF NOT EXISTS idx_certificate_institution_id ON pki.certificate(institution_id);
