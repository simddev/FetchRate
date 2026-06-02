package com.fetchrate.adapters.http;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JsonErrorControllerTest {

    private JsonErrorController controller;

    @BeforeEach
    void setUp() {
        controller = new JsonErrorController(new ObjectMapper());
    }

    @Test
    void handleError_404_returnsJsonWith404() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/nonexistent");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.handleError(request, response);

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":404"));
        assertTrue(body.contains("Not Found"));
        assertTrue(body.contains("/nonexistent"));
    }

    @Test
    void handleError_406_returnsJsonWithoutHtml() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 406);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/convert");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.handleError(request, response);

        assertEquals(406, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":406"));
        assertFalse(body.contains("<html>"));
        assertFalse(body.contains("Whitelabel"));
    }

    @Test
    void handleError_405_returnsJsonWithMethodNotAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 405);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/convert");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.handleError(request, response);

        assertEquals(405, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("Method Not Allowed"));
    }

    @Test
    void handleError_noStatusAttribute_defaults500() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.handleError(request, response);

        assertEquals(500, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
    }

    @Test
    void handleError_noPathAttribute_omitsPathField() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.handleError(request, response);

        String body = response.getContentAsString();
        assertFalse(body.contains("\"path\""));
    }

    @Test
    void handleError_responseIsValidJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/convert");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.handleError(request, response);

        // Parsing the body with Jackson should succeed without exceptions
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(response.getContentAsString()));
    }
}
