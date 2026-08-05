package so.cb.pki.institution.dto;

import so.cb.pki.institution.enums.InstitutionStatus;
import java.time.Instant;
import java.util.UUID;

public record InstitutionResponse(
    UUID id,
    String name,
    String bic,
    InstitutionStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
