package so.cb.adapter.acmt023.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.service.AccountVerificationService;

@Tag(name = "Account Verification (acmt.023)", description = "Endpoints for creating and digitally signing ISO 20022 acmt.023.001.03 Account Verification Requests")
@RestController
@RequestMapping("/api/v1/adapter/acmt023")
public class AccountVerificationController {

    private final AccountVerificationService prowideService;
    private final AccountVerificationService templateService;
    private final AccountVerificationService jaxbService;

    public AccountVerificationController(
            @Qualifier("prowideAccountVerificationService") AccountVerificationService prowideService,
            @Qualifier("templateAccountVerificationService") AccountVerificationService templateService,
            @Qualifier("jaxbAccountVerificationService") AccountVerificationService jaxbService
    ) {
        this.prowideService = prowideService;
        this.templateService = templateService;
        this.jaxbService = jaxbService;
    }

    @Operation(summary = "Sign Account Verification Request via Prowide Engine (acmt.023)",
            description = "Accepts a 3-field JSON request from Core Banking, uses Prowide ISO 20022 Engine (MxAcmt02300103) to build <document:Document>, wraps inside FPEnvelope XML, and returns raw digitally signed XML.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw ISO 20022 acmt.023 FPEnvelope XML created and digitally signed successfully via Prowide"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields"),
            @ApiResponse(responseCode = "500", description = "Cryptographic signing failure or missing certificates")
    })
    @PostMapping(value = "/prowide/sign", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> signAcmt023WithProwide(
            @RequestBody @Valid AccountVerificationRequestJson request
    ) {
        String signedXml = prowideService.createAndSignAcmt023Xml(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_XML)
                .body(signedXml);
    }

    @Operation(summary = "Sign Account Verification Request via Custom Template Engine (acmt.023)",
            description = "Accepts a 3-field JSON request from Core Banking, uses Custom XML Template Engine to build <document:Document>, wraps inside FPEnvelope XML, and returns raw digitally signed XML.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw ISO 20022 acmt.023 FPEnvelope XML created and digitally signed successfully via Custom Template"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields"),
            @ApiResponse(responseCode = "500", description = "Cryptographic signing failure or missing certificates")
    })
    @PostMapping(value = "/template/sign", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> signAcmt023WithTemplate(
            @RequestBody @Valid AccountVerificationRequestJson request
    ) {
        String signedXml = templateService.createAndSignAcmt023Xml(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_XML)
                .body(signedXml);
    }

    @Operation(summary = "Sign Account Verification Request via JAXB XML Binding Engine (acmt.023)",
            description = "Accepts a 3-field JSON request from Core Banking, uses JAXB XML Binding Engine to build <document:Document>, wraps inside FPEnvelope XML, and returns raw digitally signed XML.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw ISO 20022 acmt.023 FPEnvelope XML created and digitally signed successfully via JAXB Binding"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields"),
            @ApiResponse(responseCode = "500", description = "Cryptographic signing failure or missing certificates")
    })
    @PostMapping(value = "/jaxb/sign", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> signAcmt023WithJaxb(
            @RequestBody @Valid AccountVerificationRequestJson request
    ) {
        String signedXml = jaxbService.createAndSignAcmt023Xml(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_XML)
                .body(signedXml);
    }
}
