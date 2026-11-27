package infrastructure;

import tools.resilience4j.RetryWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

public class HttpClients {

    public static HttpClient getSimpleHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /// use only in performance free context or testing, initialize http client each time
    public static String sendSimpleRequest(String url) {
        HttpClient client = getSimpleHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = sendRequest(client, request);
        return response.body();
    }

    public static HttpResponse<String> sendRequest(HttpClient client, HttpRequest request) {
        Supplier<HttpResponse<String>> s = () -> {
            try {
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        return RetryWrapper.retry(s, 2, 2);
    }

}
