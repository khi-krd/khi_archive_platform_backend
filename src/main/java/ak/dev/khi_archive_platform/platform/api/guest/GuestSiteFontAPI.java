package ak.dev.khi_archive_platform.platform.api.guest;

import ak.dev.khi_archive_platform.S3Service;
import ak.dev.khi_archive_platform.platform.dto.sitefont.SiteFontResponseDTO;
import ak.dev.khi_archive_platform.platform.model.sitefont.SiteFont;
import ak.dev.khi_archive_platform.platform.service.sitefont.SiteFontService;
import ak.dev.khi_archive_platform.user.exceptions.UserStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Public read for the admin-uploaded site typeface. Anonymous visitors must
 * paint the font on the sign-in page and the public catalogue before they
 * hold a token, so reads live under the permitAll {@code /api/guest/**}
 * namespace; writes stay behind {@code site_font:*} authorities on
 * {@code /api/site-fonts}.
 *
 * <p>The file endpoint doubles as the CORS-safe font source: a {@code url()}
 * inside {@code @font-face} cannot carry an Authorization header, and the S3
 * bucket sends no CORS headers of its own, so the bytes are proxied through
 * the API (which already emits the right CORS response headers).</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guest")
public class GuestSiteFontAPI {

    private final SiteFontService siteFontService;
    private final S3Service s3Service;

    /** Which font is live — 404 when the admin has not activated one. */
    @GetMapping("/site-font")
    public ResponseEntity<SiteFontResponseDTO> getActive() {
        return ResponseEntity.ok(siteFontService.getActive());
    }

    /**
     * Streams a font file's bytes. Font files are tiny (well under the image
     * payloads this pattern already proxies), so a buffered read is fine.
     */
    @GetMapping("/site-fonts/{id}/file")
    public ResponseEntity<byte[]> file(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        SiteFont font = siteFontService.getEntity(id);
        String fileUrl = font.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Font file not available");
        }

        String etag = "\"" + sha1Short(id + ":" + font.getUpdatedAt()) + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        String key = s3Service.extractKeyFromUrl(fileUrl);
        if (key == null || key.isBlank()) {
            log.error("Could not extract S3 key from fileUrl for site font id={}", id);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Font file not available");
        }

        byte[] bytes;
        try (ResponseInputStream<GetObjectResponse> stream = s3Service.openStream(key)) {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read site font for key={}", key, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serve font file");
        } catch (UserStorageException e) {
            if (e.getCause() instanceof S3Exception s3Exception && s3Exception.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Font file not available");
            }
            log.error("S3 storage failure serving site font id={}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serve font file");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(fontContentType(fileUrl));
        headers.setETag(etag);
        // Fonts are immutable per record (a replace = a new row), so cache hard.
        headers.setCacheControl("public, max-age=86400");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private MediaType fontContentType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        String path = lower.split("[?#]")[0];
        if (path.endsWith(".woff2")) return MediaType.valueOf("font/woff2");
        if (path.endsWith(".woff"))  return MediaType.valueOf("font/woff");
        if (path.endsWith(".ttf"))   return MediaType.valueOf("font/ttf");
        if (path.endsWith(".otf"))   return MediaType.valueOf("font/otf");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String sha1Short(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
