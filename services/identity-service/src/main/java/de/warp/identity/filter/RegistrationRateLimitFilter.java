package de.warp.identity.filter;

import tools.jackson.databind.ObjectMapper;
import de.warp.identity.dto.ApiErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RegistrationRateLimitFilter extends OncePerRequestFilter {

    private static final int REQUEST_LIMIT = 10;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RegistrationRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().endsWith("/registrations")
                || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientAddress = request.getRemoteAddr();

        Bucket bucket = buckets.computeIfAbsent(
                clientAddress,
                ignored -> createBucket()
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(
                    "X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );

            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
                1,
                Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds()
        );

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "TOO_MANY_REQUESTS",
                "Zu viele Registrierungsversuche. Bitte versuche es später erneut.",
                List.of()
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                String.valueOf(retryAfterSeconds)
        );
        response.setHeader("X-Rate-Limit-Remaining", "0");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REQUEST_LIMIT)
                .refillIntervally(
                        REQUEST_LIMIT,
                        Duration.ofMinutes(1)
                )
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
