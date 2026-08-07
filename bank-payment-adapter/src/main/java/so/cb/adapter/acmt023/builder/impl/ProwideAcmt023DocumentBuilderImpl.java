package so.cb.adapter.acmt023.builder.impl;

import com.prowidesoftware.swift.model.mx.MxAcmt02300103;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import so.cb.adapter.acmt023.builder.ProwideAcmt023DocumentBuilder;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.enums.AccountIdentifierType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
public class ProwideAcmt023DocumentBuilderImpl implements ProwideAcmt023DocumentBuilder {

    @Override
    public String buildDocumentXml(String instructingBic, AccountVerificationRequestJson request) {
        String timestampPattern = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(java.time.Instant.now());
        String randomSuffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
        String msgId = instructingBic + timestampPattern + randomSuffix; // Exactly 31 characters
        String receiverBic = request.receiverBic();

        log.info("Building Prowide ISO 20022 document:Document XML for acmt.023.001.03 (instructingBic: {}, receiverBic: {})",
                instructingBic, receiverBic);

        try {
            MxAcmt02300103 mx = new MxAcmt02300103();
            IdentificationVerificationRequestV03 requestV03 = new IdentificationVerificationRequestV03();

            // 1. Header Assignment (assgnmt)
            IdentificationAssignment3 assgnmt = new IdentificationAssignment3();
            assgnmt.setMsgId(msgId);
            assgnmt.setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC));

            // Assignor (Sender Bank BIC)
            Party40Choice assgnr = new Party40Choice();
            PartyIdentification135 assgnrParty = new PartyIdentification135();
            Party38Choice assgnrChoice = new Party38Choice();
            OrganisationIdentification29 assgnrOrg = new OrganisationIdentification29();
            assgnrOrg.setAnyBIC(instructingBic);
            assgnrChoice.setOrgId(assgnrOrg);
            assgnrParty.setId(assgnrChoice);
            assgnr.setPty(assgnrParty);
            assgnmt.setAssgnr(assgnr);

            // Assignee (Receiver Bank BIC)
            Party40Choice assgne = new Party40Choice();
            PartyIdentification135 assgneParty = new PartyIdentification135();
            Party38Choice assgneChoice = new Party38Choice();
            OrganisationIdentification29 assgneOrg = new OrganisationIdentification29();
            assgneOrg.setAnyBIC(receiverBic);
            assgneChoice.setOrgId(assgneOrg);
            assgneParty.setId(assgneChoice);
            assgne.setPty(assgneParty);
            assgnmt.setAssgne(assgne);

            requestV03.setAssgnmt(assgnmt);

            // 2. Verification Item (vrfctn)
            IdentificationVerification4 vrfctn = new IdentificationVerification4();
            vrfctn.setId("FP");

            IdentificationInformation4 ptyAndAcctId = new IdentificationInformation4();
            CashAccount40 acct = new CashAccount40();
            AccountIdentification4Choice acctIdChoice = new AccountIdentification4Choice();

            if (request.identifierType() == AccountIdentifierType.IBAN) {
                acctIdChoice.setIBAN(request.accountIdentifier());
            } else {
                GenericAccountIdentification1 othr = new GenericAccountIdentification1();
                othr.setId(request.accountIdentifier());

                AccountSchemeName1Choice schemeChoice = new AccountSchemeName1Choice();
                schemeChoice.setPrtry(request.identifierType().name());
                othr.setSchmeNm(schemeChoice);

                acctIdChoice.setOthr(othr);
            }

            acct.setId(acctIdChoice);
            ptyAndAcctId.setAcct(acct);

            // Receiving Bank Agent
            BranchAndFinancialInstitutionIdentification6 agt = new BranchAndFinancialInstitutionIdentification6();
            FinancialInstitutionIdentification18 finInstnId = new FinancialInstitutionIdentification18();
            finInstnId.setBICFI(receiverBic);
            agt.setFinInstnId(finInstnId);
            ptyAndAcctId.setAgt(agt);

            vrfctn.setPtyAndAcctId(ptyAndAcctId);
            requestV03.addVrfctn(vrfctn);

            mx.setIdVrfctnReq(requestV03);

            // Return ONLY the inner <document:Document> XML string from Prowide
            return mx.document();

        } catch (Exception e) {
            log.error("Failed to generate Prowide ISO 20022 document XML: {}", e.getMessage(), e);
            throw new so.cb.adapter.shared.exception.InvalidIsoDocumentException("Prowide ISO 20022 document generation failed: " + e.getMessage(), e);
        }
    }
}
