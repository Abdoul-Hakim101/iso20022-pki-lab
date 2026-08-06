package so.cb.pki.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import so.cb.pki.certificate.dto.CertificateResponse;
import so.cb.pki.certificate.service.CertificateService;
import so.cb.pki.notification.service.PkiNotificationService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PkiNotificationServiceImpl implements PkiNotificationService {

    private final CertificateService certificateService;

    @Override
    public void handleApprovedCsr(UUID csrId, UUID institutionId, String bic, String csrPem) {
        log.info("Handling approved CSR notification for bank BIC: {}, CSR ID: {}", bic, csrId);

        CertificateResponse response = certificateService.issueCertificate(csrId, institutionId, bic, csrPem);

        log.info("Successfully issued X.509 Certificate via notification pipeline. Certificate ID: {}, SerialNumber: {}, BIC: {}",
                response.id(), response.serialNumber(), response.bic());
    }
}
