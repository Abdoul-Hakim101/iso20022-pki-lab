package so.cb.pki.certificate.dto;

import so.cb.pki.certificate.enums.CertificateStatus;

import java.time.Instant;
import java.util.UUID;

public record CertificateSummary(
        UUID id,
        UUID institutionId,
        String institutionName,
        UUID csrId,
        String bic,
        String serialNumber,
        CertificateStatus status,
        Instant validFrom,
        Instant validTo,
        Instant revokedAt,
        Instant createdAt
) {
}
