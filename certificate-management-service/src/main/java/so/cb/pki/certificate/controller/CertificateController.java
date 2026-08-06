package so.cb.pki.certificate.controller;

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
import so.cb.pki.certificate.dto.RevokeCertificateRequest;
import so.cb.pki.certificate.enums.CertificateStatus;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.dto.Response;
import so.cb.pki.shared.util.RequestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/{id}")
    public ResponseEntity<Response> getCertificateById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        log.info("REST GET /api/v1/certificates/{} - Fetch certificate by ID", id);
        CertificateResponse certificate = certificateService.getCertificateById(id);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("certificate", certificate),
                        "Certificate details retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @GetMapping("/serial/{serialNumber}")
    public ResponseEntity<Response> getCertificateBySerialNumber(
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

    @GetMapping("/institution/{bic}")
    public ResponseEntity<Response> getCertificatesByBic(
            @PathVariable String bic,
            HttpServletRequest httpRequest
    ) {
        log.info("REST GET /api/v1/certificates/institution/{} - Fetch certificates by bank BIC", bic);
        List<CertificateResponse> certificates = certificateService.getCertificatesByBic(bic);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("certificates", certificates),
                        "Bank certificates retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @GetMapping
    public ResponseEntity<Response> getCertificates(
            @RequestParam(required = false) CertificateStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest
    ) {
        log.info("REST GET /api/v1/certificates - Fetch paginated certificates (status: {}, search: {}, page: {}, size: {})",
                status, search, pageNumber, pageSize);
        PaginatedResponse<CertificateResponse> certificates = certificateService.getCertificates(status, search, pageNumber, pageSize);
        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("certificates", certificates),
                        "Certificates retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @PatchMapping("/serial/{serialNumber}/revoke")
    public ResponseEntity<Response> revokeCertificate(
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

    @GetMapping(value = "/chain.pem", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCaChainPem() {
        log.info("REST GET /api/v1/certificates/chain.pem - Download Root CA trust chain");
        String chainPem = certificateService.getCaChainPem();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chain.pem\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(chainPem);
    }

    @GetMapping(value = "/serial/{serialNumber}/fullchain.pem", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCertificateFullChainPem(@PathVariable String serialNumber) {
        log.info("REST GET /api/v1/certificates/serial/{}/fullchain.pem - Download full chain bundle", serialNumber);
        String fullChainPem = certificateService.getCertificateFullChainPem(serialNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fullchain.pem\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fullChainPem);
    }
}
