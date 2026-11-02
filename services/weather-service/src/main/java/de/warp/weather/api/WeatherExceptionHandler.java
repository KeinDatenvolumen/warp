package de.warp.weather.api;

import de.warp.weather.application.WeatherEvaluationNotFoundException;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoRateLimitException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoTimeoutException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WeatherExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problem.setTitle("Ungültige Wetteranfrage");
        problem.setProperty("code", "INVALID_WEATHER_REQUEST");
        return problem;
    }

    @ExceptionHandler(WeatherEvaluationNotFoundException.class)
    public ProblemDetail handleNotFound(WeatherEvaluationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Wetterbewertung nicht gefunden");
        problem.setProperty("code", "WEATHER_EVALUATION_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler({
            InvalidOpenMeteoResponseException.class,
            OpenMeteoUnavailableException.class,
            OpenMeteoTimeoutException.class,
            OpenMeteoRateLimitException.class
    })
    public ProblemDetail handleOpenMeteoFailure(RuntimeException exception) {
        LOGGER.warn("External weather provider failed: {}", exception.getMessage(), exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Der externe Wetterdienst hat keine gültigen Daten geliefert."
        );
        problem.setTitle("Wetterdienst nicht verfügbar");
        problem.setProperty("code", "WEATHER_PROVIDER_UNAVAILABLE");
        return problem;
    }
}
