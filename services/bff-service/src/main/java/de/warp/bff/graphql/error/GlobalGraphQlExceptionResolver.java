package de.warp.bff.graphql.error;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import de.warp.bff.graphql.SessionInvalidException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import java.util.Map;
import de.warp.bff.infrastructure.geocoding.GeocodingServiceUnavailableException;
import de.warp.bff.application.exception
        .InvalidRouteRequestException;
import de.warp.bff.application.exception
        .RouteCalculationNotFoundException;
import de.warp.bff.application.exception
        .RoutingServiceUnavailableException;

@Component
@Order()
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
            @NonNull Throwable exception,
            @NonNull DataFetchingEnvironment environment
    ) {
        switch (exception) {
            case SessionInvalidException sessionInvalidException -> {
                return createError(
                        environment,
                        ErrorType.UNAUTHORIZED,
                        GraphQlErrorCode.SESSION_INVALID,
                        SESSION_INVALID_MESSAGE
                );
            }
            case InvalidRouteRequestException invalidRouteRequestException -> {
                return createError(
                        environment,
                        ErrorType.BAD_REQUEST,
                        GraphQlErrorCode.INVALID_ROUTE_REQUEST,
                        "Die Routenanfrage ist ungültig."
                );
            }
            case de.warp.bff.application.exception.EmailNotVerifiedException emailNotVerifiedException -> {
                return createError(
                        environment,
                        ErrorType.FORBIDDEN,
                        GraphQlErrorCode.EMAIL_NOT_VERIFIED,
                        "Die E-Mail-Adresse ist noch nicht verifiziert."
                );
            }
            case RouteCalculationNotFoundException routeCalculationNotFoundException -> {
                return createError(
                        environment,
                        ErrorType.NOT_FOUND,
                        GraphQlErrorCode.ROUTE_CALCULATION_NOT_FOUND,
                        "Die Routenberechnung wurde nicht gefunden."
                );
            }
            case RoutingServiceUnavailableException routingServiceUnavailableException -> {
                LOGGER.warn(
                        "Der Routing-Dienst ist nicht verfügbar.",
                        exception
                );

                return createError(
                        environment,
                        ErrorType.INTERNAL_ERROR,
                        GraphQlErrorCode.ROUTING_SERVICE_UNAVAILABLE,
                        "Die Routenberechnung ist momentan "
                                + "nicht verfügbar."
                );
            }
            case IllegalArgumentException illegalArgumentException -> {
                return createError(
                        environment,
                        ErrorType.BAD_REQUEST,
                        GraphQlErrorCode.BAD_REQUEST,
                        BAD_REQUEST_MESSAGE
                );
            }
            case GeocodingServiceUnavailableException geocodingServiceUnavailableException -> {
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
            default -> {
            }
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
                .extensions(Map.of(
                        "code",
                        errorCode.name()
                ))
                .build();
    }
}