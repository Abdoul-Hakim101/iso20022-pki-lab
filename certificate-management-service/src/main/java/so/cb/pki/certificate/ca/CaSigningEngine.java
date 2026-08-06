package so.cb.pki.certificate.ca;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Component;
import so.cb.pki.shared.exception.ApiException;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaSigningEngine {

    private final CaKeyProvider caKeyProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public record IssuedCertificateResult(
            String serialNumber,
            String certificatePem,
            Instant validFrom,
            Instant validTo
    ) {
    }

    public IssuedCertificateResult issueCertificate(String csrPem) {
        try {
            log.info("Parsing CSR for certificate issuance...");
            PKCS10CertificationRequest csr = parseCsrPem(csrPem);

            BigInteger serialNumber = new BigInteger(64, secureRandom).abs();
            String serialNumberStr = serialNumber.toString();

            Instant validFrom = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant validTo = Instant.now().plus(365, ChronoUnit.DAYS);

            X500Name issuer = caKeyProvider.getCaCertificateHolder().getSubject();
            X500Name subject = csr.getSubject();

            log.info("Signing X.509 Certificate. Subject: '{}', SerialNumber: '{}'", subject, serialNumberStr);

            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuer,
                    serialNumber,
                    Date.from(validFrom),
                    Date.from(validTo),
                    subject,
                    csr.getSubjectPublicKeyInfo()
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(caKeyProvider.getCaPrivateKey());

            X509CertificateHolder certificateHolder = certBuilder.build(signer);

            String certificatePem = exportToPem(certificateHolder);
            log.info("Successfully issued X.509 Certificate with SerialNumber: '{}'", serialNumberStr);

            return new IssuedCertificateResult(
                    serialNumberStr,
                    certificatePem,
                    validFrom,
                    validTo
            );

        } catch (Exception e) {
            log.error("Failed to issue X.509 Certificate from CSR: {}", e.getMessage(), e);
            throw new ApiException("Failed to sign CSR and issue X.509 Certificate: " + e.getMessage());
        }
    }

    private PKCS10CertificationRequest parseCsrPem(String csrPem) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(csrPem))) {
            Object object = pemParser.readObject();
            if (object instanceof PKCS10CertificationRequest csr) {
                return csr;
            }
            throw new IllegalArgumentException("Provided CSR text is not a valid PKCS#10 Certification Request");
        }
    }

    private String exportToPem(X509CertificateHolder holder) throws Exception {
        try (StringWriter sw = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            pemWriter.writeObject(holder);
            pemWriter.flush();
            return sw.toString();
        }
    }
}
