package infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.JacksonUtils;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/// {@link RegisterExtension} : register instance as JUnit extension
/// will trigger all native junit routines internally
class HttpClientsTest {

    @RegisterExtension
    static WireMockExtension wireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig()
                            .dynamicPort()
                            .withRootDirectory("src/test/resources/wiremock"))
                    .build();

    @Test
    void test_given_http_client_should_call_to_wiremock_api() throws IOException, InterruptedException {
        // given
        HttpClient httpClient = HttpClients.getSimpleHttpClient();
        String urlComplete = wireMock.baseUrl() + "/some/thing";

        // when
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(urlComplete))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonNode = JacksonUtils.OBJECT_MAPPER.readTree(response.body());

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.body()).isNotNull();
        Assertions.assertThat(jsonNode.at("/payload/customerId").stringValue()).isEqualTo("a3be9d0fc37e44e0be5e5c122d3c2453");
    }

    @Test
    void test_given_http_client_should_call_with_retry_wrapper_to_wiremock_api() throws IOException, InterruptedException {
        // given
        HttpClient httpClient = HttpClients.getSimpleHttpClient();
        String urlComplete = wireMock.baseUrl() + "/some/thing";

        // when
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(urlComplete))
                .build();
        HttpResponse<String> response = HttpClients.sendRequest(httpClient, request);
        JsonNode jsonNode = JacksonUtils.OBJECT_MAPPER.readTree(response.body());

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.body()).isNotNull();
        Assertions.assertThat(jsonNode.at("/payload/customerId").stringValue()).isEqualTo("a3be9d0fc37e44e0be5e5c122d3c2453");
    }

}