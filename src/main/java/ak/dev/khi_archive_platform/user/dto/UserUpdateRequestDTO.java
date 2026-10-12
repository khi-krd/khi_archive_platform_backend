package ak.dev.khi_archive_platform.user.dto;

import ak.dev.khi_archive_platform.user.consts.ValidationPatterns;
import ak.dev.khi_archive_platform.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDTO {

    /** Optional — updates the display name when provided. */
    private String name;

    /** Optional — updates the username when provided. */
    private String username;

    /** Optional — updates the email address when provided. */
    private String email;

    /** Optional — updates the password when provided. */
    private String password;

    /** Optional — changes the user role when provided. */
    private Role role;

    /** Optional — enables or disables the user account when provided. */
    private Boolean isActivated;

    /** Optional — when true, removes the current profile image. */
    private Boolean removeProfileImage;  // Flag to explicitly remove image
}