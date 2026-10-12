package ak.dev.khi_archive_platform.user.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleChangeRequestDTO {
    /** Role name: GUEST, EMPLOYEE, or ADMIN (case-insensitive on input). */
    private String role;
}
