package infrastructure;

import tools.resilience4j.RetryWrapper;

import java.io.IOException;
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

    public static HttpResponse<String> sendRequest(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
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
