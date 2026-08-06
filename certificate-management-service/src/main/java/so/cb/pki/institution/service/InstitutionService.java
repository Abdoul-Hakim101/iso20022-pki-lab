package so.cb.pki.institution.service;

import so.cb.pki.institution.dto.CreateInstitutionRequest;
import so.cb.pki.institution.dto.InstitutionResponse;
import so.cb.pki.institution.enums.InstitutionStatus;
import so.cb.pki.shared.dto.PaginatedResponse;

import java.util.UUID;

public interface InstitutionService {

    /**
     * Registers a new institution in the system.
     *
     * @param request the registration details containing name and BIC
     * @return the registered institution details
     */
    InstitutionResponse createInstitution(CreateInstitutionRequest request);

    /**
     * Updates the operational status of an institution (e.g. suspending or activating).
     *
     * @param id the unique identifier of the institution
     * @param status the new operational status
     * @return the updated institution details
     */
    InstitutionResponse updateInstitutionStatus(UUID id, InstitutionStatus status);

    /**
     * Retrieves the ID of an institution by BIC code if it is registered and has ACTIVE status in a single query.
     *
     * @param bic the Business Identifier Code (BIC)
     * @return the unique identifier UUID of the active institution
     */
    UUID getActiveInstitutionIdByBic(String bic);

    /**
     * Retrieves the name of an institution by its unique identifier.
     *
     * @param id the unique identifier UUID of the institution
     * @return the name of the institution
     */
    String getInstitutionNameById(UUID id);

    /**
     * Retrieves a paginated list of institutions, optionally filtered by a search term.
     *
     * @param search optional search term to match institution name or BIC
     * @param pageNumber zero-based page index
     * @param pageSize number of records per page
     * @return paginated list of matching institutions
     */
    PaginatedResponse<InstitutionResponse> getInstitutions(
            String search,
            int pageNumber,
            int pageSize
    );
}
