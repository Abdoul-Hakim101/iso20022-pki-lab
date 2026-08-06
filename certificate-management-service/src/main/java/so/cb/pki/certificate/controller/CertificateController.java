package so.cb.pki.certificate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import so.cb.pki.certificate.service.CertificateService;
import so.cb.pki.certificate.dto.CertificateResponse;
import so.cb.pki.certificate.dto.CertificateSummary;
import so.cb.pki.certificate.dto.RevokeCertificateRequest;
import so.cb.pki.certificate.enums.CertificateStatus;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.dto.Response;
import so.cb.pki.shared.util.RequestUtils;

import java.util.Map;

@Tag(name = "Certificate Management", description = "Endpoints for X.509 certificate queries, revocation, and PEM downloads")
@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "Get Certificate by Serial Number", description = "Retrieves complete certificate details by its unique X.509 hex serial number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificate details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @GetMapping("/serial/{serialNumber}")
    public ResponseEntity<Response> getCertificateBySerialNumber(
            @Parameter(description = "Unique hex serial number of the certificate", required = true)
            @PathVariable String serialNumber,
            HttpServletRequest httpRequest
    ) {
        log.info("REST GET /api/v1/certificates/serial/{} - Fetch certificate by SerialNumber", serialNumber);
        CertificateResponse certificate = certificateService.getCertificateBySerialNumber(serialNumber);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("certificate", certificate),
                        "Certificate details retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "Search & List Certificates", description = "Retrieves a paginated list of certificate summaries with status filtering and keyword search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificates retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Response> getCertificates(
            @Parameter(description = "Optional status filter (ACTIVE, REVOKED, EXPIRED)")
            @RequestParam(required = false) CertificateStatus status,
            @Parameter(description = "Optional search keyword matching bank BIC or serial number")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest
    ) {
        log.info("REST GET /api/v1/certificates - Fetch paginated certificates (status: {}, search: {}, page: {}, size: {})",
                status, search, pageNumber, pageSize);
        PaginatedResponse<CertificateSummary> certificates = certificateService.getCertificates(status, search, pageNumber, pageSize);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("certificates", certificates),
                        "Certificates retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "Revoke Certificate", description = "Revokes an active X.509 certificate by serial number with a required revocation reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificate revoked successfully"),
            @ApiResponse(responseCode = "400", description = "Certificate is already revoked"),
            @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @PatchMapping("/serial/{serialNumber}/revoke")
    public ResponseEntity<Response> revokeCertificate(
            @Parameter(description = "Unique hex serial number of the certificate", required = true)
            @PathVariable String serialNumber,
            @RequestBody @Valid RevokeCertificateRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("REST PATCH /api/v1/certificates/serial/{}/revoke - Revoke certificate request", serialNumber);
        CertificateResponse certificate = certificateService.revokeCertificate(serialNumber, request);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("certificate", certificate),
                        "Certificate revoked successfully.",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "Download Root CA Chain PEM", description = "Downloads the Central Bank Root CA trust certificate in text/plain PEM format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Root CA chain PEM retrieved", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string")))
    })
    @GetMapping(value = "/chain.pem", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCaChainPem() {
        log.info("REST GET /api/v1/certificates/chain.pem - Download Root CA trust chain");
        String chainPem = certificateService.getCaChainPem();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chain.pem\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(chainPem);
    }

    @Operation(summary = "Download Leaf Certificate PEM", description = "Downloads only the leaf X.509 certificate for a bank in text/plain PEM format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leaf certificate PEM retrieved", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @GetMapping(value = "/serial/{serialNumber}/certificate.pem", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCertificatePem(
            @Parameter(description = "Unique hex serial number of the certificate", required = true)
            @PathVariable String serialNumber
    ) {
        log.info("REST GET /api/v1/certificates/serial/{}/certificate.pem - Download leaf certificate PEM", serialNumber);
        String certPem = certificateService.getCertificatePem(serialNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate.pem\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(certPem);
    }

    @Operation(summary = "Download Full Chain PEM Bundle", description = "Downloads the full certificate bundle (leaf + Root CA) in text/plain PEM format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Full chain PEM bundle retrieved", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @GetMapping(value = "/serial/{serialNumber}/fullchain.pem", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCertificateFullChainPem(
            @Parameter(description = "Unique hex serial number of the certificate", required = true)
            @PathVariable String serialNumber
    ) {
        log.info("REST GET /api/v1/certificates/serial/{}/fullchain.pem - Download full chain bundle", serialNumber);
        String fullChainPem = certificateService.getCertificateFullChainPem(serialNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fullchain.pem\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fullChainPem);
    }
}
