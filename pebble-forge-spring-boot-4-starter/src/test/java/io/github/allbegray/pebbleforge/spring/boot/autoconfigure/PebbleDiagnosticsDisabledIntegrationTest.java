package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = DiagnosticsTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pebble.strict-variables=true",
                "pebble.forge.diagnostics.enabled=false"
        })
class PebbleDiagnosticsDisabledIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void aBrokenTemplateStillProducesA500WithoutDiagnostics() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/broken"))
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body())
                .contains("No template diagnostics available")
                .doesNotContain("components/broken");
    }
}
