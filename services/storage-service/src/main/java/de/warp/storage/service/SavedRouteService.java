package de.warp.storage.service;

import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.SavedRoutePageResponse;
import de.warp.storage.dto.SavedRouteResponse;
import de.warp.storage.dto.UpdateSavedRouteRequest;
import de.warp.storage.dto.WeatherSegmentDto;
import de.warp.storage.entity.SavedRouteEntity;
import de.warp.storage.exceptions.InvalidSavedRouteRequestException;
import de.warp.storage.exceptions.SavedRouteNotFoundException;
import de.warp.storage.repository.SavedRouteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity Gemini 3.8 Flash High) für Routen-Persistenz & Ownership
@Service
public class SavedRouteService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    private final SavedRouteRepository repository;
    private final SavedRouteMapper mapper;
    private final Clock clock;

    public SavedRouteService(
            SavedRouteRepository repository,
            SavedRouteMapper mapper,
            Clock clock
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Bewusst ohne @Transactional: Eine Verletzung des Unique-Index beim
     * parallelen Wiederholen mit gleichem Idempotency-Key muss außerhalb der
     * fehlgeschlagenen Transaktion erneut gelesen werden können.
     */
    public SavedRouteCreationResult create(
            String userId,
            String idempotencyKey,
            CreateSavedRouteRequest request
    ) {
        validateIdempotencyKey(idempotencyKey);
        validateRequest(request);

        if (idempotencyKey != null) {
            Optional<SavedRouteEntity> existing =
                    repository.findByUserIdAndIdempotencyKey(
                            userId,
                            idempotencyKey
                    );

            if (existing.isPresent()) {
                return replay(existing.get());
            }
        }

        SavedRouteEntity entity = mapper.toNewEntity(
                generateRouteId(),
                userId,
                idempotencyKey,
                request,
                clock.instant()
        );

        try {
            SavedRouteEntity saved = repository.saveAndFlush(entity);

            return new SavedRouteCreationResult(
                    mapper.toResponse(saved),
                    true
            );
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyKey != null) {
                Optional<SavedRouteEntity> existing =
                        repository.findByUserIdAndIdempotencyKey(
                                userId,
                                idempotencyKey
                        );

                if (existing.isPresent()) {
                    return replay(existing.get());
                }
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public SavedRoutePageResponse list(
            String userId,
            Boolean favorite,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidSavedRouteRequestException(
                    "page",
                    "INVALID_PAGE",
                    "Die Seitennummer darf nicht negativ sein."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidSavedRouteRequestException(
                    "size",
                    "INVALID_PAGE_SIZE",
                    "Die Seitengröße muss zwischen 1 und "
                            + MAX_PAGE_SIZE + " liegen."
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "favorite")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<SavedRouteEntity> result = favorite == null
                ? repository.findByUserId(userId, pageable)
                : repository.findByUserIdAndFavorite(
                userId,
                favorite,
                pageable
        );

        return mapper.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public SavedRouteResponse get(String userId, String routeId) {
        return mapper.toResponse(findOwnedRoute(userId, routeId));
    }

    @Transactional
    public SavedRouteResponse update(
            String userId,
            String routeId,
            UpdateSavedRouteRequest request
    ) {
        if (request.name() == null && request.favorite() == null) {
            throw new InvalidSavedRouteRequestException(
                    "body",
                    "AT_LEAST_ONE_FIELD_REQUIRED",
                    "Mindestens ein Feld muss geändert werden."
            );
        }

        SavedRouteEntity entity = findOwnedRoute(userId, routeId);
        Instant now = clock.instant();

        if (request.name() != null) {
            entity.rename(request.name(), now);
        }

        if (request.favorite() != null) {
            entity.changeFavorite(request.favorite(), now);
        }

        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(String userId, String routeId) {
        if (repository.deleteOwnedRoute(routeId, userId) == 0) {
            throw new SavedRouteNotFoundException();
        }
    }

    private SavedRouteEntity findOwnedRoute(String userId, String routeId) {
        return repository.findByIdAndUserId(routeId, userId)
                .orElseThrow(SavedRouteNotFoundException::new);
    }

    private SavedRouteCreationResult replay(SavedRouteEntity existing) {
        return new SavedRouteCreationResult(
                mapper.toResponse(existing),
                false
        );
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey != null
                && !IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new InvalidSavedRouteRequestException(
                    "Idempotency-Key",
                    "INVALID_IDEMPOTENCY_KEY",
                    "Der Idempotency-Key ist ungültig."
            );
        }
    }

    private void validateRequest(CreateSavedRouteRequest request) {
        if (request.routeSnapshot().arrivalTime()
                .isBefore(request.routeSnapshot().departureTime())) {
            throw new InvalidSavedRouteRequestException(
                    "routeSnapshot.arrivalTime",
                    "MUST_NOT_BE_BEFORE_DEPARTURE",
                    "Die Ankunftszeit darf nicht vor der Abfahrtszeit liegen."
            );
        }

        Set<Integer> indexes = new HashSet<>();

        for (WeatherSegmentDto segment
                : request.weatherSnapshot().segments()) {
            if (!indexes.add(segment.segmentIndex())) {
                throw new InvalidSavedRouteRequestException(
                        "weatherSnapshot.segments",
                        "DUPLICATE_SEGMENT_INDEX",
                        "Abschnittsindizes müssen eindeutig sein."
                );
            }
        }
    }

    private String generateRouteId() {
        return "route_" + UUID.randomUUID().toString().replace("-", "");
    }
}