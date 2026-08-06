package so.cb.pki.certificate.ca;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import so.cb.pki.certificate.config.CaProperties;
import so.cb.pki.shared.exception.ApiException;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaKeyProvider {

    private final CaProperties caProperties;
    private final Environment environment;

    @Getter
    private PrivateKey caPrivateKey;

    @Getter
    private X509Certificate caCertificate;

    @Getter
    private X509CertificateHolder caCertificateHolder;

    @Getter
    private String chainPem;

    @PostConstruct
    public void init() {
        try {
            Path caDirPath = Paths.get(caProperties.getDir());
            Path keyPath = caDirPath.resolve(caProperties.getPrivateKeyFile());
            Path certPath = caDirPath.resolve(caProperties.getCertificateFile());

            boolean keyExists = Files.exists(keyPath);
            boolean certExists = Files.exists(certPath);

            boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

            if (!keyExists || !certExists) {
                if (isProd) {
                    log.error("CRITICAL SECURITY FAILURE: Production profile 'prod' active but Root CA files missing in {}", caDirPath.toAbsolutePath());
                    throw new IllegalStateException("CRITICAL SECURITY ERROR: Production profile 'prod' is active but Root CA Keypair/Certificate is missing in: " + caDirPath.toAbsolutePath());
                } else {
                    log.warn("Root CA files missing in {}. Auto-generating DEV Root CA keypair...", caDirPath.toAbsolutePath());
                    generateAndSaveRootCa(caDirPath, keyPath, certPath);
                }
            }

            loadCaKeys(keyPath, certPath);
            log.info("Central Bank Root CA successfully loaded. Issuer DN: {}", caCertificate.getSubjectX500Principal().getName());

        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            log.error("Failed to initialize Central Bank Root CA: {}", e.getMessage(), e);
            throw new ApiException("Failed to initialize Central Bank Root CA: " + e.getMessage());
        }
    }

    private void generateAndSaveRootCa(Path caDirPath, Path keyPath, Path certPath) throws Exception {
        Files.createDirectories(caDirPath);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name issuer = new X500Name(caProperties.getIssuerDn());
        BigInteger serialNumber = new BigInteger(64, new SecureRandom()).abs();

        Instant notBefore = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant notAfter = Instant.now().plus(caProperties.getValidityYears() * 365L, ChronoUnit.DAYS);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                Date.from(notBefore),
                Date.from(notAfter),
                issuer,
                keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = certBuilder.build(signer);

        // Save Private Key PEM
        try (StringWriter sw = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            pemWriter.writeObject(keyPair.getPrivate());
            pemWriter.flush();
            Files.writeString(keyPath, sw.toString());
        }

        // Save Certificate PEM
        try (StringWriter sw = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            pemWriter.writeObject(holder);
            pemWriter.flush();
            Files.writeString(certPath, sw.toString());
        }

        log.info("Successfully generated and saved DEV Root CA to {}", caDirPath.toAbsolutePath());
    }

    private void loadCaKeys(Path keyPath, Path certPath) throws Exception {
        // Load Private Key
        String keyContent = Files.readString(keyPath);
        try (PEMParser parser = new PEMParser(new StringReader(keyContent))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (obj instanceof org.bouncycastle.openssl.PEMKeyPair keyPair) {
                this.caPrivateKey = converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo keyInfo) {
                this.caPrivateKey = converter.getPrivateKey(keyInfo);
            } else {
                throw new IllegalArgumentException("Unsupported CA private key format in " + keyPath);
            }
        }

        // Load Certificate
        this.chainPem = Files.readString(certPath);
        try (PEMParser parser = new PEMParser(new StringReader(chainPem))) {
            Object obj = parser.readObject();
            if (obj instanceof X509CertificateHolder holder) {
                this.caCertificateHolder = holder;
                this.caCertificate = new JcaX509CertificateConverter().getCertificate(holder);
            } else {
                throw new IllegalArgumentException("Unsupported CA certificate format in " + certPath);
            }
        }
    }
}
