package ak.dev.khi_archive_platform.platform.dto.sitefont;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteFontResponseDTO implements Serializable {
    private Long id;
    private String name;
    private String fileUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
