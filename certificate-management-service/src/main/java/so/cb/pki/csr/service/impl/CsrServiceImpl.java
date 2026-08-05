package so.cb.pki.csr.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import so.cb.pki.csr.event.CsrApprovedEvent;
import so.cb.pki.csr.dto.CsrResponse;
import so.cb.pki.csr.dto.ReviewCsrRequest;
import so.cb.pki.csr.dto.UploadCsrRequest;
import so.cb.pki.csr.entity.Csr;
import so.cb.pki.csr.enums.CsrStatus;
import so.cb.pki.csr.mapper.CsrMapper;
import so.cb.pki.csr.repository.CsrRepository;
import so.cb.pki.csr.service.CsrService;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.exception.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Transactional(rollbackFor = Exception.class)
public class CsrServiceImpl implements CsrService {

    private final CsrRepository csrRepository;
    private final InstitutionService institutionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void uploadCsr(UploadCsrRequest request) {
        UUID institutionId = institutionService.getActiveInstitutionIdByBic(request.bic());

        Csr entity = CsrMapper.toEntity(request, institutionId);
        csrRepository.save(entity);
    }

    @Override
    public CsrResponse reviewCsr(UUID id, ReviewCsrRequest request) {
        Csr entity = getById(id);

        if (entity.getStatus() != CsrStatus.PENDING) {
            throw new ApiException("CSR with ID '" + id + "' has already been reviewed (Status: " + entity.getStatus() + ")");
        }

        if (request.status() == CsrStatus.PENDING) {
            throw new ApiException("Review status must be APPROVED or REJECTED");
        }

        if (request.status() == CsrStatus.REJECTED && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new ApiException("Rejection reason is required when rejecting a CSR");
        }

        entity.setStatus(request.status());
        entity.setRejectionReason(request.rejectionReason());
        entity.setUpdatedAt(Instant.now());

        entity = csrRepository.save(entity);

        if (request.status() == CsrStatus.APPROVED) {
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
        Csr entity = getById(id);
        return CsrMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CsrResponse> getCsrsByBic(String bic) {
        return csrRepository.findByBic(bic).stream()
                .map(CsrMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CsrResponse> getCsrs(CsrStatus status, String search, int pageNumber, int pageSize) {
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
        Csr entity = getById(csrId);
        if (entity.getStatus() != CsrStatus.APPROVED) {
            throw new ApiException("CSR with ID '" + csrId + "' is not APPROVED (Current Status: " + entity.getStatus() + ")");
        }
        return entity.getCsrPem();
    }

    private Csr getById(UUID id) {
        return csrRepository.findById(id)
                .orElseThrow(() -> new ApiException("CSR not found with ID: " + id));
    }
}
