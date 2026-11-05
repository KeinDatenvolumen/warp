// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.repository;

import de.warp.identity.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    Optional<SessionEntity> findByTokenHash(String tokenHash);
}
