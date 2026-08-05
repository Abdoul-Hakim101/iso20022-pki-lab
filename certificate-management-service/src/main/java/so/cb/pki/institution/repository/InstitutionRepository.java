package so.cb.pki.institution.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import so.cb.pki.institution.entity.Institution;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    Optional<Institution> findByBic(String bic);

    boolean existsByBic(String bic);

    @Query("SELECT i FROM Institution i WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.bic) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Institution> search(@Param("search") String search, Pageable pageable);
}
