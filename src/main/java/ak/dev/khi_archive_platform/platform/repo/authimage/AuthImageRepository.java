package ak.dev.khi_archive_platform.platform.repo.authimage;

import ak.dev.khi_archive_platform.platform.model.authimage.AuthImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthImageRepository extends JpaRepository<AuthImage, Long> {

    /** The "current" auth panel image — the most recently created row. */
    Optional<AuthImage> findTopByOrderByIdDesc();
}
