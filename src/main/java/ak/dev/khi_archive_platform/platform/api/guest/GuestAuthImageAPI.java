package ak.dev.khi_archive_platform.platform.api.guest;

import ak.dev.khi_archive_platform.platform.dto.authimage.AuthImageResponseDTO;
import ak.dev.khi_archive_platform.platform.service.authimage.AuthImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read for the sign-in / register brand-panel image. Anonymous
 * visitors see the auth pages before they hold a token, so the current
 * image is served under the permitAll {@code /api/guest/**} namespace —
 * writes stay behind {@code auth_image:*} authorities on
 * {@code /api/auth-image}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guest")
public class GuestAuthImageAPI {

    private final AuthImageService authImageService;

    @GetMapping("/auth-image")
    public ResponseEntity<AuthImageResponseDTO> getCurrent() {
        return ResponseEntity.ok(authImageService.getCurrent());
    }
}
