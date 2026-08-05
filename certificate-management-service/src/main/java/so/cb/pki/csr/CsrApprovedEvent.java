package so.cb.pki.csr;

import java.util.UUID;

/**
 * Domain event published when a Certificate Signing Request (CSR) is approved by the PKI administrator.
 *
 * @param csrId the unique identifier of the approved CSR
 * @param institutionId the unique identifier of the bank institution
 * @param bic the Business Identifier Code (BIC) of the bank
 * @param csrPem the raw PEM-encoded CSR content
 */
public record CsrApprovedEvent(
    UUID csrId,
    UUID institutionId,
    String bic,
    String csrPem
) {}
