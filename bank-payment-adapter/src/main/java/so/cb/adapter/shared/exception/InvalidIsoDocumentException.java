package so.cb.adapter.shared.exception;

public class InvalidIsoDocumentException extends ApiException {
    public InvalidIsoDocumentException(String message) {
        super(message);
    }

    public InvalidIsoDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
