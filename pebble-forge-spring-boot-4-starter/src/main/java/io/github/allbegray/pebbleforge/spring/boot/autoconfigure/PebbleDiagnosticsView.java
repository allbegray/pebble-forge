package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.spring.servlet.PebbleView;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Wraps a {@link PebbleView} so a render failure records diagnostics and starts a real
 * servlet error dispatch instead of leaving a partial response in place.
 */
final class PebbleDiagnosticsView implements View {

    static final String ERROR_HANDLING_ATTRIBUTE =
            PebbleDiagnosticsView.class.getName() + ".errorHandling";

    private static final Logger LOG = LoggerFactory.getLogger(PebbleDiagnosticsView.class);

    private final PebbleView delegate;
    private final TemplateDiagnostics diagnostics;

    PebbleDiagnosticsView(PebbleView delegate, TemplateDiagnostics diagnostics) {
        this.delegate = delegate;
        this.diagnostics = diagnostics;
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (isErrorHandling(request)) {
            delegate.render(model, request, response);
            return;
        }

        FlushSuppressingResponse bufferedResponse = new FlushSuppressingResponse(response);
        try {
            // PebbleView flushes the response writer in a finally block, which would commit the
            // response before we can turn a render failure into an error dispatch.
            TemplateTrace.run(() -> {
                try {
                    delegate.render(model, request, bufferedResponse);
                } catch (IOException error) {
                    throw error;
                } catch (Exception error) {
                    throw new RenderException(error);
                }
            });
        } catch (RenderException error) {
            handle(error, error.getCause(), request, response);
            return;
        } catch (Exception error) {
            handle(error, error, request, response);
            return;
        }
        bufferedResponse.flushAfterSuccessfulRender();
    }

    private void handle(Exception reportError, Throwable servletError,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        DiagnosticReport report = diagnostics == null ? null : diagnostics.render(reportError);
        if (report != null && !report.frames().isEmpty()) {
            request.setAttribute(PebbleDiagnosticsAttributes.REPORT, report);
        }

        if (response.isCommitted()) {
            LOG.warn("Pebble template render failed after the response was committed; "
                    + "report:\n{}", report == null ? servletError : report.format(), servletError);
            return;
        }

        request.setAttribute(ERROR_HANDLING_ATTRIBUTE, Boolean.TRUE);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, servletError);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.resetBuffer();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private static boolean isErrorHandling(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(ERROR_HANDLING_ATTRIBUTE))
                || request.getDispatcherType() == DispatcherType.ERROR;
    }

    private static final class RenderException extends RuntimeException {

        private RenderException(Exception cause) {
            super(cause);
        }
    }

    private static final class FlushSuppressingResponse extends HttpServletResponseWrapper {

        private PrintWriter delegateWriter;
        private PrintWriter writer;

        private FlushSuppressingResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                delegateWriter = super.getWriter();
                writer = new PrintWriter(new FlushSuppressingWriter(delegateWriter));
            }
            return writer;
        }

        @Override
        public void flushBuffer() {
            // Do not commit until the whole template has rendered.
        }

        private void flushAfterSuccessfulRender() throws IOException {
            if (delegateWriter != null) {
                delegateWriter.flush();
            }
        }
    }

    private static final class FlushSuppressingWriter extends FilterWriter {

        private FlushSuppressingWriter(Writer out) {
            super(out);
        }

        @Override
        public void flush() {
            // The wrapped view flushes on failure; keep the response uncommitted until then.
        }
    }
}
