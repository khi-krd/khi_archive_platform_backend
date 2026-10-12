package ak.dev.khi_archive_platform.platform.dto.correction;

import ak.dev.khi_archive_platform.platform.enums.CorrectionMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuestCorrectionSubmitRequestDTO {

    private CorrectionMediaType mediaType;

    private String mediaCode;

    private String targetField;

    private String currentValue;

    private String suggestedValue;

    private String note;
}
