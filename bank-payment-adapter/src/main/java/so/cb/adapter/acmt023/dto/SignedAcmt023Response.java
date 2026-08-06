package so.cb.adapter.acmt023.dto;

import so.cb.adapter.acmt023.enums.AccountIdentifierType;

public record SignedAcmt023Response(
        String status,
        String messageId,
        String instructingBic,
        String receiverBic,
        String accountIdentifier,
        AccountIdentifierType identifierType,
        String signedXml
) {}
