package ak.dev.khi_archive_platform.platform.api.authimage;

import ak.dev.khi_archive_platform.platform.dto.authimage.AuthImageResponseDTO;
import ak.dev.khi_archive_platform.platform.service.authimage.AuthImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth-image")
public class AuthImageAPI {

    private final AuthImageService authImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('auth_image:create')")
    public ResponseEntity<AuthImageResponseDTO> create(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(authImageService.create(file));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('auth_image:read')")
    public ResponseEntity<AuthImageResponseDTO> getCurrent() {
        return ResponseEntity.ok(authImageService.getCurrent());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('auth_image:read')")
    public ResponseEntity<AuthImageResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(authImageService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('auth_image:update')")
    public ResponseEntity<AuthImageResponseDTO> update(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(authImageService.update(id, file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('auth_image:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authImageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
