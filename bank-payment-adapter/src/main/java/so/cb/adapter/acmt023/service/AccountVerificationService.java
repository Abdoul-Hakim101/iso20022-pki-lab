package so.cb.adapter.acmt023.service;

import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.dto.SignedAcmt023Response;

public interface AccountVerificationService {
    SignedAcmt023Response createAndSignAcmt023(AccountVerificationRequestJson request);
}
