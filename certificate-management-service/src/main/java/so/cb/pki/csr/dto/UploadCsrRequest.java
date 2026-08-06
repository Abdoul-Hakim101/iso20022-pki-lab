package so.cb.pki.csr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

public record UploadCsrRequest(
    @NotBlank(message = "BIC code is required")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "Invalid BIC format")
    String bic,

    @NotNull(message = "CSR file is required")
    MultipartFile file
) {}

