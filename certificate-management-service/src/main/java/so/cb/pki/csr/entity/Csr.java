package so.cb.pki.csr.entity;

import jakarta.persistence.*;
import lombok.*;
import so.cb.pki.csr.enums.CsrStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "csr", schema = "pki")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Csr {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "bic", nullable = false, length = 11)
    private String bic;

    @Column(name = "csr_pem", nullable = false, columnDefinition = "TEXT")
    private String csrPem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CsrStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
