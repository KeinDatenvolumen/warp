// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
package de.warp.bff.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.warp.bff.api.dto.SystemHealthResponse;
import de.warp.bff.application.service.SystemHealthService;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    public SystemHealthController(
            SystemHealthService systemHealthService
    ) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/health")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(systemHealthService.getSystemHealth());
    }
}