package so.cb.pki.certificate.service;

import so.cb.pki.certificate.dto.CertificateResponse;
import so.cb.pki.certificate.dto.CertificateSummary;
import so.cb.pki.certificate.dto.RevokeCertificateRequest;
import so.cb.pki.certificate.enums.CertificateStatus;
import so.cb.pki.shared.dto.PaginatedResponse;

import java.util.UUID;

public interface CertificateService {

    CertificateResponse issueCertificate(UUID csrId, UUID institutionId, String bic, String csrPem);

    CertificateResponse getCertificateBySerialNumber(String serialNumber);

    PaginatedResponse<CertificateSummary> getCertificates(CertificateStatus status, String search, int pageNumber, int pageSize);

    CertificateResponse revokeCertificate(String serialNumber, RevokeCertificateRequest request);

    String getCaChainPem();

    String getCertificatePem(String serialNumber);

    String getCertificateFullChainPem(String serialNumber);
}
