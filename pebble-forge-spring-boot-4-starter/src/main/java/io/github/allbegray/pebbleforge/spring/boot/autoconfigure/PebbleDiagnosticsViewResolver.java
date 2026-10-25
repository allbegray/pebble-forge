package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.pebbletemplates.spring.servlet.PebbleView;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * Resolves views through the official Pebble resolver and decorates only Pebble views.
 */
final class PebbleDiagnosticsViewResolver implements ViewResolver, Ordered {

    private final ViewResolver delegate;
    private final TemplateDiagnostics diagnostics;

    PebbleDiagnosticsViewResolver(ViewResolver delegate, TemplateDiagnostics diagnostics) {
        this.delegate = delegate;
        this.diagnostics = diagnostics;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View view = delegate.resolveViewName(viewName, locale);
        if (view instanceof PebbleView pebbleView) {
            return new PebbleDiagnosticsView(pebbleView, diagnostics);
        }
        return view;
    }

    @Override
    public int getOrder() {
        if (delegate instanceof Ordered ordered) {
            int order = ordered.getOrder();
            return order == Ordered.HIGHEST_PRECEDENCE ? order : order - 1;
        }
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
