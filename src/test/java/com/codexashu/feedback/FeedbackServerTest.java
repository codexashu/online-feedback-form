package com.codexashu.feedback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackServerTest {

    private FeedbackServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = new FeedbackServer(0);
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + server.getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void rootServesFeedbackFormWithRequiredFields() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        String html = resp.body();
        assertTrue(html.contains("<form"), "Missing form element");
        assertTrue(html.contains("name=\"name\""), "Missing Name field");
        assertTrue(html.contains("name=\"email\""), "Missing Email field");
        assertTrue(html.contains("name=\"category\""), "Missing Feedback Category field");
        assertTrue(html.contains("name=\"rating\""), "Missing Rating field");
        assertTrue(html.contains("name=\"comments\""), "Missing Comments field");
        assertTrue(html.contains("type=\"submit\""), "Missing Submit button");
    }

    @Test
    void submitValidFeedbackStoresInMemoryAndReturnsSuccess() throws Exception {
        String body = "name=Ashutosh+Pandey&email=ashutosh%40example.com&category=Website+%26+UI&rating=5&comments=Great+experience";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/submit"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"status\":\"ok\""));
        assertEquals(1, server.getSubmissions().size());
        assertEquals("Ashutosh Pandey", server.getSubmissions().get(0).name());
        assertEquals(5, server.getSubmissions().get(0).rating());
    }

    @Test
    void submitMissingFieldsReturnsBadRequest() throws Exception {
        String body = "name=Ashutosh&email=&category=Bug+Report&rating=4&comments=";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/submit"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        assertEquals(0, server.getSubmissions().size());
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/health")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"status\":\"UP\""));
    }
}
