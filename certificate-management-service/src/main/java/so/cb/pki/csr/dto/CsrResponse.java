package so.cb.pki.csr.dto;

import so.cb.pki.csr.enums.CsrStatus;

import java.time.Instant;
import java.util.UUID;

public record CsrResponse(
    UUID id,
    UUID institutionId,
    String bic,
    String csrPem,
    CsrStatus status,
    String rejectionReason,
    Instant createdAt,
    Instant updatedAt
) {}
