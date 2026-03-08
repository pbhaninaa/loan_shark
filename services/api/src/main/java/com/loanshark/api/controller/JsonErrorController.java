package com.loanshark.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Ensures /error always returns JSON so API clients (e.g. frontend) get a parseable
 * body with a "message" field instead of HTML.
 */
@Controller
public class JsonErrorController implements ErrorController {

    private static final String PATH = "/error";

    @RequestMapping(value = PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());

        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        Exception ex = (Exception) request.getAttribute("jakarta.servlet.error.exception");

        if (statusCode == null) {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        if (message == null || message.isBlank()) {
            message = ex != null && ex.getMessage() != null
                ? ex.getMessage()
                : "An error occurred. Please try again.";
        }
        body.put("message", message);
        body.put("status", statusCode);

        return ResponseEntity.status(statusCode).body(body);
    }

    public String getErrorPath() {
        return PATH;
    }
}
