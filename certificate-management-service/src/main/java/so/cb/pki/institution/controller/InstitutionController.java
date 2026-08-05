package so.cb.pki.institution.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import so.cb.pki.institution.dto.CreateInstitutionRequest;
import so.cb.pki.institution.dto.InstitutionResponse;
import so.cb.pki.institution.enums.InstitutionStatus;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.dto.Response;
import so.cb.pki.shared.util.RequestUtils;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @PostMapping
    public ResponseEntity<Response> createInstitution(
            @RequestBody @Valid CreateInstitutionRequest request,
            HttpServletRequest httpRequest
    ) {

        InstitutionResponse institution = institutionService.createInstitution(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("institution", institution),
                        "Institution created successfully.",
                        HttpStatus.CREATED
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Response> updateInstitutionStatus(
            @PathVariable UUID id,
            @RequestParam InstitutionStatus status,
            HttpServletRequest httpRequest
    ) {

        InstitutionResponse institution =
                institutionService.updateInstitutionStatus(id, status);

        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("institution", institution),
                        "Institution status updated successfully.",
                        HttpStatus.OK
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getInstitutionById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {

        InstitutionResponse institution =
                institutionService.getInstitutionById(id);

        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("institution", institution),
                        "Institution retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @GetMapping
    public ResponseEntity<Response> getInstitutions(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest
    ) {

        PaginatedResponse<InstitutionResponse> institutions =
                institutionService.getInstitutions(
                        search,
                        pageNumber,
                        pageSize
                );

        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("institutions", institutions),
                        "Institutions retrieved successfully.",
                        HttpStatus.OK
                )
        );
    }

    @GetMapping("/active")
    public ResponseEntity<Response> isInstitutionActive(
            @RequestParam String bic,
            HttpServletRequest httpRequest
    ) {

        boolean active = institutionService.isInstitutionActive(bic);

        return ResponseEntity.ok(
                RequestUtils.getResponse(
                        httpRequest,
                        Map.of("active", active),
                        "Institution status checked successfully.",
                        HttpStatus.OK
                )
        );
    }
}

