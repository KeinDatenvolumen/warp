// [KI-Log Nr. KI-082] (d939743) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: implement session creation service
// [KI-Log Nr. KI-173] (fd1cf9e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement route calculation workflow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.infrastructure.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}