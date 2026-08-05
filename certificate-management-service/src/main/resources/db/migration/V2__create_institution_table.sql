-- Create the pki schema if it does not exist
CREATE SCHEMA IF NOT EXISTS pki;

-- Create the institution table within the pki schema
CREATE TABLE IF NOT EXISTS pki.institution (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    bic VARCHAR(11) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

-- Index the bic column for faster lookups
CREATE INDEX IF NOT EXISTS idx_institution_bic ON pki.institution(bic);

-- Seed default institutions for the development environment

INSERT INTO pki.institution (
    id,
    name,
    bic,
    status,
    created_at,
    updated_at
)
VALUES
    (gen_random_uuid(), 'Salaam Somali Bank', 'SSBMSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Dahabshil Bank International', 'DAHISOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Amal Bank', 'AALLSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'IBS (International Bank of Somalia)', 'IBOSSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Premier Bank Ltd', 'PBSMSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Daryeel Bank', 'DARYSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'SomBank Ltd', 'SOMNSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'MyBank Limited', 'MYBASOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Amana Bank', 'AMBKSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Agro Bank', 'AGROSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Galaxy International Bank', 'GLXYSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Idman Community Bank', 'IDMNSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Bushra Business Bank', 'BUHBSOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Ziraat Katilim Bank Somalia', 'ZKBASOS0', 'ACTIVE', NOW(), NOW()),
    (gen_random_uuid(), 'Bulsho Development Bank', 'BDBKSOS0', 'ACTIVE', NOW(), NOW())
    ON CONFLICT (bic) DO NOTHING;