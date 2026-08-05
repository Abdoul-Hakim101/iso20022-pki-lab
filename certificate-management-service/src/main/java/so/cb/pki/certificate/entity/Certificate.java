package so.cb.pki.certificate.entity;

import jakarta.persistence.*;
import lombok.*;
import so.cb.pki.certificate.enums.CertificateStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificate", schema = "pki")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "csr_id", nullable = false, unique = true)
    private UUID csrId;

    @Column(name = "bic", nullable = false, length = 11)
    private String bic;

    @Column(name = "serial_number", nullable = false, unique = true, length = 64)
    private String serialNumber;

    @Column(name = "certificate_pem", nullable = false, columnDefinition = "TEXT")
    private String certificatePem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CertificateStatus status;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
