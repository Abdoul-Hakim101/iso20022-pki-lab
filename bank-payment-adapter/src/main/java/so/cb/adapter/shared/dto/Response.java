package so.cb.adapter.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Response (String time, String path, int code, String message, HttpStatus status, Map<?,?> data, String exception) {
}
