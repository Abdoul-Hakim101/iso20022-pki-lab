package so.cb.pki.institution.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.institution.dto.CreateInstitutionRequest;
import so.cb.pki.institution.dto.InstitutionResponse;
import so.cb.pki.institution.entity.Institution;
import so.cb.pki.institution.enums.InstitutionStatus;
import so.cb.pki.institution.mapper.InstitutionMapper;
import so.cb.pki.institution.repository.InstitutionRepository;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.exception.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class InstitutionServiceImpl implements InstitutionService {

    private final InstitutionRepository institutionRepository;

    @Override
    public InstitutionResponse createInstitution(CreateInstitutionRequest request) {
        log.info("Creating new institution with name: '{}', BIC: '{}'", request.name(), request.bic());

        if (institutionRepository.existsByBic(request.bic())) {
            log.warn("Failed to create institution. BIC code '{}' already exists", request.bic());
            throw new ApiException("Institution with BIC code '" + request.bic() + "' already exists");
        }

        Institution entity = InstitutionMapper.toEntity(request);
        entity = institutionRepository.save(entity);
        log.info("Institution created successfully with ID: {}, BIC: {}", entity.getId(), entity.getBic());
        return InstitutionMapper.toResponse(entity);
    }

    @Override
    public InstitutionResponse updateInstitutionStatus(UUID id, InstitutionStatus status) {
        log.info("Updating status of institution ID: {} to {}", id, status);
        Institution entity = getById(id);
        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());
        entity = institutionRepository.save(entity);
        log.info("Institution ID: {} status updated to {} successfully", id, status);
        return InstitutionMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getActiveInstitutionIdByBic(String bic) {
        log.debug("Fetching active institution ID for BIC: '{}'", bic);
        return institutionRepository.findActiveIdByBic(bic)
                .orElseThrow(() -> {
                    log.warn("Institution lookup failed. BIC '{}' is not registered or active", bic);
                    return new ApiException("Institution with BIC '" + bic + "' is not registered or active");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public String getInstitutionNameById(UUID id) {
        log.debug("Fetching institution name for ID: {}", id);
        return institutionRepository.findById(id)
                .map(Institution::getName)
                .orElse("Unknown Institution");
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InstitutionResponse> getInstitutions(String search, int pageNumber, int pageSize) {
        log.debug("Searching institutions (search: '{}', page: {}, size: {})", search, pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Institution> page = institutionRepository.search(search, pageable);

        List<InstitutionResponse> items = page.getContent().stream()
                .map(InstitutionMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(
                items,
                (int) page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    private Institution getById(UUID id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ApiException("Institution not found with id: " + id));
    }
}
