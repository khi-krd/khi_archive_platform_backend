package ak.dev.khi_archive_platform.platform.model.statictext;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An admin-editable UI text block keyed by (key, locale), e.g.
 * ("nav.catalogue", "ckb") -> "کاتالۆگ". The public catalogue merges these
 * over its bundled defaults; admins edit them through /api/text-blocks.
 */
@Entity
@Table(name = "static_texts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_static_texts_key_locale",
                columnNames = {"text_key", "locale"}),
        indexes = {
                @Index(name = "idx_static_texts_locale", columnList = "locale"),
                @Index(name = "idx_static_texts_section", columnList = "section")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaticText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dotted message key, e.g. "nav.catalogue". */
    @Column(name = "text_key", nullable = false, length = 300)
    private String key;

    /** Locale tag: "ckb" (Sorani), "ku" (Kurmanji), "en", … */
    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    /** Top-level key segment kept for grouping/filtering in the editor. */
    @Column(name = "section", length = 120)
    private String section;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
