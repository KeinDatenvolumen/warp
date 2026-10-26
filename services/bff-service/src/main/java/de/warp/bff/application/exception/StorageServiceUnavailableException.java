package de.warp.bff.application.exception;

public class StorageServiceUnavailableException extends RuntimeException {

    public StorageServiceUnavailableException(Throwable cause) {
        super("Der Speicherdienst ist momentan nicht verfügbar.", cause);
    }
}