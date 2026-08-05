package so.cb.pki.csr.dto;

import jakarta.validation.constraints.NotNull;
import so.cb.pki.csr.enums.CsrStatus;

public record ReviewCsrRequest(
    @NotNull(message = "Review status is required")
    CsrStatus status,

    String rejectionReason
) {}
