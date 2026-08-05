package so.cb.pki.certificate.dto;

import so.cb.pki.certificate.enums.CertificateStatus;

import java.time.Instant;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID institutionId,
        UUID csrId,
        String bic,
        String serialNumber,
        String certificatePem,
        CertificateStatus status,
        String revocationReason,
        Instant revokedAt,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt
) {
}
