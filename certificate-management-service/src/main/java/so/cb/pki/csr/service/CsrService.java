package so.cb.pki.csr.service;

import org.springframework.web.multipart.MultipartFile;
import so.cb.pki.csr.dto.CsrResponse;
import so.cb.pki.csr.dto.ReviewCsrRequest;
import so.cb.pki.csr.enums.CsrStatus;
import so.cb.pki.shared.dto.PaginatedResponse;

import java.util.List;
import java.util.UUID;

public interface CsrService {

    /**
     * Uploads a new Certificate Signing Request (CSR) for an active institution using multipart form data.
     *
     * @param file the CSR file payload
     * @param bic the Business Identifier Code (BIC) of the bank
     */
    void uploadCsr(MultipartFile file, String bic);

    /**
     * Reviews (approves or rejects) a pending CSR request.
     *
     * @param id the unique identifier of the CSR
     * @param request the review decision (APPROVED or REJECTED with optional rejection reason)
     * @return the updated CSR response
     */
    CsrResponse reviewCsr(UUID id, ReviewCsrRequest request);

    /**
     * Retrieves a CSR by its unique identifier.
     *
     * @param id the unique identifier of the CSR
     * @return the CSR response
     */
    CsrResponse getCsrById(UUID id);

    /**
     * Retrieves all CSRs submitted by a specific bank BIC.
     *
     * @param bic the Business Identifier Code (BIC) of the bank
     * @return list of matching CSR responses
     */
    List<CsrResponse> getCsrsByBic(String bic);

    /**
     * Retrieves a paginated list of CSRs, optionally filtered by status and BIC search keyword.
     *
     * @param status optional status filter (PENDING, APPROVED, REJECTED)
     * @param search optional BIC search keyword
     * @param pageNumber zero-based page index
     * @param pageSize number of items per page
     * @return paginated response of matching CSRs
     */
    PaginatedResponse<CsrResponse> getCsrs(CsrStatus status, String search, int pageNumber, int pageSize);

    /**
     * Retrieves the PEM-encoded content of an APPROVED CSR for certificate issuance by the CA.
     *
     * @param csrId the unique identifier of the approved CSR
     * @return raw PEM text of the CSR
     */
    String getApprovedCsrPem(UUID csrId);
}
