package ak.dev.khi_archive_platform.platform.repo.sitefont;

import ak.dev.khi_archive_platform.platform.model.sitefont.SiteFont;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteFontRepository extends JpaRepository<SiteFont, Long> {

    Optional<SiteFont> findByActiveTrue();

    List<SiteFont> findAllByOrderByCreatedAtDesc();
}
