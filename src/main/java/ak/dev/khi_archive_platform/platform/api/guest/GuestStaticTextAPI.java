package ak.dev.khi_archive_platform.platform.api.guest;

import ak.dev.khi_archive_platform.platform.service.statictext.StaticTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public read for editable static text blocks — the guest catalogue can paint
 * admin-edited strings before a token exists. Writes stay behind the
 * {@code static_text:*} authorities on {@code /api/text-blocks}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guest")
public class GuestStaticTextAPI {

    private final StaticTextService service;

    @GetMapping("/text-blocks")
    public ResponseEntity<Map<String, String>> getMap(
            @RequestParam(defaultValue = "ckb") String locale) {
        return ResponseEntity.ok(service.getPublicMap(locale));
    }
}
