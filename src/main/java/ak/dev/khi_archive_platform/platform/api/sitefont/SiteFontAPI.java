package ak.dev.khi_archive_platform.platform.api.sitefont;

import ak.dev.khi_archive_platform.platform.dto.sitefont.SiteFontResponseDTO;
import ak.dev.khi_archive_platform.platform.service.sitefont.SiteFontService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/site-fonts")
public class SiteFontAPI {

    private final SiteFontService siteFontService;

    @GetMapping
    @PreAuthorize("hasAuthority('site_font:read')")
    public ResponseEntity<List<SiteFontResponseDTO>> list() {
        return ResponseEntity.ok(siteFontService.list());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('site_font:create')")
    public ResponseEntity<SiteFontResponseDTO> create(@RequestPart("file") MultipartFile file,
                                                      @RequestParam(value = "name", required = false) String name) {
        return ResponseEntity.ok(siteFontService.create(file, name));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('site_font:update')")
    public ResponseEntity<SiteFontResponseDTO> activate(@PathVariable Long id) {
        return ResponseEntity.ok(siteFontService.setActive(id, true));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('site_font:update')")
    public ResponseEntity<SiteFontResponseDTO> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(siteFontService.setActive(id, false));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('site_font:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        siteFontService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
