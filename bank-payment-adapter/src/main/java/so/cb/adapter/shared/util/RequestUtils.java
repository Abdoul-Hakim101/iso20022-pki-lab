package so.cb.adapter.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import so.cb.adapter.shared.dto.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.apache.logging.log4j.util.Strings.EMPTY;

public class RequestUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return getResponse(request, data, message, status, EMPTY);
    }

    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status, String exception) {
        String time = LocalDateTime.now().format(FORMATTER);
        String path = request.getRequestURI();
        int code = status.value();

        return new Response(time, path, code, message, status, data, exception);
    }
}