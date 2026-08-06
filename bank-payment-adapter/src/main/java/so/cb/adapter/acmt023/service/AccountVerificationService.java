package so.cb.adapter.acmt023.service;

import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;

public interface AccountVerificationService {
    String createAndSignAcmt023Xml(AccountVerificationRequestJson request);
}
