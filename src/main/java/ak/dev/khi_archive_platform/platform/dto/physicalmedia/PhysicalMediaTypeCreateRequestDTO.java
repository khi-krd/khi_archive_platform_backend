package ak.dev.khi_archive_platform.platform.dto.physicalmedia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code POST /api/physical-media/types}. {@code name} is the
 * only required field; the nine technical defaults are optional so an
 * admin can register the type now and fill the capture chain in later.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalMediaTypeCreateRequestDTO {

    private String name;

    private String description;

    private String extension;
    private String bitOrColorDepth;
    private String sampleOrFrameRate;
    private String channelsOrResolution;

    private String playbackModel;
    private String captureInterface;
    private String signalInterface;
    private String ingestSoftware;

    private String formatCodec;
}
