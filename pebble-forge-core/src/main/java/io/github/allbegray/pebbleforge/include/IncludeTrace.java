package io.github.allbegray.pebbleforge.include;

import io.github.allbegray.pebbleforge.trace.TemplateTrace;

import java.util.List;

/**
 * Compatibility view of the include portion of the unified template trace.
 */
public final class IncludeTrace {

    private IncludeTrace() {
    }

    /**
     * Returns only include frames, in chronological order.
     *
     * @param error the error to inspect
     * @return the include frames
     */
    public static List<IncludeFrame> frames(Throwable error) {
        return TemplateTrace.frames(error).stream()
                .filter(frame -> frame.kind() == TemplateTrace.Kind.INCLUDE)
                .map(frame -> new IncludeFrame(frame.template(), frame.line(), frame.target()))
                .toList();
    }

    /**
     * Counts only include frames; extends frames do not consume include depth.
     */
    static int depth() {
        return TemplateTrace.includeDepth();
    }

    /**
     * One include call in a template trace.
     *
     * @param caller the template that declared the include
     * @param line the line of the include tag
     * @param target the included template
     */
    public record IncludeFrame(String caller, int line, String target) {
    }
}
