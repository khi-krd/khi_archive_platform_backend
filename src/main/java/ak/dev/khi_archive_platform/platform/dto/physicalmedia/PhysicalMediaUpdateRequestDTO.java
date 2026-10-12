package ak.dev.khi_archive_platform.platform.dto.physicalmedia;

import ak.dev.khi_archive_platform.platform.enums.DigitizationStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Body for {@code PATCH /api/physical-media/{pmCode}}. All fields are
 * optional; {@code null} means "leave alone" — the service only touches a
 * column when the corresponding field is present in the JSON. This avoids
 * the classic "save wiped my notes" bug when the UI sends a partial form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalMediaUpdateRequestDTO {

    private Integer rowNumber;

    private Integer inventoryNumber;

    private String physicalMediaType;

    private String mediaCategory;

    private String title;

    private String sizeGB;

    private String physicalLabel;

    private String physicalSize;

    private String content;

    private String archiveDepNote;

    private DigitizationStatus digitization;

    /** Sent as 0/1/2 — service resolves to enum when {@link #digitization}
     *  is null. */
    private Integer digitizationCode;

    private String owner;

    private Integer year;

    private Integer durationMin;

    private Integer trackNumbers;

    private String trackName;

    private String extension;

    private String bitOrColorDepth;

    private String sampleOrFrameRate;

    private String channelsOrResolution;

    private String playbackModel;

    private String captureInterface;

    private String signalInterface;

    private String ingestSoftware;

    private String formatCodec;

    private LocalDate digitizeDate;

    private String tags;

    private Boolean needToClear;

    /** Sent as 0/1 — service resolves to boolean when {@link #needToClear}
     *  is null. */
    private Integer needToClearCode;

    private String captureDepNote;
}
