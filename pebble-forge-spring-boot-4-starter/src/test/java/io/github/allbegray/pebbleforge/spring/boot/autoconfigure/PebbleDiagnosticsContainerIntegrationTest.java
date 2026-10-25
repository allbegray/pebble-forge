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
                "pebble.forge.sandbox.enabled=true",
                "pebble.forge.diagnostics.expose-in-error-attributes=true"
        })
class PebbleDiagnosticsContainerIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void aBrokenTemplateProducesAReal500WithSourceFrames() throws Exception {
        HttpResponse<String> response = get("/broken");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body())
                .contains("broken-view:1 includes components/broken")
                .contains("components/broken:1")
                .contains("{{ missing }}");
    }

    @Test
    void aNonPebbleExceptionProducesA500WithoutDiagnostics() throws Exception {
        HttpResponse<String> response = get("/boom");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body())
                .contains("No template diagnostics available")
                .doesNotContain("components/broken");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path))
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
