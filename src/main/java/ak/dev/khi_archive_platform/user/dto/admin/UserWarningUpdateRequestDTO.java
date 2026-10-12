package ak.dev.khi_archive_platform.user.dto.admin;

import ak.dev.khi_archive_platform.user.enums.WarningSeverity;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code PUT /api/admin/warnings/{id}}. Every field is optional —
 * unset fields keep their current value. Acknowledged warnings stay
 * acknowledged after an edit, but the recipient is shown the "edited" badge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWarningUpdateRequestDTO {

    private WarningSeverity severity;

    private String title;

    private String message;
}
