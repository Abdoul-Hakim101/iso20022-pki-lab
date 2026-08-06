package so.cb.adapter.acmt023.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountIdentifierType {
    ACCT("Account"),
    EWLT("Wallet"),
    MSIS("Phone Number"),
    IBAN("IBAN");

    private final String label;
}
