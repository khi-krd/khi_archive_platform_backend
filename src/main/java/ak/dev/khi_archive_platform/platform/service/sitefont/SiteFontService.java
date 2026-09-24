package ak.dev.khi_archive_platform.platform.service.sitefont;

import ak.dev.khi_archive_platform.S3Service;
import ak.dev.khi_archive_platform.platform.dto.sitefont.SiteFontResponseDTO;
import ak.dev.khi_archive_platform.platform.exceptions.SiteFontNotFoundException;
import ak.dev.khi_archive_platform.platform.model.sitefont.SiteFont;
import ak.dev.khi_archive_platform.platform.repo.sitefont.SiteFontRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SiteFontService {

    private static final String SITE_FONT_FOLDER = "site_fonts";
    private static final Set<String> FONT_EXTENSIONS = Set.of("woff2", "woff", "ttf", "otf");
    private static final long MAX_FONT_BYTES = 20L * 1024 * 1024;

    private final SiteFontRepository siteFontRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public List<SiteFontResponseDTO> list() {
        return siteFontRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    /** The font every page paints with. 404 when nothing is activated. */
    @Transactional(readOnly = true)
    public SiteFontResponseDTO getActive() {
        return siteFontRepository.findByActiveTrue()
                .map(this::toResponse)
                .orElseThrow(() -> new SiteFontNotFoundException("No site font is active."));
    }

    @Transactional(readOnly = true)
    public SiteFont getEntity(Long id) {
        return findOrThrow(id);
    }

    public SiteFontResponseDTO create(MultipartFile file, String name) {
        requireFile(file);
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            cleanName = stripExtension(file.getOriginalFilename());
        }
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Font name is required.");
        }

        String fileUrl = s3Service.upload(file, SITE_FONT_FOLDER);

        // The very first upload is almost certainly meant to go live —
        // activate it so the admin does not have to take a second step.
        boolean firstRow = siteFontRepository.count() == 0;
        SiteFont saved = siteFontRepository.save(SiteFont.builder()
                .name(cleanName)
                .fileUrl(fileUrl)
                .active(firstRow)
                .build());
        return toResponse(saved);
    }

    /** Single-active invariant: turning one row on turns every other row off. */
    public SiteFontResponseDTO setActive(Long id, boolean active) {
        SiteFont font = findOrThrow(id);
        if (active) {
            for (SiteFont other : siteFontRepository.findAll()) {
                if (other.isActive() && !other.getId().equals(id)) {
                    other.setActive(false);
                    siteFontRepository.save(other);
                }
            }
        }
        font.setActive(active);
        return toResponse(siteFontRepository.save(font));
    }

    public void delete(Long id) {
        SiteFont font = findOrThrow(id);
        siteFontRepository.delete(font);
        if (font.getFileUrl() != null && s3Service.isOurS3Url(font.getFileUrl())) {
            s3Service.deleteFile(font.getFileUrl());
        }
    }

    private SiteFont findOrThrow(Long id) {
        return siteFontRepository.findById(id)
                .orElseThrow(() -> new SiteFontNotFoundException("Site font not found: " + id));
    }

    private void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Font file is required.");
        }
        if (file.getSize() > MAX_FONT_BYTES) {
            throw new IllegalArgumentException("Font file exceeds the 20 MB limit.");
        }
        String ext = extension(file.getOriginalFilename());
        if (!FONT_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported font format. Use WOFF2, WOFF, TTF, or OTF.");
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        if (filename == null) return "";
        String base = filename.trim();
        int dot = base.lastIndexOf('.');
        return dot > 0 ? base.substring(0, dot) : base;
    }

    private SiteFontResponseDTO toResponse(SiteFont font) {
        return SiteFontResponseDTO.builder()
                .id(font.getId())
                .name(font.getName())
                .fileUrl(font.getFileUrl())
                .active(font.isActive())
                .createdAt(font.getCreatedAt())
                .updatedAt(font.getUpdatedAt())
                .build();
    }
}
