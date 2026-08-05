package so.cb.pki.csr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import so.cb.pki.csr.entity.Csr;
import so.cb.pki.csr.enums.CsrStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface CsrRepository extends JpaRepository<Csr, UUID> {

    List<Csr> findByBic(String bic);

    List<Csr> findByStatus(CsrStatus status);

    @Query("SELECT c FROM Csr c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.bic) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Csr> search(@Param("status") CsrStatus status, @Param("search") String search, Pageable pageable);
}
