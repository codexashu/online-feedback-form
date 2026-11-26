package com.codexashu.feedback;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Minimal embedded HTTP server serving the Online Feedback HTML form
 * and handling form submissions in memory without a database.
 */
public class FeedbackServer {

    public record FeedbackEntry(
            String name,
            String email,
            String category,
            int rating,
            String comments,
            String submittedAt
    ) {}

    private final HttpServer server;
    private final List<FeedbackEntry> submissions = new CopyOnWriteArrayList<>();

    public FeedbackServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(4));
        registerRoutes();
    }

    private void registerRoutes() {
        server.createContext("/", this::handleFormPage);
        server.createContext("/index.html", this::handleFormPage);
        server.createContext("/submit", this::handleSubmit);
        server.createContext("/api/feedback", this::handleListFeedback);
        server.createContext("/health", this::handleHealth);
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public List<FeedbackEntry> getSubmissions() {
        return List.copyOf(submissions);
    }

    private void handleFormPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
            return;
        }
        String html = loadIndexHtml();
        sendText(exchange, 200, "text/html; charset=UTF-8", html);
    }

    private void handleSubmit(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
            return;
        }

        String rawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormBody(rawBody);

        String name = trimOrEmpty(params.get("name"));
        String email = trimOrEmpty(params.get("email"));
        String category = trimOrEmpty(params.get("category"));
        String ratingStr = trimOrEmpty(params.get("rating"));
        String comments = trimOrEmpty(params.get("comments"));

        boolean wantsJson = expectsJson(exchange);

        if (name.isEmpty() || email.isEmpty() || category.isEmpty() || ratingStr.isEmpty() || comments.isEmpty()) {
            sendError(exchange, 400, "All feedback fields are required.", wantsJson);
            return;
        }

        int rating;
        try {
            rating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Rating must be a number between 1 and 5.", wantsJson);
            return;
        }

        if (rating < 1 || rating > 5) {
            sendError(exchange, 400, "Rating must be between 1 and 5.", wantsJson);
            return;
        }

        FeedbackEntry entry = new FeedbackEntry(
                name,
                email,
                category,
                rating,
                comments,
                Instant.now().toString()
        );
        submissions.add(entry);

        if (wantsJson) {
            String json = String.format(
                    "{\"status\":\"ok\",\"name\":\"%s\",\"email\":\"%s\",\"category\":\"%s\",\"rating\":%d,\"comments\":\"%s\",\"submittedAt\":\"%s\"}",
                    escapeJson(entry.name()),
                    escapeJson(entry.email()),
                    escapeJson(entry.category()),
                    entry.rating(),
                    escapeJson(entry.comments()),
                    escapeJson(entry.submittedAt())
            );
            sendText(exchange, 200, "application/json; charset=UTF-8", json);
            return;
        }

        String confirmationHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Feedback Received</title></head>
                <body style="font-family:sans-serif;max-width:560px;margin:40px auto;padding:24px;border:1px solid #cbd5e1;border-radius:12px;">
                  <h2>Thank you for your feedback!</h2>
                  <p><strong>Name:</strong> %s</p>
                  <p><strong>Email:</strong> %s</p>
                  <p><strong>Category:</strong> %s</p>
                  <p><strong>Rating:</strong> %d / 5</p>
                  <p><strong>Comments:</strong> %s</p>
                  <a href="/">Submit another response</a>
                </body>
                </html>
                """.formatted(
                escapeHtml(entry.name()),
                escapeHtml(entry.email()),
                escapeHtml(entry.category()),
                entry.rating(),
                escapeHtml(entry.comments())
        );
        sendText(exchange, 200, "text/html; charset=UTF-8", confirmationHtml);
    }

    private void handleListFeedback(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < submissions.size(); i++) {
            FeedbackEntry e = submissions.get(i);
            if (i > 0) sb.append(',');
            sb.append(String.format(
                    "{\"name\":\"%s\",\"email\":\"%s\",\"category\":\"%s\",\"rating\":%d,\"comments\":\"%s\",\"submittedAt\":\"%s\"}",
                    escapeJson(e.name()),
                    escapeJson(e.email()),
                    escapeJson(e.category()),
                    e.rating(),
                    escapeJson(e.comments()),
                    escapeJson(e.submittedAt())
            ));
        }
        sb.append(']');
        sendText(exchange, 200, "application/json; charset=UTF-8", sb.toString());
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendText(exchange, 200, "application/json; charset=UTF-8",
                "{\"status\":\"UP\",\"service\":\"online-feedback-form\"}");
    }

    static String loadIndexHtml() throws IOException {
        try (InputStream in = FeedbackServer.class.getResourceAsStream("/index.html")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Path fallback = Path.of("index.html");
        if (Files.exists(fallback)) {
            return Files.readString(fallback, StandardCharsets.UTF_8);
        }
        throw new IOException("index.html not found on classpath or working directory");
    }

    static Map<String, String> parseFormBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isBlank()) {
            return map;
        }
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }

    private static boolean expectsJson(HttpExchange exchange) {
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        return accept != null && accept.contains("application/json");
    }

    private static void sendError(HttpExchange exchange, int status, String message, boolean json) throws IOException {
        if (json) {
            sendText(exchange, status, "application/json; charset=UTF-8",
                    "{\"error\":\"" + escapeJson(message) + "\"}");
        } else {
            sendText(exchange, status, "text/plain; charset=UTF-8", message);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        FeedbackServer app = new FeedbackServer(port);
        app.start();
        System.out.println("Online Feedback Server running on http://0.0.0.0:" + app.getPort());
    }
}
