package so.cb.pki.certificate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import so.cb.pki.certificate.entity.Certificate;
import so.cb.pki.certificate.enums.CertificateStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findBySerialNumber(String serialNumber);

    Optional<Certificate> findByCsrId(UUID csrId);

    List<Certificate> findByBic(String bic);

    List<Certificate> findByStatus(CertificateStatus status);

    @Query("SELECT c FROM Certificate c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.bic) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           "LOWER(c.serialNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Certificate> search(
            @Param("status") CertificateStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
