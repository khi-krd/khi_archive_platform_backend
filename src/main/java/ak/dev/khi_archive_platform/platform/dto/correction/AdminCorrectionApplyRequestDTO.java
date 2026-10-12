package ak.dev.khi_archive_platform.platform.dto.correction;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCorrectionApplyRequestDTO {

    private String resolveNote;
}
