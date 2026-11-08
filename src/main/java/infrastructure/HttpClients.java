package infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClients {

    public static HttpClient getSimpleHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

}
