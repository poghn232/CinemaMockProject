package com.example.superapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        // use getStatusCode() for ResponseStatusException
        var statusCode = ex.getStatusCode();
        // prefer getReason(); if absent, try to parse a quoted reason from the exception message
        String reason = ex.getReason();
        if (reason == null || reason.isBlank()) {
            String msg = ex.getMessage();
            if (msg != null) {
                int first = msg.indexOf('"');
                int last = msg.lastIndexOf('"');
                if (first >= 0 && last > first) {
                    reason = msg.substring(first + 1, last);
                } else {
                    // strip leading status text like "403 FORBIDDEN " if present
                    reason = msg.replaceFirst("^\\d+\\s+\\w+\\s*", "").trim();
                }
            }
        }
        if (reason == null) {
            // HttpStatusCode may not expose a reason phrase method; try to resolve to HttpStatus
            String rp = null;
            try {
                if (statusCode instanceof HttpStatus hs) {
                    rp = hs.getReasonPhrase();
                } else {
                    HttpStatus resolved = HttpStatus.resolve(statusCode.value());
                    if (resolved != null) rp = resolved.getReasonPhrase();
                }
            } catch (Exception ignored) {
            }
            if (rp == null || rp.isBlank()) rp = statusCode.toString();
            reason = rp;
        }
        Map<String, Object> body = Map.of(
                "message", reason,
                "timestamp", LocalDateTime.now().toString(),
                "status", statusCode.value()
        );
        return ResponseEntity.status(statusCode.value()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = Map.of(
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
