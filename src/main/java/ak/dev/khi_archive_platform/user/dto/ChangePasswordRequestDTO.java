package ak.dev.khi_archive_platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {

    /** The user's existing password — only length-checked, not complexity (it was already stored). */
    private String currentPassword;

    /** Must be at least 6 characters. */
    private String newPassword;

    /** Must be identical to {@code newPassword} — verified in the service layer. */
    private String confirmPassword;
}