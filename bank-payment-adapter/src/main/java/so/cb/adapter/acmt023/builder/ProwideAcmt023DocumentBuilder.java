package so.cb.adapter.acmt023.builder;

import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;

public interface ProwideAcmt023DocumentBuilder {
    String buildDocumentXml(String instructingBic, AccountVerificationRequestJson request);
}
