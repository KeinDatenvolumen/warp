package de.warp.bff.graphql.error;

import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.geocoding.GeocodingServiceUnavailableException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalGraphQlExceptionResolver
        extends DataFetcherExceptionResolverAdapter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalGraphQlExceptionResolver.class);

    private static final String INTERNAL_ERROR_MESSAGE =
            "Ein interner Fehler ist aufgetreten.";

    private static final String BAD_REQUEST_MESSAGE =
            "Die Anfrage ist ungültig.";

    private static final String SESSION_INVALID_MESSAGE =
            "Die Sitzung ist ungültig oder abgelaufen.";

    @Override
    protected GraphQLError resolveToSingleError(
            Throwable exception,
            DataFetchingEnvironment environment
    ) {
        if (exception instanceof SessionInvalidException) {
            return createError(
                    environment,
                    ErrorType.UNAUTHORIZED,
                    GraphQlErrorCode.SESSION_INVALID,
                    SESSION_INVALID_MESSAGE
            );
        }
        if (exception instanceof IllegalArgumentException) {
            return createError(
                    environment,
                    ErrorType.BAD_REQUEST,
                    GraphQlErrorCode.BAD_REQUEST,
                    BAD_REQUEST_MESSAGE
            );
        }
        if (exception instanceof SavedRouteNotFoundException) {
            return createError(
                    environment,
                    ErrorType.NOT_FOUND,
                    GraphQlErrorCode.SAVED_ROUTE_NOT_FOUND,
                    "Die gespeicherte Route wurde nicht gefunden."
            );
        }
        if (exception instanceof StorageServiceUnavailableException) {
            LOGGER.warn(
                    "Der Speicherdienst ist nicht verfügbar.",
                    exception
            );

            return createError(
                    environment,
                    ErrorType.INTERNAL_ERROR,
                    GraphQlErrorCode.DOWNSTREAM_SERVICE_UNAVAILABLE,
                    "Der Speicherdienst ist momentan nicht verfügbar."
            );
        }
        if (exception instanceof GeocodingServiceUnavailableException) {
            LOGGER.warn(
                    "Der Geocoding-Dienst ist nicht verfügbar.",
                    exception
            );

            return createError(
                    environment,
                    ErrorType.INTERNAL_ERROR,
                    GraphQlErrorCode.GEOCODING_SERVICE_UNAVAILABLE,
                    "Die Ortssuche ist momentan nicht verfügbar."
            );
        }

        LOGGER.error(
                "Unbehandelte Exception bei der GraphQL-Ausführung.",
                exception
        );

        return createError(
                environment,
                ErrorType.INTERNAL_ERROR,
                GraphQlErrorCode.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_MESSAGE
        );
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
                .extensions(Map.of("code", errorCode.name()))
                .build();
    }
}