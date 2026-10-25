package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticFrame;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.spring.servlet.PebbleView;
import io.pebbletemplates.spring.servlet.PebbleViewResolver;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.support.RequestContext;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PebbleDiagnosticsViewTest {

    private final MockServletContext servletContext = new MockServletContext();
    private final GenericWebApplicationContext applicationContext =
            new GenericWebApplicationContext(servletContext);
    private final ClasspathLoader loader = classpathLoader();
    private final PebbleEngine engine = PebbleForge.engine()
            .loader(loader)
            .strictVariables(true)
            .cacheActive(false)
            .build();
    private final TemplateDiagnostics diagnostics = TemplateDiagnostics.builder()
            .loader(loader)
            .build();

    {
        applicationContext.refresh();
    }

    @Test
    void wrapsPebbleViewsAndLeavesOtherViewsUntouched() throws Exception {
        PebbleViewResolver official = officialResolver();
        PebbleDiagnosticsViewResolver resolver =
                new PebbleDiagnosticsViewResolver(official, diagnostics);

        assertThat(resolver.resolveViewName("broken-view", Locale.ENGLISH))
                .isInstanceOf(PebbleDiagnosticsView.class);

        View plain = (model, request, response) -> {
        };
        ViewResolver plainResolver = (viewName, locale) -> plain;
        assertThat(new PebbleDiagnosticsViewResolver(plainResolver, diagnostics)
                .resolveViewName("plain", Locale.ENGLISH)).isSameAs(plain);
    }

    @Test
    void resolvesBeforeTheOfficialResolver() throws Exception {
        PebbleViewResolver official = officialResolver();
        official.setOrder(100);

        assertThat(new PebbleDiagnosticsViewResolver(official, diagnostics).getOrder())
                .isEqualTo(99);
    }

    @Test
    void renderFailureSendsA500AndRecordsTheDiagnosticReport() throws Exception {
        View view = new PebbleDiagnosticsViewResolver(officialResolver(), diagnostics)
                .resolveViewName("broken-view", Locale.ENGLISH);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        view.render(Map.of(), request, response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .isInstanceOf(PebbleException.class);
        DiagnosticReport report = (DiagnosticReport) request.getAttribute(
                PebbleDiagnosticsAttributes.REPORT);
        assertThat(report).isNotNull();
        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("broken-view", "components/broken");
        assertThat(report.format())
                .contains("broken-view:1 includes components/broken")
                .contains("> 1 | {{ missing }}");
    }

    @Test
    void parentBodyFailureCarriesTheExtendsFrame() throws Exception {
        View view = new PebbleDiagnosticsViewResolver(officialResolver(), diagnostics)
                .resolveViewName("extends-child", Locale.ENGLISH);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        view.render(Map.of(), request, response);

        assertThat(response.getStatus()).isEqualTo(500);
        DiagnosticReport report = (DiagnosticReport) request.getAttribute(
                PebbleDiagnosticsAttributes.REPORT);
        assertThat(report).isNotNull();
        assertThat(report.frames()).extracting(DiagnosticFrame::label)
                .contains("extends extends-layout");
        assertThat(report.format())
                .contains("extends-child:1 extends extends-layout")
                .contains("extends-layout:2");
    }

    @Test
    void renderFailureStillSends500WhenDiagnosticsAreDisabled() throws Exception {
        View view = new PebbleDiagnosticsViewResolver(officialResolver(), null)
                .resolveViewName("broken-view", Locale.ENGLISH);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        view.render(Map.of(), request, response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(request.getAttribute(PebbleDiagnosticsAttributes.REPORT)).isNull();
        assertThat(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .isInstanceOf(PebbleException.class);
    }

    @Test
    void nonPebbleRenderFailureSends500WithoutDiagnostics() throws Exception {
        PebbleView failing = new PebbleView() {
            @Override
            public void render(Map<String, ?> model, HttpServletRequest request,
                    HttpServletResponse response) {
                throw new IllegalStateException("boom");
            }
        };
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new PebbleDiagnosticsView(failing, diagnostics).render(Map.of(), request, response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(request.getAttribute(PebbleDiagnosticsAttributes.REPORT)).isNull();
        assertThat(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void committedResponseOnlyRecordsAndLogs() throws Exception {
        View view = new PebbleDiagnosticsViewResolver(officialResolver(), diagnostics)
                .resolveViewName("broken-view", Locale.ENGLISH);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        view.render(Map.of(), request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
        assertThat(request.getAttribute(PebbleDiagnosticsAttributes.REPORT))
                .isInstanceOf(DiagnosticReport.class);
    }

    @Test
    void doesNotHandleFailuresWhileAlreadyRenderingAnErrorPage() throws Exception {
        View view = new PebbleDiagnosticsViewResolver(officialResolver(), diagnostics)
                .resolveViewName("broken-view", Locale.ENGLISH);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(PebbleDiagnosticsView.ERROR_HANDLING_ATTRIBUTE, Boolean.TRUE);

        assertThatThrownBy(() -> view.render(Map.of(), request, response))
                .isInstanceOf(PebbleException.class);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(PebbleDiagnosticsAttributes.REPORT)).isNull();
    }

    @Test
    void exposesTheDelegateContentType() throws Exception {
        PebbleViewResolver official = officialResolver();
        official.setContentType("text/html");
        View view = new PebbleDiagnosticsViewResolver(official, diagnostics)
                .resolveViewName("broken-view", Locale.ENGLISH);

        assertThat(view.getContentType()).isEqualTo("text/html");
    }

    @Test
    void doesNotWrapNonPebbleViews() throws Exception {
        AtomicReference<Boolean> rendered = new AtomicReference<>(false);
        View failing = (model, request, response) -> {
            rendered.set(true);
            request.setAttribute(PebbleDiagnosticsAttributes.REPORT, "not a report");
            throw new IllegalStateException("boom");
        };
        View wrapped = wrapPlainView(failing);

        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> wrapped.render(Map.of(), request, response))
                .isInstanceOf(IllegalStateException.class);

        assertThat(rendered).hasValue(true);
        assertThat(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private View wrapPlainView(View view) throws Exception {
        return new PebbleDiagnosticsViewResolver((viewName, locale) -> view, diagnostics)
                .resolveViewName("plain", Locale.ENGLISH);
    }

    private PebbleViewResolver officialResolver() {
        PebbleViewResolver resolver = new PebbleViewResolver(engine);
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("templates/");
        resolver.setSuffix(".peb");
        resolver.setCache(false);
        resolver.afterPropertiesSet();
        return resolver;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                applicationContext);
        return request;
    }

    private static ClasspathLoader classpathLoader() {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("templates/");
        loader.setSuffix(".peb");
        return loader;
    }
}
