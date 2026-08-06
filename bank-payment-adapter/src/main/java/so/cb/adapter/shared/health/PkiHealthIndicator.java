package so.cb.adapter.shared.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import so.cb.adapter.shared.config.AdapterProperties;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

@Slf4j
@Component("pki")
@RequiredArgsConstructor
public class PkiHealthIndicator implements HealthIndicator {

    private final AdapterProperties adapterProperties;

    @Override
    public Health health() {
        Path certDir = Path.of(adapterProperties.getDir());
        Path caCertPath = certDir.resolve(adapterProperties.getCaCertFile());
        Path bankCertPath = certDir.resolve(adapterProperties.getBankCertFile());

        if (!Files.exists(caCertPath)) {
            return Health.down()
                    .withDetail("status", "FAILED")
                    .withDetail("reason", "Central Bank Root CA Certificate (chain.pem) missing at: " + caCertPath.toAbsolutePath())
                    .build();
        }

        if (!Files.exists(bankCertPath)) {
            return Health.down()
                    .withDetail("status", "FAILED")
                    .withDetail("reason", "Bank Leaf Certificate (certificate.pem) missing at: " + bankCertPath.toAbsolutePath())
                    .build();
        }

        try {
            X509Certificate caCert = parseCertificate(caCertPath);
            X509Certificate bankCert = parseCertificate(bankCertPath);

            caCert.checkValidity();
            bankCert.checkValidity();

            bankCert.verify(caCert.getPublicKey());

            String bankDn = bankCert.getSubjectX500Principal().getName();
            String bankBic = extractCnFromDn(bankDn);

            return Health.up()
                    .withDetail("caTrustAnchor", "LOADED")
                    .withDetail("caIssuer", caCert.getSubjectX500Principal().getName())
                    .withDetail("bankBic", bankBic)
                    .withDetail("bankSubjectDn", bankDn)
                    .withDetail("serialNumber", bankCert.getSerialNumber().toString(16))
                    .withDetail("validFrom", bankCert.getNotBefore().toInstant().toString())
                    .withDetail("validTo", bankCert.getNotAfter().toInstant().toString())
                    .build();

        } catch (Exception e) {
            log.warn("PKI Health Indicator check failed: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("status", "FAILED")
                    .withDetail("reason", e.getMessage())
                    .build();
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

    private String extractCnFromDn(String dn) {
        if (dn == null) return "UNKNOWN";
        for (String part : dn.split(",")) {
            part = part.trim();
            if (part.startsWith("CN=") || part.startsWith("cn=")) {
                return part.substring(3);
            }
        }
        return dn;
    }
}
