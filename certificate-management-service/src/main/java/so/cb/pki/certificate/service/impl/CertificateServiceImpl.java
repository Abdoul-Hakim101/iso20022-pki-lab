package so.cb.pki.certificate.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import so.cb.pki.certificate.dto.CertificateSummary;
import so.cb.pki.certificate.service.CertificateService;
import so.cb.pki.certificate.ca.CaKeyProvider;
import so.cb.pki.certificate.dto.CertificateResponse;
import so.cb.pki.certificate.dto.RevokeCertificateRequest;
import so.cb.pki.certificate.entity.Certificate;
import so.cb.pki.certificate.enums.CertificateStatus;
import so.cb.pki.certificate.mapper.CertificateMapper;
import so.cb.pki.certificate.repository.CertificateRepository;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.exception.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import so.cb.pki.certificate.ca.CaSigningEngine;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CaKeyProvider caKeyProvider;
    private final CaSigningEngine caSigningEngine;
    private final InstitutionService institutionService;

    @Override
    public CertificateResponse issueCertificate(UUID csrId, UUID institutionId, String bic, String csrPem) {
        log.info("Issuing certificate for CSR ID: {}, Institution ID: {}, BIC: {}", csrId, institutionId, bic);

        return certificateRepository.findByCsrId(csrId)
                .map(CertificateMapper::toResponse)
                .orElseGet(() -> {
                    CaSigningEngine.IssuedCertificateResult result = caSigningEngine.issueCertificate(csrPem);

                    Certificate entity = Certificate.builder()
                            .id(UUID.randomUUID())
                            .institutionId(institutionId)
                            .csrId(csrId)
                            .bic(bic)
                            .serialNumber(result.serialNumber())
                            .certificatePem(result.certificatePem())
                            .status(CertificateStatus.ACTIVE)
                            .validFrom(result.validFrom())
                            .validTo(result.validTo())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    entity = certificateRepository.save(entity);
                    log.info("Successfully issued and saved Certificate ID: {}, SerialNumber: {}, BIC: {}",
                            entity.getId(), entity.getSerialNumber(), entity.getBic());
                    return CertificateMapper.toResponse(entity);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse getCertificateBySerialNumber(String serialNumber) {
        log.debug("Fetching certificate by SerialNumber: {}", serialNumber);
        Certificate entity = getBySerialNumber(serialNumber);
        return CertificateMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CertificateSummary> getCertificates(CertificateStatus status, String search, int pageNumber, int pageSize) {
        log.debug("Searching certificate records (status: {}, search: '{}', page: {}, size: {})", status, search, pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Certificate> page = certificateRepository.search(status, search, pageable);

        List<CertificateSummary> items = page.getContent().stream()
                .map(entity -> {
                    String institutionName = institutionService.getInstitutionNameById(entity.getInstitutionId());
                    return CertificateMapper.toSummary(entity, institutionName);
                })
                .toList();

        return new PaginatedResponse<>(
                items,
                (int) page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    @Override
    public CertificateResponse revokeCertificate(String serialNumber, RevokeCertificateRequest request) {
        log.info("Processing revocation for certificate SerialNumber: {}, Reason: '{}'", serialNumber, request.reason());
        Certificate entity = getBySerialNumber(serialNumber);

        if (entity.getStatus() == CertificateStatus.REVOKED) {
            log.warn("Certificate SerialNumber: {} is already REVOKED", serialNumber);
            throw new ApiException("Certificate with SerialNumber '" + serialNumber + "' is already REVOKED");
        }

        entity.setStatus(CertificateStatus.REVOKED);
        entity.setRevocationReason(request.reason());
        entity.setRevokedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        entity = certificateRepository.save(entity);
        log.info("Successfully revoked certificate SerialNumber: {}", serialNumber);

        return CertificateMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCaChainPem() {
        log.debug("Fetching Root CA trust chain PEM");
        return caKeyProvider.getChainPem();
    }

    @Override
    @Transactional(readOnly = true)
    public String getCertificatePem(String serialNumber) {
        log.debug("Fetching leaf certificate PEM text for SerialNumber: {}", serialNumber);
        Certificate entity = getBySerialNumber(serialNumber);
        return entity.getCertificatePem();
    }

    @Override
    @Transactional(readOnly = true)
    public String getCertificateFullChainPem(String serialNumber) {
        log.debug("Building full chain PEM bundle for certificate SerialNumber: {}", serialNumber);
        Certificate entity = getBySerialNumber(serialNumber);
        String leafPem = entity.getCertificatePem().trim();
        String chainPem = caKeyProvider.getChainPem().trim();
        return leafPem + "\n" + chainPem + "\n";
    }

    private Certificate getById(UUID id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ApiException("Certificate not found with ID: " + id));
    }

    private Certificate getBySerialNumber(String serialNumber) {
        return certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ApiException("Certificate not found with SerialNumber: " + serialNumber));
    }
}
