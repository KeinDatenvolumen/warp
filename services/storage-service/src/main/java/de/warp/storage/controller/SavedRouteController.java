// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.controller;

import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.SavedRoutePageResponse;
import de.warp.storage.dto.SavedRouteResponse;
import de.warp.storage.dto.UpdateSavedRouteRequest;
import de.warp.storage.service.SavedRouteCreationResult;
import de.warp.storage.service.SavedRouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users/{userId}/saved-routes")
public class SavedRouteController {

    private final SavedRouteService savedRouteService;

    public SavedRouteController(SavedRouteService savedRouteService) {
        this.savedRouteService = savedRouteService;
    }

    @PostMapping
    public ResponseEntity<SavedRouteResponse> create(
            @PathVariable("userId") String userId,
            @RequestHeader(name = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody CreateSavedRouteRequest request
    ) {
        SavedRouteCreationResult result =
                savedRouteService.create(userId, idempotencyKey, request);

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(result.route());
    }

    @GetMapping
    public SavedRoutePageResponse list(
            @PathVariable("userId") String userId,
            @RequestParam(name = "favorite", required = false)
            Boolean favorite,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return savedRouteService.list(userId, favorite, page, size);
    }

    @GetMapping("/{routeId}")
    public SavedRouteResponse get(
            @PathVariable("userId") String userId,
            @PathVariable("routeId") String routeId
    ) {
        return savedRouteService.get(userId, routeId);
    }

    @PatchMapping("/{routeId}")
    public SavedRouteResponse update(
            @PathVariable("userId") String userId,
            @PathVariable("routeId") String routeId,
            @Valid @RequestBody UpdateSavedRouteRequest request
    ) {
        return savedRouteService.update(userId, routeId, request);
    }

    @DeleteMapping("/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("userId") String userId,
            @PathVariable("routeId") String routeId
    ) {
        savedRouteService.delete(userId, routeId);
    }
}