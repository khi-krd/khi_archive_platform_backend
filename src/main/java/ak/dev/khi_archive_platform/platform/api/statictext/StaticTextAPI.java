package ak.dev.khi_archive_platform.platform.api.statictext;

import ak.dev.khi_archive_platform.platform.service.statictext.StaticTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for editable static text blocks. Writes need the
 * {@code static_text:*} authorities — ADMIN holds them all automatically,
 * and they are deliberately absent from EMPLOYEE_DEFAULT_PERMISSIONS so only
 * admins edit interface text. Public reads live under /api/guest/text-blocks.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/text-blocks")
public class StaticTextAPI {

    private final StaticTextService service;

    /** Admin: every key with its per-locale values. */
    @GetMapping
    @PreAuthorize("hasAuthority('static_text:read')")
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    /** Admin: bulk upsert. Body: {entries:[{key, locale, value}]}. */
    @PutMapping
    @PreAuthorize("hasAuthority('static_text:update')")
    public ResponseEntity<Map<String, Object>> upsert(
            @RequestBody(required = false) UpsertRequest body) {
        List<StaticTextService.Entry> entries = body == null ? null : body.entries();
        int saved = service.upsert(entries);
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('static_text:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record UpsertRequest(List<StaticTextService.Entry> entries) {}
}
