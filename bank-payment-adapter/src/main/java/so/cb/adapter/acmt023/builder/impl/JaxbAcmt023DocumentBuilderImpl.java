package so.cb.adapter.acmt023.builder.impl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import so.cb.adapter.acmt023.builder.JaxbAcmt023DocumentBuilder;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.enums.AccountIdentifierType;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class JaxbAcmt023DocumentBuilderImpl implements JaxbAcmt023DocumentBuilder {

    @Override
    public String buildDocumentXml(String instructingBic, AccountVerificationRequestJson request) {
        String timestampPattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
        String msgId = instructingBic + timestampPattern + randomSuffix;
        String receiverBic = request.receiverBic();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC));

        log.info("Building JAXB document:Document XML for acmt.023.001.03 (instructingBic: {}, receiverBic: {})",
                instructingBic, receiverBic);

        try {
            JaxbDocument doc = new JaxbDocument();
            JaxbIdVrfctnReq req = new JaxbIdVrfctnReq();

            // 1. Assignment
            JaxbAssgnmt assgnmt = new JaxbAssgnmt();
            assgnmt.setMsgId(msgId);
            assgnmt.setCreDtTm(timestamp);
            assgnmt.setAssgnr(new JaxbParty(instructingBic));
            assgnmt.setAssgne(new JaxbParty(receiverBic));
            req.setAssgnmt(assgnmt);

            // 2. Verification Item
            JaxbVrfctn vrfctn = new JaxbVrfctn();
            vrfctn.setId("FP");

            JaxbAcct acct = new JaxbAcct();
            JaxbAcctId acctId = new JaxbAcctId();

            if (request.identifierType() == AccountIdentifierType.IBAN) {
                acctId.setIban(request.accountIdentifier());
            } else {
                JaxbOthr othr = new JaxbOthr();
                othr.setId(request.accountIdentifier());
                othr.setSchmeNm(new JaxbSchmeNm(request.identifierType().name()));
                acctId.setOthr(othr);
            }

            acct.setId(acctId);
            vrfctn.setPtyAndAcctId(new JaxbPtyAndAcctId(acct));
            req.setVrfctn(vrfctn);

            doc.setIdVrfctnReq(req);

            JAXBContext context = JAXBContext.newInstance(JaxbDocument.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(doc, writer);

            return writer.toString();

        } catch (Exception e) {
            log.error("Failed to generate JAXB ISO 20022 document XML: {}", e.getMessage(), e);
            throw new IllegalStateException("JAXB ISO 20022 document generation failed: " + e.getMessage(), e);
        }
    }

    @Data
    @XmlRootElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbDocument {
        @XmlElement(name = "IdVrfctnReq", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbIdVrfctnReq idVrfctnReq;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbIdVrfctnReq {
        @XmlElement(name = "Assgnmt", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbAssgnmt assgnmt;

        @XmlElement(name = "Vrfctn", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbVrfctn vrfctn;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbAssgnmt {
        @XmlElement(name = "MsgId", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String msgId;

        @XmlElement(name = "CreDtTm", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String creDtTm;

        @XmlElement(name = "Assgnr", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbParty assgnr;

        @XmlElement(name = "Assgne", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbParty assgne;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbParty {
        @XmlElement(name = "Agt", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbAgt agt;

        public JaxbParty() {}

        public JaxbParty(String bic) {
            this.agt = new JaxbAgt(bic);
        }
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbAgt {
        @XmlElement(name = "FinInstnId", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbFinInstnId finInstnId;

        public JaxbAgt() {}

        public JaxbAgt(String bic) {
            this.finInstnId = new JaxbFinInstnId(bic);
        }
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbFinInstnId {
        @XmlElement(name = "Othr", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbOthrBic othr;

        public JaxbFinInstnId() {}

        public JaxbFinInstnId(String bic) {
            this.othr = new JaxbOthrBic(bic);
        }
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbOthrBic {
        @XmlElement(name = "Id", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String id;

        public JaxbOthrBic() {}

        public JaxbOthrBic(String bic) {
            this.id = bic;
        }
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbVrfctn {
        @XmlElement(name = "Id", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String id;

        @XmlElement(name = "PtyAndAcctId", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbPtyAndAcctId ptyAndAcctId;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbPtyAndAcctId {
        @XmlElement(name = "Acct", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbAcct acct;

        public JaxbPtyAndAcctId() {}

        public JaxbPtyAndAcctId(JaxbAcct acct) {
            this.acct = acct;
        }
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbAcct {
        @XmlElement(name = "Id", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbAcctId id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbAcctId {
        @XmlElement(name = "IBAN", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String iban;

        @XmlElement(name = "Othr", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbOthr othr;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbOthr {
        @XmlElement(name = "Id", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String id;

        @XmlElement(name = "SchmeNm", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private JaxbSchmeNm schmeNm;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class JaxbSchmeNm {
        @XmlElement(name = "Prtry", namespace = "urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03")
        private String prtry;

        public JaxbSchmeNm() {}

        public JaxbSchmeNm(String prtry) {
            this.prtry = prtry;
        }
    }
}
