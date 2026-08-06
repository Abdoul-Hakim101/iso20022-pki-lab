package so.cb.adapter.acmt023.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import so.cb.adapter.acmt023.enums.AccountIdentifierType;

public record AccountVerificationRequestJson(
        @NotBlank(message = "Receiver BIC is required")
        String receiverBic,

        @NotBlank(message = "Account identifier value is required")
        String accountIdentifier,

        @NotNull(message = "Identifier type is required (ACCT, EWLT, MSIS, IBAN)")
        AccountIdentifierType identifierType
) {}
