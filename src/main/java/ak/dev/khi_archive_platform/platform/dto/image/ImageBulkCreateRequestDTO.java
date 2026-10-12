package ak.dev.khi_archive_platform.platform.dto.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bulk-create payload for Image. Same fields as the single-create DTO but
 * accepts the {@code imageFileUrl} directly instead of a multipart file part,
 * so 1000s of records can be inserted in one JSON call.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class ImageBulkCreateRequestDTO extends ImageBaseRequestDTO {

    private static final java.util.Set<String> VALID_VERSIONS = java.util.Set.of(
            "RAW", "MASTER", "RESTORED", "ARCHIVE", "ORIGINAL",
            "HIGH_RES", "PROFESSIONAL"
    );

    /** Pre-uploaded image URL (S3 or external). May be null/blank for fixtures. */
    private String imageFileUrl;

    public boolean isProjectCodePresent() {
        return getProjectCode() != null && !getProjectCode().isBlank();
    }

    public boolean isImageVersionValid() {
        String iv = getImageVersion();
        return iv != null && VALID_VERSIONS.contains(iv.toUpperCase(java.util.Locale.ROOT));
    }

    public boolean isVersionNumberValid() {
        Integer vn = getVersionNumber();
        return vn != null && vn >= 1;
    }

    public boolean isCopyNumberValid() {
        Integer cn = getCopyNumber();
        return cn != null && cn >= 1;
    }
}
