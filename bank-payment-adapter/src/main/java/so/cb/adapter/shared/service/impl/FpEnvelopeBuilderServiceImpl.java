package so.cb.adapter.shared.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import so.cb.adapter.shared.service.FpEnvelopeBuilderService;

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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FpEnvelopeBuilderServiceImpl implements FpEnvelopeBuilderService {

    @Override
    public String wrapInFpEnvelope(String innerDocumentXml, String instructingBic, String receiverBic, String messageDefinition) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC));
        String bizMsgIdr = generate33CharBizMsgIdr(instructingBic);

        log.info("Wrapping inner ISO 20022 document ({}) in FPEnvelope for instructing BIC: {}, receiver BIC: {}",
                messageDefinition, instructingBic, receiverBic);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document innerDoc = db.parse(new ByteArrayInputStream(innerDocumentXml.getBytes(StandardCharsets.UTF_8)));

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
            msgDefIdrElem.setTextContent(messageDefinition);
            appHdr.appendChild(msgDefIdrElem);

            Element creDtElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "header:CreDt");
            creDtElem.setTextContent(timestamp);
            appHdr.appendChild(creDtElem);

            Element sgntrElem = envelopeDoc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "document:Sgntr");
            appHdr.appendChild(sgntrElem);

            fpEnvelope.appendChild(appHdr);

            // Import Prowide inner document under <document:Document>
            Node importedDocNode = envelopeDoc.importNode(innerDoc.getDocumentElement(), true);
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

        } catch (Exception e) {
            log.error("Failed to wrap inner document in FPEnvelope: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to build FPEnvelope XML: " + e.getMessage(), e);
        }
    }

    private String generate33CharBizMsgIdr(String instructingBic) {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(Instant.now().atZone(ZoneOffset.UTC));
        String randomPart = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 11).toUpperCase();
        return instructingBic + datePart + randomPart;
    }
}
