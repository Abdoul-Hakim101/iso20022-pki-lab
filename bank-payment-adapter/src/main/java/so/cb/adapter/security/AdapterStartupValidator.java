package so.cb.adapter.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import so.cb.adapter.config.AdapterProperties;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterStartupValidator implements CommandLineRunner {

    private final AdapterProperties adapterProperties;

    @Override
    public void run(String... args) {
        if (!adapterProperties.isStrictStartupCheck()) {
            log.warn("Adapter strict startup security check is DISABLED in configuration.");
            return;
        }

        log.info("Executing Bank Payment Adapter Cryptographic Startup Security Check...");

        Path certDir = Path.of(adapterProperties.getDir());
        Path caCertPath = certDir.resolve(adapterProperties.getCaCertFile());
        Path bankCertPath = certDir.resolve(adapterProperties.getBankCertFile());
        Path privateKeyPath = certDir.resolve(adapterProperties.getPrivateKeyFile());

        // 1. File Existence Check
        if (!Files.exists(caCertPath)) {
            log.error("CRITICAL STARTUP ERROR: Central Bank Root CA Certificate file missing at: {}", caCertPath.toAbsolutePath());
            throw new IllegalStateException("CRITICAL STARTUP SECURITY ERROR: Central Bank Root CA Certificate (chain.pem) missing at: " + caCertPath.toAbsolutePath());
        }
        if (!Files.exists(bankCertPath)) {
            log.error("CRITICAL STARTUP ERROR: Bank Leaf Certificate file missing at: {}", bankCertPath.toAbsolutePath());
            throw new IllegalStateException("CRITICAL STARTUP SECURITY ERROR: Bank Leaf Certificate (certificate.pem) missing at: " + bankCertPath.toAbsolutePath());
        }
        if (!Files.exists(privateKeyPath)) {
            log.error("CRITICAL STARTUP ERROR: Bank Private Key file missing at: {}", privateKeyPath.toAbsolutePath());
            throw new IllegalStateException("CRITICAL STARTUP SECURITY ERROR: Bank Private Key (private.pem) missing at: " + privateKeyPath.toAbsolutePath());
        }

        try {
            // 2. X.509 Parsing & Expiration Check
            X509Certificate caCert = parseCertificate(caCertPath);
            X509Certificate bankCert = parseCertificate(bankCertPath);

            caCert.checkValidity();
            bankCert.checkValidity();
            log.info("X.509 Certificate parsing & validity date check passed.");

            // 3. CA Trust Verification (bankCert signed by caCert)
            bankCert.verify(caCert.getPublicKey());
            log.info("CA Trust Verification passed: Bank certificate was issued and signed by Central Bank Root CA ({})", caCert.getSubjectX500Principal().getName());

            // 4. RSA Keypair Match Verification
            PrivateKey privateKey = parsePrivateKey(privateKeyPath);
            verifyKeypairMatch(privateKey, bankCert);
            log.info("RSA Keypair match verification passed: private.pem matches certificate.pem public key.");

            log.info("=========================================================================================");
            log.info("SUCCESS: Bank Payment Adapter Cryptographic Startup Security Check PASSED!");
            log.info("Bank Subject DN  : {}", bankCert.getSubjectX500Principal().getName());
            log.info("Root CA Issuer   : {}", caCert.getSubjectX500Principal().getName());
            log.info("Cert Serial Number: {}", bankCert.getSerialNumber().toString(16));
            log.info("Cert Valid From  : {}", bankCert.getNotBefore());
            log.info("Cert Valid To    : {}", bankCert.getNotAfter());
            log.info("=========================================================================================");

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("CRITICAL STARTUP ERROR: Cryptographic verification failed during adapter startup: {}", e.getMessage(), e);
            throw new IllegalStateException("CRITICAL STARTUP SECURITY ERROR: Failed cryptographic verification: " + e.getMessage(), e);
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

    private void verifyKeypairMatch(PrivateKey privateKey, X509Certificate cert) throws Exception {
        byte[] testPayload = "SPS-ADAPTER-STARTUP-VERIFICATION-PAYLOAD".getBytes(StandardCharsets.UTF_8);

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(testPayload);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(cert.getPublicKey());
        verifier.update(testPayload);

        if (!verifier.verify(signature)) {
            throw new IllegalStateException("CRITICAL STARTUP SECURITY ERROR: Bank private.pem does NOT match the public key in certificate.pem!");
        }
    }
}
