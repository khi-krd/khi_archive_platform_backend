package ak.dev.khi_archive_platform.platform.dto.video;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bulk-create payload for Video. Same fields as the single-create DTO but
 * accepts the {@code videoFileUrl} directly instead of a multipart file part.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class VideoBulkCreateRequestDTO extends VideoBaseRequestDTO {

    private static final java.util.Set<String> VALID_VERSIONS = java.util.Set.of(
            "RAW", "MASTER", "RESTORED", "ARCHIVE", "ORIGINAL",
            "4K_MASTER", "PROFESSIONAL"
    );

    /** Pre-uploaded video URL (S3 or external). May be null/blank for fixtures. */
    private String videoFileUrl;

    public boolean isProjectCodePresent() {
        return getProjectCode() != null && !getProjectCode().isBlank();
    }

    public boolean isVideoVersionValid() {
        String vv = getVideoVersion();
        return vv != null && VALID_VERSIONS.contains(vv.toUpperCase(java.util.Locale.ROOT));
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
