package ak.dev.khi_archive_platform.platform.dto.maqam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload for {@code POST /api/maqam}. The audio binary travels alongside as
 * the {@code file} multipart part — the URL is computed server-side and never
 * accepted from the client. The teacher roster (1–3 user IDs) can be supplied
 * at create time or assigned later by an admin via the dedicated endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaqamCreateRequestDTO {

    private String songName;

    private String producer;

    private String archiveNote;

    /** Optional. When provided, must contain 1–3 user IDs and every ID must
     *  belong to a user with role TEACHER. Validated in the service layer. */
    private List<Long> teacherUserIds;
}
