package so.cb.pki.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateInstitutionRequest(
    @NotBlank(message = "Institution name is required")
    String name,

    @NotBlank(message = "BIC code is required")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "Invalid BIC format")
    String bic
) {}
