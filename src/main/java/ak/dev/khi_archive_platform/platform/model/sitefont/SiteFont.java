package ak.dev.khi_archive_platform.platform.model.sitefont;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "site_fonts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteFont {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display label shown in the admin library ("Rabar", "Vazirmatn"…). */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Public S3 URL of the uploaded font file. */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /** Exactly one row is active at a time — that file is served to pages. */
    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
