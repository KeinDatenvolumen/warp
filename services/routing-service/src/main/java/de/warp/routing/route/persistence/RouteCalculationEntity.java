// [KI-Log Nr. KI-156] (7fb0636) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 persist route calculations
// [KI-Log Nr. KI-158] (e710e2c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 support idempotent route calculation requests
// [KI-Log Nr. KI-172] (45536d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 persist route calculations
// [KI-Log Nr. KI-174] (0d8cf61) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 support idempotent route calculation requests
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.persistence;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.route.domain.RouteCalculationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "route_calculations",
        schema = "warp_routing"
)
public class RouteCalculationEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            length = 40
    )
    private String id;

    @Column(
            name = "requested_by_user_id",
            nullable = false,
            length = 40
    )
    private String requestedByUserId;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 128
    )
    private String idempotencyKey;

    @Column(
            name = "start_label",
            nullable = false,
            length = 255
    )
    private String startLabel;

    @Column(
            name = "start_latitude",
            nullable = false
    )
    private double startLatitude;

    @Column(
            name = "start_longitude",
            nullable = false
    )
    private double startLongitude;

    @Column(
            name = "destination_label",
            nullable = false,
            length = 255
    )
    private String destinationLabel;

    @Column(
            name = "destination_latitude",
            nullable = false
    )
    private double destinationLatitude;

    @Column(
            name = "destination_longitude",
            nullable = false
    )
    private double destinationLongitude;

    @Column(
            name = "departure_time",
            nullable = false
    )
    private Instant departureTime;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_profile_code",
            nullable = false,
            length = 32
    )
    private ActivityProfileCode activityProfileCode;

    @Column(
            name = "routing_profile",
            nullable = false,
            length = 64
    )
    private String routingProfile;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private RouteCalculationStatus status;

    @Column(name = "arrival_time")
    private Instant arrivalTime;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "geometry",
            columnDefinition = "jsonb"
    )
    private String geometry;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "segments",
            columnDefinition = "jsonb"
    )
    private String segments;

    @Column(
            name = "error_code",
            length = 64
    )
    private String errorCode;

    @Column(
            name = "error_message",
            length = 500
    )
    private String errorMessage;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected RouteCalculationEntity() {
        // Wird von JPA benötigt.
    }

    public RouteCalculationEntity(
            String id,
            String requestedByUserId,
            String idempotencyKey,
            String startLabel,
            double startLatitude,
            double startLongitude,
            String destinationLabel,
            double destinationLatitude,
            double destinationLongitude,
            Instant departureTime,
            ActivityProfileCode activityProfileCode,
            String routingProfile,
            Instant createdAt
    ) {
        this.id = id;
        this.requestedByUserId = requestedByUserId;
        this.idempotencyKey = idempotencyKey;
        this.startLabel = startLabel;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.destinationLabel = destinationLabel;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.departureTime = departureTime;
        this.activityProfileCode = activityProfileCode;
        this.routingProfile = routingProfile;
        this.status = RouteCalculationStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void markProcessing(Instant updatedAt) {
        requireStatus(RouteCalculationStatus.PENDING);

        status = RouteCalculationStatus.PROCESSING;
        this.updatedAt = updatedAt;
    }

    public void complete(
            Instant arrivalTime,
            double distanceMeters,
            double durationSeconds,
            String geometry,
            String segments,
            Instant updatedAt
    ) {
        requireStatus(RouteCalculationStatus.PROCESSING);

        this.status = RouteCalculationStatus.COMPLETED;
        this.arrivalTime = arrivalTime;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.geometry = geometry;
        this.segments = segments;
        this.errorCode = null;
        this.errorMessage = null;
        this.updatedAt = updatedAt;
    }

    public void fail(
            String errorCode,
            String errorMessage,
            Instant updatedAt
    ) {
        if (status != RouteCalculationStatus.PENDING
                && status != RouteCalculationStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Nur eine offene Routenberechnung "
                            + "kann fehlschlagen."
            );
        }

        this.status = RouteCalculationStatus.FAILED;
        this.arrivalTime = null;
        this.distanceMeters = null;
        this.durationSeconds = null;
        this.geometry = null;
        this.segments = null;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.updatedAt = updatedAt;
    }

    private void requireStatus(
            RouteCalculationStatus requiredStatus
    ) {
        if (status != requiredStatus) {
            throw new IllegalStateException(
                    "Ungültiger Statusübergang von "
                            + status
                            + ". Erwartet wurde "
                            + requiredStatus
                            + "."
            );
        }
    }

    public String getId() {
        return id;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getStartLabel() {
        return startLabel;
    }

    public double getStartLatitude() {
        return startLatitude;
    }

    public double getStartLongitude() {
        return startLongitude;
    }

    public String getDestinationLabel() {
        return destinationLabel;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public ActivityProfileCode getActivityProfileCode() {
        return activityProfileCode;
    }

    public String getRoutingProfile() {
        return routingProfile;
    }

    public RouteCalculationStatus getStatus() {
        return status;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public String getGeometry() {
        return geometry;
    }

    public String getSegments() {
        return segments;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}