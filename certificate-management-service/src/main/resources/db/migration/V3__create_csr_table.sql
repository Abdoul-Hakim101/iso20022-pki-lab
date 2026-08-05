-- Create the csr table within the pki schema
CREATE TABLE IF NOT EXISTS pki.csr (
    id UUID NOT NULL,
    institution_id UUID NOT NULL,
    bic VARCHAR(11) NOT NULL,
    csr_pem TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_csr_institution FOREIGN KEY (institution_id) REFERENCES pki.institution(id) ON DELETE CASCADE
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_csr_bic ON pki.csr(bic);
CREATE INDEX IF NOT EXISTS idx_csr_status ON pki.csr(status);
CREATE INDEX IF NOT EXISTS idx_csr_institution_id ON pki.csr(institution_id);
