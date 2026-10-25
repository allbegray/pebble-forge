package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.DiagnosticFrame;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class PebbleDiagnosticsErrorAttributesTest {

    private final PebbleDiagnosticsErrorAttributes errorAttributes =
            new PebbleDiagnosticsErrorAttributes();

    @Test
    void addsStructuredAndTextDiagnosticsWithoutChangingDefaults() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/broken");
        DiagnosticReport report = report();
        request.setAttribute(PebbleDiagnosticsAttributes.REPORT, report);

        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertThat(attributes)
                .containsEntry("status", 500)
                .containsEntry("path", "/broken")
                .containsEntry("error", "Internal Server Error");
        assertThat(attributes.get(PebbleDiagnosticsAttributes.MODEL_REPORT)).isSameAs(report);
        assertThat(report.frames())
                .extracting(DiagnosticFrame::kind, DiagnosticFrame::target)
                .containsExactly(tuple(DiagnosticFrame.Kind.INCLUDE, "components/broken"),
                        tuple(DiagnosticFrame.Kind.ERROR, null));
        assertThat((String) attributes.get(PebbleDiagnosticsAttributes.MODEL_TEXT))
                .contains("broken-view:1 includes components/broken")
                .contains("> 1 | {{ missing }}");
    }

    @Test
    void leavesBootAttributesAloneWithoutADiagnosticReport() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);

        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertThat(attributes).containsEntry("status", 500);
        assertThat(attributes).doesNotContainKeys(PebbleDiagnosticsAttributes.MODEL_REPORT,
                PebbleDiagnosticsAttributes.MODEL_TEXT);
    }

    @Test
    void ignoresUnrelatedRequestAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(PebbleDiagnosticsAttributes.REPORT, "not a report");

        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertThat(attributes).doesNotContainKeys(PebbleDiagnosticsAttributes.MODEL_REPORT,
                PebbleDiagnosticsAttributes.MODEL_TEXT);
    }

    private static DiagnosticReport report() {
        return new DiagnosticReport("Root attribute [missing] does not exist", List.of(
                new DiagnosticFrame("broken-view", 1, "includes components/broken",
                        List.of("{% include \"components/broken\" %}"), 0, 1,
                        DiagnosticFrame.Kind.INCLUDE, "components/broken"),
                new DiagnosticFrame("components/broken", 1, null,
                        List.of("{{ missing }}"), 0, 1)));
    }
}
