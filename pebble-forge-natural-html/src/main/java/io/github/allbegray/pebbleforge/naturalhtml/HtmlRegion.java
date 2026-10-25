package io.github.allbegray.pebbleforge.naturalhtml;

sealed interface HtmlRegion {

    int start();

    int end();

    record Allowed(int start, int end, HtmlContext context, String elementName,
                   String attributeName, char quote) implements HtmlRegion {
    }

    record Forbidden(int start, int end, String reason) implements HtmlRegion {
    }

    record Hint(int start, int end, String reason) implements HtmlRegion {
    }
}
