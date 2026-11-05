// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.bff.application.exception;

public class StorageServiceUnavailableException extends RuntimeException {

    public StorageServiceUnavailableException(Throwable cause) {
        super("Der Speicherdienst ist momentan nicht verfügbar.", cause);
    }
}