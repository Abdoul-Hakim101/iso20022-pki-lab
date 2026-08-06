package so.cb.adapter.acmt023.service.impl;

import com.prowidesoftware.swift.model.mx.MxAcmt02300103;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.enums.AccountIdentifierType;
import so.cb.adapter.acmt023.service.AccountVerificationService;
import so.cb.adapter.shared.config.AdapterProperties;
import so.cb.adapter.shared.security.XmlSignatureSigningService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountVerificationServiceImpl implements AccountVerificationService {

    private final AdapterProperties adapterProperties;
    private final XmlSignatureSigningService xmlSignatureSigningService;

    @Override
    public String createAndSignAcmt023Xml(AccountVerificationRequestJson request) {
        String instructingBic = adapterProperties.getBankBic();
        String receiverBic = request.receiverBic();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC));
        String bizMsgIdr = instructingBic + System.currentTimeMillis();
        String msgId = instructingBic + (System.currentTimeMillis() + 1);

        log.info("Creating ISO 20022 FPEnvelope acmt.023.001.03 using Prowide Engine for instructing BIC: {}, receiver BIC: {}, accountIdentifier: {}, type: {}",
                instructingBic, receiverBic, request.accountIdentifier(), request.identifierType());

        try {
            // 1. Build inner ISO 20022 acmt.023 payload using Prowide MxAcmt02300103 model
            MxAcmt02300103 mx = new MxAcmt02300103();
            IdentificationVerificationRequestV03 requestV03 = new IdentificationVerificationRequestV03();

            // Header Assignment (assgnmt)
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

            // Verification Item (vrfctn)
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

            // Generate Prowide Document XML
            String prowideDocXml = mx.document();
            log.debug("Generated Prowide Document XML: {}", prowideDocXml);

            // 2. Parse Prowide Document XML and assemble SPS FPEnvelope
            String fpEnvelopeXml = buildFpEnvelope(prowideDocXml, instructingBic, receiverBic, bizMsgIdr, timestamp);

            // 3. Digitally sign FPEnvelope using bank's private.pem and certificate.pem
            String signedXml = xmlSignatureSigningService.signXml(fpEnvelopeXml);

            log.info("Successfully generated and signed FPEnvelope ISO 20022 acmt.023.001.03 messageId: {}", msgId);
            return signedXml;

        } catch (Exception e) {
            log.error("Failed to build and sign FPEnvelope acmt.023.001.03 message using Prowide: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to generate FPEnvelope acmt.023.001.03 message: " + e.getMessage(), e);
        }
    }

    private String buildFpEnvelope(String prowideDocXml, String instructingBic, String receiverBic, String bizMsgIdr, String timestamp) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document prowideDoc = db.parse(new ByteArrayInputStream(prowideDocXml.getBytes(StandardCharsets.UTF_8)));

        Document envelopeDoc = db.newDocument();
        Element fpEnvelope = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:verification_request", "FPEnvelope");
        fpEnvelope.setAttribute("xmlns:header", "urn:iso:std:iso:20022:tech:xsd:head.001.001.03");
        fpEnvelope.setAttribute("xmlns:document", "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03");
        envelopeDoc.appendChild(fpEnvelope);

        // Build header:AppHdr
        Element appHdr = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:AppHdr");

        Element fr = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:Fr");
        Element frFiId = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:FIId");
        Element frFinInstnId = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:FinInstnId");
        Element frOthr = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:Othr");
        Element frId = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:Id");
        frId.setTextContent(instructingBic);
        frOthr.appendChild(frId);
        frFinInstnId.appendChild(frOthr);
        frFiId.appendChild(frFinInstnId);
        fr.appendChild(frFiId);
        appHdr.appendChild(fr);

        Element to = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:To");
        Element toFiId = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:FIId");
        Element toFinInstnId = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:FinInstnId");
        Element toOthr = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:Othr");
        Element toId = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:Id");
        toId.setTextContent(receiverBic);
        toOthr.appendChild(toId);
        toFinInstnId.appendChild(toOthr);
        toFiId.appendChild(toFinInstnId);
        to.appendChild(toFiId);
        appHdr.appendChild(to);

        Element bizMsgIdrElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:BizMsgIdr");
        bizMsgIdrElem.setTextContent(bizMsgIdr);
        appHdr.appendChild(bizMsgIdrElem);

        Element msgDefIdrElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:MsgDefIdr");
        msgDefIdrElem.setTextContent("acmt.023.001.03");
        appHdr.appendChild(msgDefIdrElem);

        Element creDtElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:CreDt");
        creDtElem.setTextContent(timestamp);
        appHdr.appendChild(creDtElem);

        Element sgntrElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "document:Sgntr");
        appHdr.appendChild(sgntrElem);

        fpEnvelope.appendChild(appHdr);

        // Import Prowide generated document into FPEnvelope under <document:Document>
        Node importedDocNode = envelopeDoc.importNode(prowideDoc.getDocumentElement(), true);

        // Standardize element tag to document:Document
        Element documentElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03", "document:Document");
        while (importedDocNode.hasChildNodes()) {
            documentElem.appendChild(importedDocNode.getFirstChild());
        }
        fpEnvelope.appendChild(documentElem);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        trans.transform(new DOMSource(envelopeDoc), new StreamResult(writer));
        return writer.toString();
    }
}
