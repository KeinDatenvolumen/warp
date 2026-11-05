// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.api;

import de.warp.weather.application.WeatherEvaluationNotFoundException;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoRateLimitException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoTimeoutException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WeatherExceptionHandler {

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
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Der externe Wetterdienst hat keine gültigen Daten geliefert."
        );
        problem.setTitle("Wetterdienst nicht verfügbar");
        problem.setProperty("code", "WEATHER_PROVIDER_UNAVAILABLE");
        return problem;
    }
}
