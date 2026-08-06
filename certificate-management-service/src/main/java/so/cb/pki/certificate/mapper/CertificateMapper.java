package so.cb.pki.certificate.mapper;

import so.cb.pki.certificate.dto.CertificateResponse;
import so.cb.pki.certificate.dto.CertificateSummary;
import so.cb.pki.certificate.entity.Certificate;

public final class CertificateMapper {

    private CertificateMapper() {
    }

    public static CertificateResponse toResponse(Certificate entity) {
        if (entity == null) {
            return null;
        }

        return new CertificateResponse(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getCsrId(),
                entity.getBic(),
                entity.getSerialNumber(),
                entity.getCertificatePem(),
                entity.getStatus(),
                entity.getRevocationReason(),
                entity.getRevokedAt(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static CertificateSummary toSummary(Certificate entity, String institutionName) {
        if (entity == null) {
            return null;
        }

        return new CertificateSummary(
                entity.getId(),
                entity.getInstitutionId(),
                institutionName,
                entity.getCsrId(),
                entity.getBic(),
                entity.getSerialNumber(),
                entity.getStatus(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}
