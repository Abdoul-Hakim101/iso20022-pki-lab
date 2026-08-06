package so.cb.pki.csr.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import so.cb.pki.csr.event.CsrApprovedEvent;
import so.cb.pki.csr.service.CsrService;
import so.cb.pki.csr.dto.CsrResponse;
import so.cb.pki.csr.dto.ReviewCsrRequest;
import so.cb.pki.csr.entity.Csr;
import so.cb.pki.csr.enums.CsrStatus;
import so.cb.pki.csr.mapper.CsrMapper;
import so.cb.pki.csr.repository.CsrRepository;
import org.springframework.web.multipart.MultipartFile;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.exception.ApiException;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CsrServiceImpl implements CsrService {

    private final CsrRepository csrRepository;
    private final InstitutionService institutionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void uploadCsr(MultipartFile file, String bic) {
        log.info("Processing CSR upload request for bank BIC: {}", bic);

        if (file == null || file.isEmpty()) {
            throw new ApiException("CSR file must not be empty");
        }

        String trimmedCsrPem;
        try {
            trimmedCsrPem = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            log.error("Failed to read uploaded CSR file for BIC {}: {}", bic, e.getMessage());
            throw new ApiException("Failed to read uploaded CSR file: " + e.getMessage());
        }

        validateCsrPem(trimmedCsrPem);
        UUID institutionId = institutionService.getActiveInstitutionIdByBic(bic);

        Csr entity = CsrMapper.toEntity(bic, trimmedCsrPem, institutionId);
        entity = csrRepository.save(entity);
        log.info("CSR uploaded successfully with ID: {} for institutionId: {}, BIC: {}", entity.getId(), institutionId, bic);
    }

    private void validateCsrPem(String csrPem) {
        if (csrPem == null || csrPem.isBlank()) {
            throw new ApiException("CSR PEM text must not be empty");
        }

        try (PEMParser pemParser = new PEMParser(new StringReader(csrPem.trim()))) {
            Object object = pemParser.readObject();
            if (!(object instanceof PKCS10CertificationRequest csr)) {
                throw new ApiException("Invalid CSR format: Provided text is not a valid PKCS#10 Certificate Signing Request");
            }
            if (csr.getSubject() == null || csr.getSubject().toString().isBlank()) {
                throw new ApiException("Invalid CSR: Certificate Signing Request does not contain a valid Subject DN");
            }
            if (csr.getSubjectPublicKeyInfo() == null) {
                throw new ApiException("Invalid CSR: Certificate Signing Request does not contain valid Public Key info");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("CSR validation failed: {}", e.getMessage());
            throw new ApiException("Failed to parse CSR PEM text: " + e.getMessage());
        }
    }

    @Override
    public CsrResponse reviewCsr(UUID id, ReviewCsrRequest request) {
        log.info("Processing review decision '{}' for CSR ID: {}", request.status(), id);
        Csr entity = getById(id);

        if (entity.getStatus() != CsrStatus.PENDING) {
            log.warn("Review rejected for CSR ID: {}. Current status is already {}", id, entity.getStatus());
            throw new ApiException("CSR with ID '" + id + "' has already been reviewed (Status: " + entity.getStatus() + ")");
        }

        if (request.status() == CsrStatus.PENDING) {
            log.warn("Invalid review decision 'PENDING' for CSR ID: {}", id);
            throw new ApiException("Review status must be APPROVED or REJECTED");
        }

        if (request.status() == CsrStatus.REJECTED && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            log.warn("Rejection reason missing for rejected CSR ID: {}", id);
            throw new ApiException("Rejection reason is required when rejecting a CSR");
        }

        entity.setStatus(request.status());
        entity.setRejectionReason(request.rejectionReason());
        entity.setUpdatedAt(Instant.now());

        entity = csrRepository.save(entity);
        log.info("CSR ID: {} status updated to {}", id, entity.getStatus());

        if (request.status() == CsrStatus.APPROVED) {
            log.info("Publishing CsrApprovedEvent for approved CSR ID: {}, BIC: {}", entity.getId(), entity.getBic());
            eventPublisher.publishEvent(new CsrApprovedEvent(
                    entity.getId(),
                    entity.getInstitutionId(),
                    entity.getBic(),
                    entity.getCsrPem()
            ));
        }

        return CsrMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CsrResponse getCsrById(UUID id) {
        log.debug("Fetching CSR details by ID: {}", id);
        Csr entity = getById(id);
        return CsrMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CsrResponse> getCsrsByBic(String bic) {
        log.debug("Fetching CSR records for bank BIC: {}", bic);
        return csrRepository.findByBic(bic).stream()
                .map(CsrMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CsrResponse> getCsrs(CsrStatus status, String search, int pageNumber, int pageSize) {
        log.debug("Searching CSR records (status: {}, search: '{}', page: {}, size: {})", status, search, pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Csr> page = csrRepository.search(status, search, pageable);

        List<CsrResponse> items = page.getContent().stream()
                .map(CsrMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(
                items,
                (int) page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String getApprovedCsrPem(UUID csrId) {
        log.debug("Fetching approved CSR PEM text for ID: {}", csrId);
        Csr entity = getById(csrId);
        if (entity.getStatus() != CsrStatus.APPROVED) {
            log.warn("Cannot retrieve CSR PEM for ID: {}. Current status is {}", csrId, entity.getStatus());
            throw new ApiException("CSR with ID '" + csrId + "' is not APPROVED (Current Status: " + entity.getStatus() + ")");
        }
        return entity.getCsrPem();
    }

    private Csr getById(UUID id) {
        return csrRepository.findById(id)
                .orElseThrow(() -> new ApiException("CSR not found with ID: " + id));
    }
}
