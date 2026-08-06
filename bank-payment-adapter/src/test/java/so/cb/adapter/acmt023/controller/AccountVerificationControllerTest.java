package so.cb.adapter.acmt023.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AccountVerificationControllerTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws Exception {
        generateTestCertificates(tempDir);
        registry.add("adapter.certs.dir", tempDir::toString);
        registry.add("adapter.certs.ca-cert-file", () -> "chain.pem");
        registry.add("adapter.certs.bank-cert-file", () -> "certificate.pem");
        registry.add("adapter.certs.private-key-file", () -> "private.pem");
        registry.add("adapter.certs.strict-startup-check", () -> "true");
        registry.add("adapter.bank-bic", () -> "PMRBSOMM");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testSignAcmt023Request_Account_Success() throws Exception {
        String jsonPayload = """
                {
                  "receiverBic": "IBSBSOMM",
                  "accountIdentifier": "4005006007",
                  "identifierType": "ACCT"
                }
                """;

        mockMvc.perform(post("/api/v1/adapter/acmt023/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<FPEnvelope")))
                .andExpect(content().string(containsString("PMRBSOMM")))
                .andExpect(content().string(containsString("IBSBSOMM")))
                .andExpect(content().string(containsString("4005006007")))
                .andExpect(content().string(containsString("ACCT")))
                .andExpect(content().string(containsString("Signature")));
    }

    @Test
    void testSignAcmt023Request_Phone_Success() throws Exception {
        String jsonPayload = """
                {
                  "receiverBic": "IBSBSOMM",
                  "accountIdentifier": "+252615000000",
                  "identifierType": "MSIS"
                }
                """;

        mockMvc.perform(post("/api/v1/adapter/acmt023/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<FPEnvelope")))
                .andExpect(content().string(containsString("+252615000000")))
                .andExpect(content().string(containsString("MSIS")))
                .andExpect(content().string(containsString("Signature")));
    }

    @Test
    void testSignAcmt023Request_Wallet_Success() throws Exception {
        String jsonPayload = """
                {
                  "receiverBic": "IBSBSOMM",
                  "accountIdentifier": "EWLT-998877",
                  "identifierType": "EWLT"
                }
                """;

        mockMvc.perform(post("/api/v1/adapter/acmt023/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<FPEnvelope")))
                .andExpect(content().string(containsString("EWLT-998877")))
                .andExpect(content().string(containsString("EWLT")))
                .andExpect(content().string(containsString("Signature")));
    }

    @Test
    void testSignAcmt023Request_IBAN_Success() throws Exception {
        String jsonPayload = """
                {
                  "receiverBic": "IBSBSOMM",
                  "accountIdentifier": "SO82PMRB0000004005006007",
                  "identifierType": "IBAN"
                }
                """;

        mockMvc.perform(post("/api/v1/adapter/acmt023/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<FPEnvelope")))
                .andExpect(content().string(containsString("SO82PMRB0000004005006007")))
                .andExpect(content().string(containsString("IBAN")))
                .andExpect(content().string(containsString("Signature")));
    }

    private static void generateTestCertificates(Path dir) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair caKeyPair = generator.generateKeyPair();
        KeyPair bankKeyPair = generator.generateKeyPair();

        X509Certificate caCert = generateCertificate("CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair, "CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair);
        X509Certificate bankCert = generateCertificate("CN=PMRBSOMM, O=Premier Bank, C=SO", bankKeyPair, "CN=Central Bank CA, O=Central Bank, C=SO", caKeyPair);

        writePem(dir.resolve("chain.pem"), caCert);
        writePem(dir.resolve("certificate.pem"), bankCert);
        writePem(dir.resolve("private.pem"), bankKeyPair.getPrivate());
    }

    private static X509Certificate generateCertificate(String subjectDn, KeyPair subjectKeyPair, String issuerDn, KeyPair issuerKeyPair) throws Exception {
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

    private static void writePem(Path path, Object object) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(object);
        }
        Files.writeString(path, stringWriter.toString());
    }
}
