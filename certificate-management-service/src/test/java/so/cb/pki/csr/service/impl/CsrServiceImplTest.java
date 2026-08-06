package so.cb.pki.csr.service.impl;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import so.cb.pki.csr.entity.Csr;
import so.cb.pki.csr.repository.CsrRepository;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.exception.ApiException;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsrServiceImplTest {

    @Mock
    private CsrRepository csrRepository;

    @Mock
    private InstitutionService institutionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CsrServiceImpl csrService;

    private String validCsrPem;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name subject = new X500Name("CN=Test Bank, O=Test Bank, C=SO");
        JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = builder.build(signer);

        StringWriter writer = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(writer)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()));
        }
        validCsrPem = writer.toString();
    }

    @Test
    void uploadCsr_Success() {
        String bic = "CBKSSOM1XXX";
        UUID instId = UUID.randomUUID();
        MockMultipartFile multipartFile = new MockMultipartFile("file", "csr.pem", "text/plain", validCsrPem.getBytes());

        when(institutionService.getActiveInstitutionIdByBic(bic)).thenReturn(instId);
        when(csrRepository.save(any(Csr.class))).thenAnswer(i -> {
            Csr c = i.getArgument(0);
            return c;
        });

        assertDoesNotThrow(() -> csrService.uploadCsr(multipartFile, bic));

        verify(institutionService).getActiveInstitutionIdByBic(bic);
        verify(csrRepository).save(any(Csr.class));
    }

    @Test
    void uploadCsr_EmptyFile_ThrowsApiException() {
        String bic = "CBKSSOM1XXX";
        MockMultipartFile multipartFile = new MockMultipartFile("file", "csr.pem", "text/plain", new byte[0]);

        ApiException ex = assertThrows(ApiException.class, () -> csrService.uploadCsr(multipartFile, bic));
        assertEquals("CSR file must not be empty", ex.getMessage());
    }
}
