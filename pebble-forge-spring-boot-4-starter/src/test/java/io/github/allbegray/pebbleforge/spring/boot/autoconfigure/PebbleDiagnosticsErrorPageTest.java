package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.DiagnosticFrame;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = DiagnosticsTestApplication.class,
        properties = {
                "pebble.strict-variables=true",
                "pebble.forge.sandbox.enabled=true",
                "pebble.forge.diagnostics.expose-in-error-attributes=true"
        })
@AutoConfigureMockMvc
class PebbleDiagnosticsErrorPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ErrorAttributes errorAttributes;

    @Test
    void replacesBootsDefaultErrorAttributesOnlyWhenExposureIsAllowed() {
        assertThat(errorAttributes).isInstanceOf(PebbleDiagnosticsErrorAttributes.class);
    }

    @Test
    void rendersDiagnosticsTextOnTheApplicationErrorPage() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/broken")
                        .requestAttr(PebbleDiagnosticsAttributes.REPORT, report()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString(
                        "broken-view:1 includes components/broken")))
                .andExpect(content().string(containsString("&gt; 1 | {{ missing }}")));
    }

    @Test
    void rendersTheErrorPageWithoutDiagnosticsWhenNoReportExists() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("No template diagnostics available")));
    }

    @Test
    void recordsDiagnosticsWhenAViewFailsDuringAMockMvcRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/broken"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getRequest().getAttribute(PebbleDiagnosticsAttributes.REPORT))
                .isInstanceOf(DiagnosticReport.class);
    }

    private static DiagnosticReport report() {
        return new DiagnosticReport("Root attribute [missing] does not exist", List.of(
                new DiagnosticFrame("broken-view", 1, "includes components/broken",
                        List.of("{% include \"components/broken\" %}"), 0, 1),
                new DiagnosticFrame("components/broken", 1, null,
                        List.of("{{ missing }}"), 0, 1)));
    }
}
