package so.cb.adapter.shared.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import so.cb.adapter.shared.config.AdapterProperties;
import so.cb.adapter.shared.exception.XmlSignatureException;
import so.cb.adapter.shared.service.XmlSignatureService;

import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class XmlSignatureServiceImpl implements XmlSignatureService {

    private static final String EXCLUSIVE_C14N = "http://www.w3.org/2001/10/xml-exc-c14n#";
    private static final String XADES_TYPE = "http://uri.etsi.org/01903/v1.3.2#SignedProperties";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    private final AdapterProperties adapterProperties;

    @Override
    public String signXml(String rawXml) {
        log.info("Initiating W3C XAdES-BES XML Digital Signature for ISO 20022 document...");
        try {
            Path certDir = Path.of(adapterProperties.getDir());
            Path privateKeyPath = certDir.resolve(adapterProperties.getPrivateKeyFile());
            Path bankCertPath = certDir.resolve(adapterProperties.getBankCertFile());
            String passphrase = adapterProperties.getPrivateKeyPassphrase();

            PrivateKey privateKey = parsePrivateKey(privateKeyPath, passphrase);
            X509Certificate certificate = parseCertificate(bankCertPath);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            DigestMethod sha256Digest = fac.newDigestMethod(DigestMethod.SHA256, null);
            Transform excC14nTransform = fac.newTransform(EXCLUSIVE_C14N, (TransformParameterSpec) null);

            String uuidBase = UUID.randomUUID().toString();
            String keyInfoId = "_" + uuidBase;
            String signedPropsId = "_" + UUID.randomUUID().toString() + "-signedprops";

            // 1. Reference 1: KeyInfo Reference (#keyInfoId)
            Reference refKeyInfo = fac.newReference(
                    "#" + keyInfoId,
                    sha256Digest,
                    Collections.singletonList(excC14nTransform),
                    null,
                    null
            );

            // 2. Reference 2: XAdES SignedProperties Reference (#signedPropsId)
            Reference refSignedProps = fac.newReference(
                    "#" + signedPropsId,
                    sha256Digest,
                    Collections.singletonList(excC14nTransform),
                    XADES_TYPE,
                    null
            );

            // 3. Reference 3: Anonymous Document Payload Reference (URI = null, targeting document:Document)
            Reference refPayload = fac.newReference(
                    null,
                    sha256Digest,
                    Collections.singletonList(excC14nTransform),
                    null,
                    null
            );

            List<Reference> references = new ArrayList<>();
            references.add(refKeyInfo);
            references.add(refSignedProps);
            references.add(refPayload);

            CanonicalizationMethod c14nMethod = fac.newCanonicalizationMethod(EXCLUSIVE_C14N, (C14NMethodParameterSpec) null);
            SignatureMethod signatureMethod = fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null);

            SignedInfo signedInfo = fac.newSignedInfo(c14nMethod, signatureMethod, references);

            // Build KeyInfo element with <ds:X509Data><ds:X509IssuerSerial>
            Element keyInfoElem = doc.createElementNS(DS_NS, "ds:KeyInfo");
            keyInfoElem.setAttribute("Id", keyInfoId);

            Element x509DataElem = doc.createElementNS(DS_NS, "ds:X509Data");
            Element x509IssuerSerialElem = doc.createElementNS(DS_NS, "ds:X509IssuerSerial");
            Element x509IssuerNameElem = doc.createElementNS(DS_NS, "ds:X509IssuerName");
            x509IssuerNameElem.setTextContent(certificate.getIssuerX500Principal().getName());
            Element x509SerialNumberElem = doc.createElementNS(DS_NS, "ds:X509SerialNumber");
            x509SerialNumberElem.setTextContent(certificate.getSerialNumber().toString());

            x509IssuerSerialElem.appendChild(x509IssuerNameElem);
            x509IssuerSerialElem.appendChild(x509SerialNumberElem);
            x509DataElem.appendChild(x509IssuerSerialElem);
            keyInfoElem.appendChild(x509DataElem);

            KeyInfoFactory kif = fac.getKeyInfoFactory();
            KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(new DOMStructure(keyInfoElem.getFirstChild())), keyInfoId);

            // Build XAdES Object containing <xades:QualifyingProperties>
            String xadesNs = adapterProperties.getSignature().getXadesNamespace();
            Element qualifyingProps = doc.createElementNS(xadesNs, "xades:QualifyingProperties");
            qualifyingProps.setAttribute("xmlns:xades", xadesNs);

            Element signedProps = doc.createElementNS(xadesNs, "xades:SignedProperties");
            signedProps.setAttribute("Id", signedPropsId);

            Element signedSigProps = doc.createElementNS(xadesNs, "xades:SignedSignatureProperties");
            Element signingTime = doc.createElementNS(xadesNs, "xades:SigningTime");
            signingTime.setTextContent(DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC)));

            signedSigProps.appendChild(signingTime);
            signedProps.appendChild(signedSigProps);
            qualifyingProps.appendChild(signedProps);

            XMLObject xadesObject = fac.newXMLObject(Collections.singletonList(new DOMStructure(qualifyingProps)), null, null, null);

            // Locate or create <document:Sgntr xmlns:document="urn:iso:std:iso:20022:tech:xsd:head.001.001.03"> inside <header:AppHdr>
            Element sgntrElem = null;
            NodeList appHdrList = doc.getElementsByTagNameNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "AppHdr");
            if (appHdrList.getLength() > 0) {
                Element appHdr = (Element) appHdrList.item(0);
                NodeList existingSgntr = appHdr.getElementsByTagNameNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "Sgntr");
                if (existingSgntr.getLength() > 0) {
                    sgntrElem = (Element) existingSgntr.item(0);
                } else {
                    sgntrElem = doc.createElementNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "document:Sgntr");
                    appHdr.appendChild(sgntrElem);
                }
            } else {
                sgntrElem = doc.getDocumentElement();
            }

            // Target for anonymous 3rd reference is <document:Document>
            NodeList documentNodeList = doc.getElementsByTagNameNS("urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03", "Document");
            Node targetPayloadNode = (documentNodeList.getLength() > 0) ? documentNodeList.item(0) : doc.getDocumentElement();

            DOMSignContext dsc = new DOMSignContext(privateKey, sgntrElem);
            dsc.putNamespacePrefix(DS_NS, "ds");
            dsc.setIdAttributeNS(signedProps, null, "Id");
            dsc.setIdAttributeNS(keyInfoElem, null, "Id");

            // Dereference URI=null to targetPayloadNode (<document:Document>)
            URIDereferencer defaultDereferencer = XMLSignatureFactory.getInstance("DOM").getURIDereferencer();
            dsc.setURIDereferencer((uriRef, context) -> {
                if (uriRef.getURI() == null || uriRef.getURI().isEmpty()) {
                    return (NodeSetData<Node>) () -> Collections.singleton(targetPayloadNode).iterator();
                }
                return defaultDereferencer.dereference(uriRef, context);
            });

            XMLSignature signature = fac.newXMLSignature(signedInfo, keyInfo, Collections.singletonList(xadesObject), null, null);
            signature.sign(dsc);

            // Clean SignatureValue to single continuous Base64 line without line breaks
            NodeList sigValList = doc.getElementsByTagNameNS(DS_NS, "SignatureValue");
            if (sigValList.getLength() > 0) {
                Node sigValNode = sigValList.item(0);
                sigValNode.setTextContent(sigValNode.getTextContent().replaceAll("\\s+", ""));
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            trans.transform(new DOMSource(doc), new StreamResult(writer));

            log.info("W3C XAdES-BES XML Digital Signature successfully generated.");
            return writer.toString();

        } catch (Exception e) {
            log.error("Failed to generate W3C XAdES-BES XML Digital Signature: {}", e.getMessage(), e);
            throw new XmlSignatureException("Failed to sign ISO 20022 XML document: " + e.getMessage(), e);
        }
    }

    private PrivateKey parsePrivateKey(Path path, String passphrase) throws Exception {
        String pemContent = Files.readString(path);
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof PKCS8EncryptedPrivateKeyInfo encryptedInfo) {
                char[] password = (passphrase != null && !passphrase.isBlank()) ? passphrase.toCharArray() : new char[0];
                InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
                PrivateKeyInfo keyInfo = encryptedInfo.decryptPrivateKeyInfo(provider);
                return converter.getPrivateKey(keyInfo);
            } else if (object instanceof PEMEncryptedKeyPair encryptedKeyPair) {
                char[] password = (passphrase != null && !passphrase.isBlank()) ? passphrase.toCharArray() : new char[0];
                PEMKeyPair keyPair = encryptedKeyPair.decryptKeyPair(new JcePEMDecryptorProviderBuilder().build(password));
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (object instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo keyInfo) {
                return converter.getPrivateKey(keyInfo);
            }
            throw new IllegalArgumentException("File at " + path.toAbsolutePath() + " is not a valid RSA Private Key (private.pem)");
        }
    }

    private X509Certificate parseCertificate(Path path) throws Exception {
        String pemContent = Files.readString(path);
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            if (object instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter().getCertificate(holder);
            }
            throw new IllegalArgumentException("File at " + path.toAbsolutePath() + " is not a valid X.509 Certificate");
        }
    }
}
