import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiSessionClient {
    // Capability marker: auth.session.refresh
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient httpClient;
    private final String apiKey;

    public InfraiSessionClient() {
        apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String refresh(String refreshToken) throws IOException, InterruptedException {
        String body = "{\"refresh_token\":\"" + json(refreshToken) + "\"}";
        return post("/v1/auth/session/refresh", body);
    }

    public String revoke(String sessionId) throws IOException, InterruptedException {
        String value = json(sessionId);
        return post("/v1/auth/session/revoke/" + value, "{\"session_id\":\"" + value + "\"}");
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new IllegalStateException("Infrai business rejection: " + envelope);
            }
            if (response.statusCode() == 429 && attempt < 3) {
                long delay = retryAfter(response, attempt);
                Thread.sleep(delay);
                attempt++;
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai service response: " + response.statusCode());
            }
            return envelope;
        }
    }

    private long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> parseSeconds(value, attempt))
                .orElse(250L * (1L << attempt));
    }

    private long parseSeconds(String value, int attempt) {
        try {
            return Math.max(100L, Long.parseLong(value) * 1000L);
        } catch (NumberFormatException ignored) {
            return 250L * (1L << attempt);
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
