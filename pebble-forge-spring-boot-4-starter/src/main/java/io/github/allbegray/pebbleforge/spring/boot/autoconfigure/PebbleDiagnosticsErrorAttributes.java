package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds rendered Pebble diagnostics to Boot's default error attributes without changing the
 * default attributes themselves.
 */
final class PebbleDiagnosticsErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest,
            ErrorAttributeOptions options) {
        Map<String, Object> attributes =
                new LinkedHashMap<>(super.getErrorAttributes(webRequest, options));
        Object value = webRequest.getAttribute(PebbleDiagnosticsAttributes.REPORT,
                RequestAttributes.SCOPE_REQUEST);
        if (value instanceof DiagnosticReport report) {
            attributes.put(PebbleDiagnosticsAttributes.MODEL_REPORT, report);
            attributes.put(PebbleDiagnosticsAttributes.MODEL_TEXT, report.format());
        }
        return attributes;
    }
}
