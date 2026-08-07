package so.cb.adapter.shared.exception;

public class XmlSignatureException extends ApiException {
    public XmlSignatureException(String message) {
        super(message);
    }

    public XmlSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
