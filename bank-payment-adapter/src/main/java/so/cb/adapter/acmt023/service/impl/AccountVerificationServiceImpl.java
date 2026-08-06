package so.cb.adapter.acmt023.service.impl;

import com.prowidesoftware.swift.model.mx.MxAcmt02300103;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.dto.SignedAcmt023Response;
import so.cb.adapter.acmt023.enums.AccountIdentifierType;
import so.cb.adapter.acmt023.service.AccountVerificationService;
import so.cb.adapter.shared.config.AdapterProperties;
import so.cb.adapter.shared.security.XmlSignatureSigningService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountVerificationServiceImpl implements AccountVerificationService {

    private final AdapterProperties adapterProperties;
    private final XmlSignatureSigningService xmlSignatureSigningService;

    @Override
    public SignedAcmt023Response createAndSignAcmt023(AccountVerificationRequestJson request) {
        String instructingBic = adapterProperties.getBankBic();
        String messageId = "AVR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Creating ISO 20022 acmt.023.001.03 for instructing BIC: {}, receiver BIC: {}, accountIdentifier: {}, type: {}",
                instructingBic, request.receiverBic(), request.accountIdentifier(), request.identifierType());

        try {
            MxAcmt02300103 mx = new MxAcmt02300103();
            IdentificationVerificationRequestV03 requestV03 = new IdentificationVerificationRequestV03();

            // 1. Header Assignment (assgnmt)
            IdentificationAssignment3 assgnmt = new IdentificationAssignment3();
            assgnmt.setMsgId(messageId);
            assgnmt.setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC));

            Party40Choice assgnr = new Party40Choice();
            PartyIdentification135 assgnrParty = new PartyIdentification135();
            Party38Choice assgnrChoice = new Party38Choice();
            OrganisationIdentification29 assgnrOrg = new OrganisationIdentification29();
            assgnrOrg.setAnyBIC(instructingBic);
            assgnrChoice.setOrgId(assgnrOrg);
            assgnrParty.setId(assgnrChoice);
            assgnr.setPty(assgnrParty);
            assgnmt.setAssgnr(assgnr);

            requestV03.setAssgnmt(assgnmt);

            // 2. Verification Item (vrfctn)
            IdentificationVerification4 vrfctn = new IdentificationVerification4();
            vrfctn.setId("VRF-" + System.currentTimeMillis());

            IdentificationInformation4 ptyAndAcctId = new IdentificationInformation4();

            // Account Identifier & Type (ACCT, EWLT, MSIS, IBAN)
            CashAccount40 acct = new CashAccount40();
            AccountIdentification4Choice acctIdChoice = new AccountIdentification4Choice();

            if (request.identifierType() == AccountIdentifierType.IBAN) {
                acctIdChoice.setIBAN(request.accountIdentifier());
            } else {
                GenericAccountIdentification1 othr = new GenericAccountIdentification1();
                othr.setId(request.accountIdentifier());

                AccountSchemeName1Choice schemeChoice = new AccountSchemeName1Choice();
                schemeChoice.setCd(request.identifierType().name());
                othr.setSchmeNm(schemeChoice);

                acctIdChoice.setOthr(othr);
            }

            acct.setId(acctIdChoice);
            ptyAndAcctId.setAcct(acct);

            // Receiving Bank Agent (agt)
            BranchAndFinancialInstitutionIdentification6 agt = new BranchAndFinancialInstitutionIdentification6();
            FinancialInstitutionIdentification18 finInstnId = new FinancialInstitutionIdentification18();
            finInstnId.setBICFI(request.receiverBic());
            agt.setFinInstnId(finInstnId);
            ptyAndAcctId.setAgt(agt);

            vrfctn.setPtyAndAcctId(ptyAndAcctId);
            requestV03.addVrfctn(vrfctn);

            mx.setIdVrfctnReq(requestV03);

            // Convert Prowide model to raw ISO 20022 XML string
            String rawXml = mx.message();

            // Digitally Sign ISO 20022 XML with bank's private.pem and certificate.pem
            String signedXml = xmlSignatureSigningService.signXml(rawXml);

            log.info("Successfully generated and signed ISO 20022 acmt.023.001.03 messageId: {}", messageId);

            return new SignedAcmt023Response(
                    "SIGNED",
                    messageId,
                    instructingBic,
                    request.receiverBic(),
                    request.accountIdentifier(),
                    request.identifierType(),
                    signedXml
            );

        } catch (Exception e) {
            log.error("Failed to build and sign acmt.023.001.03 message: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to generate acmt.023.001.03 message: " + e.getMessage(), e);
        }
    }
}
