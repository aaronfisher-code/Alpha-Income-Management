package services;

/** Indicates that a screen configured for live Z data could not reach its agent. */
public class LiveZUnavailableException extends RuntimeException {
    public LiveZUnavailableException(String message) {
        super(message);
    }

    public LiveZUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
