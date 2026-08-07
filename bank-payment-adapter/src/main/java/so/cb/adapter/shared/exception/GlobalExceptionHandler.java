package so.cb.adapter.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import so.cb.adapter.shared.dto.Response;
import so.cb.adapter.shared.util.RequestUtils;

import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(XmlSignatureException.class)
    public ResponseEntity<Response> handleXmlSignatureException(XmlSignatureException ex, HttpServletRequest request) {
        log.error("XmlSignatureException at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponseEntity(request, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(InvalidIsoDocumentException.class)
    public ResponseEntity<Response> handleInvalidIsoDocumentException(InvalidIsoDocumentException ex, HttpServletRequest request) {
        log.warn("InvalidIsoDocumentException at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponseEntity(request, ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("IllegalArgumentException at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponseEntity(request, ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Response> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.error("IllegalStateException at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponseEntity(request, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponseEntity(request, ex.getMessage(), HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Response> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API Exception at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponseEntity(request, ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Response> handleMissingRequestHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String message = "Required HTTP header is missing: " + ex.getHeaderName();
        log.warn("MissingRequestHeaderException at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), message);
        return buildResponseEntity(request, message, HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), errors);
        return buildResponseEntity(request, errors, HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponseEntity(request, "An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    private ResponseEntity<Response> buildResponseEntity(HttpServletRequest request, String message, HttpStatus status, Exception ex) {
        Response response = RequestUtils.getResponse(
                request,
                Collections.emptyMap(),
                message,
                status,
                ex.getClass().getSimpleName()
        );
        return new ResponseEntity<>(response, status);
    }
}
