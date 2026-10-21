package de.warp.routing.infrastructure.routing;

public class RoutingServiceUnavailableException
        extends RuntimeException {

    public RoutingServiceUnavailableException(String message) {
        super(message);
    }

    public RoutingServiceUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}