package so.cb.pki.institution.mapper;

import so.cb.pki.institution.dto.CreateInstitutionRequest;
import so.cb.pki.institution.dto.InstitutionResponse;
import so.cb.pki.institution.entity.Institution;
import so.cb.pki.institution.enums.InstitutionStatus;

import java.time.Instant;
import java.util.UUID;

public class InstitutionMapper {

    private InstitutionMapper() {
        // Private constructor to prevent instantiation
    }

    public static  Institution toEntity(CreateInstitutionRequest request) {
        if (request == null) {
            return null;
        }
        Instant now = Instant.now();
        return Institution.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .bic(request.bic())
                .status(InstitutionStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static InstitutionResponse toResponse(Institution entity) {
        if (entity == null) {
            return null;
        }
        return new InstitutionResponse(
                entity.getId(),
                entity.getName(),
                entity.getBic(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
