package so.cb.pki.institution.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import so.cb.pki.institution.dto.CreateInstitutionRequest;
import so.cb.pki.institution.dto.InstitutionResponse;
import so.cb.pki.institution.entity.Institution;
import so.cb.pki.institution.enums.InstitutionStatus;
import so.cb.pki.institution.mapper.InstitutionMapper;
import so.cb.pki.institution.repository.InstitutionRepository;
import so.cb.pki.institution.service.InstitutionService;
import so.cb.pki.shared.dto.PaginatedResponse;
import so.cb.pki.shared.exception.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Transactional(rollbackFor = Exception.class)
public class InstitutionServiceImpl implements InstitutionService {

    private final InstitutionRepository institutionRepository;

    @Override
    public InstitutionResponse createInstitution(CreateInstitutionRequest request) {
        if (institutionRepository.existsByBic(request.bic())) {
            throw new ApiException("Institution with BIC code '" + request.bic() + "' already exists");
        }

        Institution entity = InstitutionMapper.toEntity(request);
        entity = institutionRepository.save(entity);
        return InstitutionMapper.toResponse(entity);
    }

    @Override
    public InstitutionResponse updateInstitutionStatus(UUID id, InstitutionStatus status) {
        Institution entity = getById(id);
        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());
        entity = institutionRepository.save(entity);
        return InstitutionMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInstitutionActive(String bic) {
        return institutionRepository.findByBic(bic)
                .map(institution -> institution.getStatus() == InstitutionStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionResponse getInstitutionById(UUID id) {
        Institution entity = getById(id);
        return InstitutionMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InstitutionResponse> getInstitutions(String search, int pageNumber, int pageSize) {
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
