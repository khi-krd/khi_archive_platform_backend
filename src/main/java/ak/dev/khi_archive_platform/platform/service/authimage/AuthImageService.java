package ak.dev.khi_archive_platform.platform.service.authimage;

import ak.dev.khi_archive_platform.S3Service;
import ak.dev.khi_archive_platform.platform.dto.authimage.AuthImageResponseDTO;
import ak.dev.khi_archive_platform.platform.exceptions.AuthImageNotFoundException;
import ak.dev.khi_archive_platform.platform.model.authimage.AuthImage;
import ak.dev.khi_archive_platform.platform.repo.authimage.AuthImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthImageService {

    private static final String AUTH_IMAGE_FOLDER = "auth_image";

    private final AuthImageRepository authImageRepository;
    private final S3Service s3Service;

    public AuthImageResponseDTO create(MultipartFile file) {
        requireFile(file);
        String imageUrl = s3Service.upload(file, AUTH_IMAGE_FOLDER);
        AuthImage saved = authImageRepository.save(AuthImage.builder().imageUrl(imageUrl).build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthImageResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /** The image shown on the sign-in / register brand panel — latest row wins. */
    @Transactional(readOnly = true)
    public AuthImageResponseDTO getCurrent() {
        return authImageRepository.findTopByOrderByIdDesc()
                .map(this::toResponse)
                .orElseThrow(() -> new AuthImageNotFoundException("No auth image has been uploaded yet."));
    }

    public AuthImageResponseDTO update(Long id, MultipartFile file) {
        requireFile(file);
        AuthImage image = findOrThrow(id);
        String oldImageUrl = image.getImageUrl();

        image.setImageUrl(s3Service.upload(file, AUTH_IMAGE_FOLDER));
        AuthImage saved = authImageRepository.save(image);

        if (oldImageUrl != null && s3Service.isOurS3Url(oldImageUrl)) {
            s3Service.deleteFile(oldImageUrl);
        }
        return toResponse(saved);
    }

    public void delete(Long id) {
        AuthImage image = findOrThrow(id);
        authImageRepository.delete(image);
        if (image.getImageUrl() != null && s3Service.isOurS3Url(image.getImageUrl())) {
            s3Service.deleteFile(image.getImageUrl());
        }
    }

    private AuthImage findOrThrow(Long id) {
        return authImageRepository.findById(id)
                .orElseThrow(() -> new AuthImageNotFoundException("Auth image not found: " + id));
    }

    private void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Auth panel image file is required.");
        }
    }

    private AuthImageResponseDTO toResponse(AuthImage image) {
        return AuthImageResponseDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
