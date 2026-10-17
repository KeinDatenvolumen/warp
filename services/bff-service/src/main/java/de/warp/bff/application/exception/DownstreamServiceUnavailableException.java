package de.warp.bff.application.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    public DownstreamServiceUnavailableException() {
        super(
                "Der Identitätsdienst ist momentan nicht verfügbar. "
                        + "Bitte versuche es später erneut."
        );
    }

    public DownstreamServiceUnavailableException(
            Throwable cause
    ) {
        super(
                "Der Identitätsdienst ist momentan nicht verfügbar. "
                        + "Bitte versuche es später erneut.",
                cause
        );
    }
}