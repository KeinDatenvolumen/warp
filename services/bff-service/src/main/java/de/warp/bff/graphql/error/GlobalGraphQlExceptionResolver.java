package de.warp.bff.graphql.error;

import de.warp.bff.application.exception.EmailNotVerifiedException;
import de.warp.bff.application.exception.InvalidRouteRequestException;
import de.warp.bff.application.exception.RouteCalculationNotFoundException;
import de.warp.bff.application.exception.RoutingServiceUnavailableException;
import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;
import de.warp.bff.application.exception.StorageValidationException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.geocoding.GeocodingServiceUnavailableException;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order()
public class GlobalGraphQlExceptionResolver
        extends DataFetcherExceptionResolverAdapter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GlobalGraphQlExceptionResolver.class
            );

    private static final String INTERNAL_ERROR_MESSAGE =
            "Ein interner Fehler ist aufgetreten.";

    private static final String BAD_REQUEST_MESSAGE =
            "Die Anfrage ist ungültig.";

    private static final String SESSION_INVALID_MESSAGE =
            "Die Sitzung ist ungültig oder abgelaufen.";

    private static final String EMAIL_NOT_VERIFIED_MESSAGE =
            "Die E-Mail-Adresse ist noch nicht verifiziert.";

    private static final String INVALID_ROUTE_REQUEST_MESSAGE =
            "Die Routenanfrage ist ungültig.";

    private static final String ROUTE_CALCULATION_NOT_FOUND_MESSAGE =
            "Die Routenberechnung wurde nicht gefunden.";

    private static final String SAVED_ROUTE_NOT_FOUND_MESSAGE =
            "Die gespeicherte Route wurde nicht gefunden.";

    private static final String ROUTING_SERVICE_UNAVAILABLE_MESSAGE =
            "Die Routenberechnung ist momentan nicht verfügbar.";

    private static final String STORAGE_SERVICE_UNAVAILABLE_MESSAGE =
            "Der Speicherdienst ist momentan nicht verfügbar.";

    private static final String GEOCODING_SERVICE_UNAVAILABLE_MESSAGE =
            "Die Ortssuche ist momentan nicht verfügbar.";

    @Override
    protected GraphQLError resolveToSingleError(
            @NonNull Throwable exception,
            @NonNull DataFetchingEnvironment environment
    ) {
        return switch (exception) {
            case SessionInvalidException ignored -> createError(
                    environment,
                    ErrorType.UNAUTHORIZED,
                    GraphQlErrorCode.SESSION_INVALID,
                    SESSION_INVALID_MESSAGE
            );

            case EmailNotVerifiedException ignored -> createError(
                    environment,
                    ErrorType.FORBIDDEN,
                    GraphQlErrorCode.EMAIL_NOT_VERIFIED,
                    EMAIL_NOT_VERIFIED_MESSAGE
            );

            case InvalidRouteRequestException ignored -> createError(
                    environment,
                    ErrorType.BAD_REQUEST,
                    GraphQlErrorCode.INVALID_ROUTE_REQUEST,
                    INVALID_ROUTE_REQUEST_MESSAGE
            );

            case StorageValidationException ignored -> createError(
                    environment,
                    ErrorType.BAD_REQUEST,
                    GraphQlErrorCode.BAD_REQUEST,
                    BAD_REQUEST_MESSAGE
            );

            case RouteCalculationNotFoundException ignored -> createError(
                    environment,
                    ErrorType.NOT_FOUND,
                    GraphQlErrorCode.ROUTE_CALCULATION_NOT_FOUND,
                    ROUTE_CALCULATION_NOT_FOUND_MESSAGE
            );

            case SavedRouteNotFoundException ignored -> createError(
                    environment,
                    ErrorType.NOT_FOUND,
                    GraphQlErrorCode.SAVED_ROUTE_NOT_FOUND,
                    SAVED_ROUTE_NOT_FOUND_MESSAGE
            );

            case RoutingServiceUnavailableException ignored -> {
                LOGGER.warn(
                        "Der Routing-Dienst ist nicht verfügbar.",
                        exception
                );

                yield createError(
                        environment,
                        ErrorType.INTERNAL_ERROR,
                        GraphQlErrorCode.ROUTING_SERVICE_UNAVAILABLE,
                        ROUTING_SERVICE_UNAVAILABLE_MESSAGE
                );
            }

            case StorageServiceUnavailableException ignored -> {
                LOGGER.warn(
                        "Der Speicherdienst ist nicht verfügbar.",
                        exception
                );

                yield createError(
                        environment,
                        ErrorType.INTERNAL_ERROR,
                        GraphQlErrorCode.DOWNSTREAM_SERVICE_UNAVAILABLE,
                        STORAGE_SERVICE_UNAVAILABLE_MESSAGE
                );
            }

            case GeocodingServiceUnavailableException ignored -> {
                LOGGER.warn(
                        "Der Geocoding-Dienst ist nicht verfügbar.",
                        exception
                );

                yield createError(
                        environment,
                        ErrorType.INTERNAL_ERROR,
                        GraphQlErrorCode.GEOCODING_SERVICE_UNAVAILABLE,
                        GEOCODING_SERVICE_UNAVAILABLE_MESSAGE
                );
            }

            case DownstreamServiceUnavailableException ignored -> {
                LOGGER.warn(
                        "Der Identitätsdienst ist nicht verfügbar.",
                        exception
                );

                yield createError(
                        environment,
                        ErrorType.INTERNAL_ERROR,
                        GraphQlErrorCode.DOWNSTREAM_SERVICE_UNAVAILABLE,
                        "Der Identitätsdienst ist momentan nicht verfügbar."
                );
            }

            case IllegalArgumentException ignored -> createError(
                    environment,
                    ErrorType.BAD_REQUEST,
                    GraphQlErrorCode.BAD_REQUEST,
                    BAD_REQUEST_MESSAGE
            );

            default -> {
                LOGGER.error(
                        "Unbehandelte Exception bei der GraphQL-Ausführung.",
                        exception
                );

                yield createError(
                        environment,
                        ErrorType.INTERNAL_ERROR,
                        GraphQlErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_ERROR_MESSAGE
                );
            }
        };
    }

    private GraphQLError createError(
            DataFetchingEnvironment environment,
            ErrorType errorType,
            GraphQlErrorCode errorCode,
            String message
    ) {
        return GraphqlErrorBuilder
                .newError(environment)
                .errorType(errorType)
                .message(message)
                .extensions(Map.of(
                        "code",
                        errorCode.name()
                ))
                .build();
    }
}