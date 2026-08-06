package so.cb.pki.csr.mapper;

import so.cb.pki.csr.dto.CsrResponse;
import so.cb.pki.csr.entity.Csr;
import so.cb.pki.csr.enums.CsrStatus;

import java.time.Instant;
import java.util.UUID;

public class CsrMapper {

    private CsrMapper() {
        // Private constructor to prevent instantiation
    }

    public static Csr toEntity(String bic, String csrPem, UUID institutionId) {
        Instant now = Instant.now();
        return Csr.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .bic(bic)
                .csrPem(csrPem)
                .status(CsrStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static CsrResponse toResponse(Csr entity) {
        if (entity == null) {
            return null;
        }
        return new CsrResponse(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getBic(),
                entity.getCsrPem(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
