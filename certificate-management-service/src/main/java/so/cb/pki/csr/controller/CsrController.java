package so.cb.pki.csr.controller;

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

@RestController
@RequestMapping("/api/v1/csrs")
@RequiredArgsConstructor
public class CsrController {

    private final CsrService csrService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> uploadCsr(
            @RequestParam("file") MultipartFile file,
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

    @PatchMapping("/{id}/review")
    public ResponseEntity<Response> reviewCsr(
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

    @GetMapping("/{id}")
    public ResponseEntity<Response> getCsrById(
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

    @GetMapping("/institution/{bic}")
    public ResponseEntity<Response> getCsrsByBic(
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

    @GetMapping
    public ResponseEntity<Response> getCsrs(
            @RequestParam(required = false) CsrStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int pageNumber,
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
