package ak.dev.khi_archive_platform.platform.dto.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ak.dev.khi_archive_platform.user.consts.ValidationPatterns;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryCreateRequestDTO {
    private String categoryCode;

    private String name;

    private String description;

    /** Keywords / alternative names to help prevent duplicate categories. */
    private List<String> keywords;
}
