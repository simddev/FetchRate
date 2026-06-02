package com.fetchrate.adapters.http;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Replaces Spring Boot's Whitelabel error page for all error conditions.
 * Writing directly to {@link HttpServletResponse} bypasses Spring MVC content negotiation,
 * so JSON is always returned regardless of the client's {@code Accept} header.
 * This prevents HTML responses when a browser navigates directly to a REST endpoint.
 */
@Profile("http")
@Controller
@RequestMapping("/error")
public class JsonErrorController implements ErrorController {

    private final ObjectMapper objectMapper;

    public JsonErrorController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RequestMapping
    public void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (statusAttr instanceof Integer i) ? i : 500;

        String phrase;
        try {
            phrase = HttpStatus.valueOf(statusCode).getReasonPhrase();
        } catch (IllegalArgumentException e) {
            phrase = "Internal Server Error";
        }

        String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", statusCode);
        body.put("error", phrase);
        if (path != null) {
            body.put("path", path);
        }

        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
