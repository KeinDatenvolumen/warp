// [KI-Log Nr. KI-078] (02bc37f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat WARP-006: add session persistence model
// [KI-Log Nr. KI-080] (093af46) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add session persistence model
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.repository;

import de.warp.identity.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    Optional<SessionEntity> findByTokenHash(String tokenHash);
}
