package so.cb.pki.institution.entity;

import jakarta.persistence.*;
import lombok.*;
import so.cb.pki.institution.enums.InstitutionStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "institution", schema = "pki")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Institution {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "bic", nullable = false, unique = true, length = 11)
    private String bic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InstitutionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
