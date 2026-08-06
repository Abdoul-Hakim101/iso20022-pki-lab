package so.cb.adapter.acmt023.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AccountVerificationController {

    private final AccountVerificationService accountVerificationService;

    @Operation(summary = "Sign Account Verification Request (acmt.023)",
            description = "Accepts a 3-field JSON request from Core Banking (receiverBic, accountIdentifier, identifierType: ACCT/EWLT/MSIS/IBAN), injects bank's BIC from application.yaml, builds ISO 20022 acmt.023.001.03 FPEnvelope XML, and returns raw digitally signed XML.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw ISO 20022 acmt.023 FPEnvelope XML created and digitally signed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields"),
            @ApiResponse(responseCode = "500", description = "Cryptographic signing failure or missing certificates")
    })
    @PostMapping(value = "/sign", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> signAcmt023Request(
            @RequestBody @Valid AccountVerificationRequestJson request
    ) {
        String signedXml = accountVerificationService.createAndSignAcmt023Xml(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_XML)
                .body(signedXml);
    }
}
