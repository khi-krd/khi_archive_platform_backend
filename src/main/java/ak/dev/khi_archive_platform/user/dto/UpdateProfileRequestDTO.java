package ak.dev.khi_archive_platform.user.dto;

import ak.dev.khi_archive_platform.user.consts.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Self-service profile-update DTO. Every field is optional — only the fields
 * that are non-null and non-blank are applied. Sending an empty body is a
 * valid no-op so the same DTO can be reused for partial updates from the
 * profile screen (name only, email only, etc.).
 */
@Data
public class UpdateProfileRequestDTO {

    private String username;

    private String name;

    private String email;
}
