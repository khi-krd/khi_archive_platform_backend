package ak.dev.khi_archive_platform.platform.service.statictext;

import ak.dev.khi_archive_platform.platform.model.statictext.StaticText;
import ak.dev.khi_archive_platform.platform.repo.statictext.StaticTextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Admin-editable static text blocks keyed by (key, locale). The public
 * catalogue merges the per-locale map over its bundled defaults; nothing is
 * seeded automatically — admins create whatever keys the frontend needs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaticTextService {

    private final StaticTextRepository repo;

    /** Public read — { "nav.catalogue": "کاتالۆگ", ... } for one locale. */
    @Transactional(readOnly = true)
    public Map<String, String> getPublicMap(String locale) {
        Map<String, String> out = new LinkedHashMap<>();
        for (StaticText t : repo.findByLocaleOrderByKeyAsc(normLocale(locale))) {
            out.put(t.getKey(), t.getValue());
        }
        return out;
    }

    /** Admin listing — one row per key, values per locale. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (StaticText t : repo.findAllByOrderByKeyAsc()) {
            Map<String, Object> row = byKey.computeIfAbsent(t.getKey(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("key", k);
                m.put("section", t.getSection());
                m.put("values", new LinkedHashMap<String, Object>());
                return m;
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) row.get("values");
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", t.getId());
            v.put("value", t.getValue());
            v.put("updatedAt", t.getUpdatedAt());
            v.put("updatedBy", t.getUpdatedBy());
            values.put(t.getLocale(), v);
        }
        return new ArrayList<>(byKey.values());
    }

    /** Bulk upsert — entries {key, locale, value}. */
    @Transactional
    public int upsert(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        String user = currentUser();
        int saved = 0;
        for (Entry e : entries) {
            if (e == null || isBlank(e.key())) continue;
            String key = e.key().trim();
            String locale = normLocale(e.locale());
            if (isBlank(locale)) continue;

            StaticText t = repo.findByKeyAndLocale(key, locale).orElse(null);
            if (t == null) {
                t = StaticText.builder()
                        .key(key)
                        .locale(locale)
                        .value(e.value() == null ? "" : e.value())
                        .section(sectionOf(key))
                        .updatedBy(user)
                        .build();
            } else {
                t.setValue(e.value() == null ? "" : e.value());
                t.setSection(sectionOf(key));
                t.setUpdatedBy(user);
            }
            repo.save(t);
            saved++;
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        repo.deleteById(id);
    }

    // =========================================================================

    public record Entry(String key, String locale, String value) {}

    private String normLocale(String locale) {
        if (isBlank(locale)) return "";
        String l = locale.trim().toLowerCase(Locale.ROOT);
        return switch (l) {
            case "kmr", "kurmanji", "ku" -> "ku";
            case "ckb", "sorani" -> "ckb";
            default -> l;
        };
    }

    private String sectionOf(String key) {
        int dot = key.indexOf('.');
        return dot > 0 ? key.substring(0, dot) : key;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }
}
