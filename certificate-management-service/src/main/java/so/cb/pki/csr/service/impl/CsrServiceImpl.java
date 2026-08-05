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
import so.cb.pki.csr.dto.UploadCsrRequest;
import so.cb.pki.csr.entity.Csr;
import so.cb.pki.csr.enums.CsrStatus;
import so.cb.pki.csr.mapper.CsrMapper;
import so.cb.pki.csr.repository.CsrRepository;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.exception.ApiException;

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
    public void uploadCsr(UploadCsrRequest request) {
        log.info("Processing CSR upload request for bank BIC: {}", request.bic());
        UUID institutionId = institutionService.getActiveInstitutionIdByBic(request.bic());

        Csr entity = CsrMapper.toEntity(request, institutionId);
        entity = csrRepository.save(entity);
        log.info("CSR uploaded successfully with ID: {} for institutionId: {}, BIC: {}", entity.getId(), institutionId, request.bic());
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
