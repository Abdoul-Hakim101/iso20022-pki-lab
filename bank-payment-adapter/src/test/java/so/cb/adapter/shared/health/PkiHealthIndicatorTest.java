package so.cb.adapter.shared.health;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import so.cb.adapter.shared.config.AdapterProperties;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class PkiHealthIndicatorTest {

    @TempDir
    Path tempDir;

    private AdapterProperties properties;
    private PkiHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new AdapterProperties();
        properties.setDir(tempDir.toString());
        properties.setCaCertFile("chain.pem");
        properties.setBankCertFile("certificate.pem");
        properties.setPrivateKeyFile("private.pem");

        healthIndicator = new PkiHealthIndicator(properties);
    }

    @Test
    void testHealth_Success() throws Exception {
        KeyPair caKeyPair = generateKeyPair();
        X509Certificate caCert = generateCertificate("CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair, "CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair);

        KeyPair bankKeyPair = generateKeyPair();
        X509Certificate bankCert = generateCertificate("CN=PMRBSOMM, O=Premier Bank, C=SO", bankKeyPair, "CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair);

        writePem(tempDir.resolve("chain.pem"), caCert);
        writePem(tempDir.resolve("certificate.pem"), bankCert);

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("LOADED", health.getDetails().get("caTrustAnchor"));
        assertEquals("PMRBSOMM", health.getDetails().get("bankBic"));
    }

    @Test
    void testHealth_MissingCert_ReturnsHealthDown() throws Exception {
        KeyPair caKeyPair = generateKeyPair();
        X509Certificate caCert = generateCertificate("CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair, "CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair);

        writePem(tempDir.resolve("chain.pem"), caCert);

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("reason").toString().contains("missing at"));
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private X509Certificate generateCertificate(String subjectDn, KeyPair subjectKeyPair, String issuerDn, KeyPair issuerKeyPair) throws Exception {
        Instant now = Instant.now();
        Date notBefore = Date.from(now.minus(1, ChronoUnit.DAYS));
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name(issuerDn),
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                new X500Name(subjectDn),
                subjectKeyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKeyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private void writePem(Path path, Object object) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(object);
        }
        Files.writeString(path, stringWriter.toString());
    }
}
