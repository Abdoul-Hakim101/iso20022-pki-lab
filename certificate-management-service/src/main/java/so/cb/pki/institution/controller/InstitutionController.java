package so.cb.pki.institution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Institution Management", description = "Endpoints for commercial bank institution registration and status lifecycle management")
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @Operation(summary = "Register Institution", description = "Registers a new commercial bank institution with unique name and BIC code.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Institution registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or BIC already exists")
    })
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

    @Operation(summary = "Update Institution Status", description = "Updates the operational status (ACTIVE, INACTIVE, SUSPENDED) of a bank institution.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institution status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Institution not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Response> updateInstitutionStatus(
            @Parameter(description = "Unique identifier (UUID) of the institution", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New operational status (ACTIVE, INACTIVE, SUSPENDED)", required = true)
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

    @Operation(summary = "Search & List Institutions", description = "Retrieves a paginated list of bank institutions, optionally filtered by keyword search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institutions retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Response> getInstitutions(
            @Parameter(description = "Optional search keyword to match bank name or BIC")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Number of items per page")
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
}

