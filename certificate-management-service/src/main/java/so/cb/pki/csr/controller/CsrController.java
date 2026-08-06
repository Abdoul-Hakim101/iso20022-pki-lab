package so.cb.pki.csr.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import so.cb.pki.csr.service.CsrService;
import so.cb.pki.csr.dto.CsrResponse;
import so.cb.pki.csr.dto.ReviewCsrRequest;
import so.cb.pki.csr.enums.CsrStatus;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.dto.Response;
import so.cb.pki.shared.util.RequestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Tag(name = "CSR Management", description = "Endpoints for bank Certificate Signing Request (CSR) upload, review, and query")
@RestController
@RequestMapping("/api/v1/csrs")
@RequiredArgsConstructor
public class CsrController {

    private final CsrService csrService;

    @Operation(summary = "Upload CSR", description = "Uploads a Certificate Signing Request (CSR) file via multipart/form-data for an active bank institution.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "CSR uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSR PEM format or empty file"),
            @ApiResponse(responseCode = "404", description = "Institution with provided BIC not found or inactive")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> uploadCsr(
            @Parameter(description = "PKCS#10 CSR file (.csr or .pem)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Business Identifier Code (BIC) of the requesting bank", required = true, example = "CBKSSOM1XXX")
            @RequestParam("bic") String bic,
            HttpServletRequest httpRequest
    ) {
        csrService.uploadCsr(file, bic);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                RequestUtils.getResponse(
                        httpRequest,
                        null,
                        "CSR uploaded successfully.",
                        HttpStatus.CREATED
                )
        );
    }

    @Operation(summary = "Review CSR", description = "Approves or rejects a pending CSR. Approving triggers automatic certificate issuance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSR reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "CSR already reviewed or missing rejection reason"),
            @ApiResponse(responseCode = "404", description = "CSR not found")
    })
    @PatchMapping("/{id}/review")
    public ResponseEntity<Response> reviewCsr(
            @Parameter(description = "Unique identifier (UUID) of the CSR", required = true)
            @PathVariable UUID id,
            @RequestBody @Valid ReviewCsrRequest request,
            HttpServletRequest httpRequest
    ) {
        CsrResponse csr = csrService.reviewCsr(id, request);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("csr", csr),
                        "CSR status updated successfully.",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "Get CSR by ID", description = "Retrieves CSR details by its unique UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSR retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "CSR not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response> getCsrById(
            @Parameter(description = "Unique identifier (UUID) of the CSR", required = true)
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        CsrResponse csr = csrService.getCsrById(id);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("csr", csr),
                        "CSR retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "Get CSRs by BIC", description = "Retrieves all CSR records associated with a specific bank BIC.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRs retrieved successfully")
    })
    @GetMapping("/institution/{bic}")
    public ResponseEntity<Response> getCsrsByBic(
            @Parameter(description = "Business Identifier Code (BIC) of the bank", required = true, example = "CBKSSOM1XXX")
            @PathVariable String bic,
            HttpServletRequest httpRequest
    ) {
        List<CsrResponse> csrs = csrService.getCsrsByBic(bic);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("csrs", csrs),
                        "CSRs retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "Search & List CSRs", description = "Retrieves a paginated list of CSRs with optional status filtering and keyword search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRs retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Response> getCsrs(
            @Parameter(description = "Optional status filter (PENDING, APPROVED, REJECTED)")
            @RequestParam(required = false) CsrStatus status,
            @Parameter(description = "Optional BIC search keyword")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest
    ) {
        PaginatedResponse<CsrResponse> csrs = csrService.getCsrs(status, search, pageNumber, pageSize);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("csrs", csrs),
                        "CSRs retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }
}
