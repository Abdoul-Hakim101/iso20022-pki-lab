package so.cb.adapter.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import so.cb.adapter.shared.config.AdapterProperties;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
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
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class XmlSignatureSigningServiceImpl implements XmlSignatureSigningService {

    private final AdapterProperties adapterProperties;

    @Override
    public String signXml(String rawXml) {
        log.info("Initiating W3C XML Digital Signature for ISO 20022 FPEnvelope document...");
        try {
            Path certDir = Path.of(adapterProperties.getDir());
            Path privateKeyPath = certDir.resolve(adapterProperties.getPrivateKeyFile());
            Path bankCertPath = certDir.resolve(adapterProperties.getBankCertFile());

            PrivateKey privateKey = parsePrivateKey(privateKeyPath);
            X509Certificate certificate = parseCertificate(bankCertPath);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            Reference ref = fac.newReference(
                    "",
                    fac.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null
            );

            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    Collections.singletonList(ref)
            );

            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data x509Data = kif.newX509Data(Collections.singletonList(certificate));
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

            Node parentNode = doc.getDocumentElement();
            NodeList sgntrList = doc.getElementsByTagNameNS("urn:iso:std:iso:20022:tech:xsd:head.001.001.03", "Sgntr");
            if (sgntrList.getLength() > 0) {
                parentNode = sgntrList.item(0);
            } else {
                NodeList fallbackSgntr = doc.getElementsByTagName("document:Sgntr");
                if (fallbackSgntr.getLength() > 0) {
                    parentNode = fallbackSgntr.item(0);
                }
            }

            DOMSignContext dsc = new DOMSignContext(privateKey, parentNode);

            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            trans.transform(new DOMSource(doc), new StreamResult(writer));

            log.info("W3C XML Digital Signature successfully generated in FPEnvelope.");
            return writer.toString();

        } catch (Exception e) {
            log.error("Failed to generate W3C XML Digital Signature: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to sign ISO 20022 XML document: " + e.getMessage(), e);
        }
    }

    private PrivateKey parsePrivateKey(Path path) throws Exception {
        String pemContent = Files.readString(path);
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair keyPair) {
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
