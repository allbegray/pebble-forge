package io.github.allbegray.pebbleforge.fuzz.support;

public final class FuzzText {

    private static final int MAX_ESCAPED_CHARS = 200;

    private FuzzText() {
    }

    public static String escape(String source) {
        StringBuilder escaped = new StringBuilder(Math.min(source.length(), MAX_ESCAPED_CHARS));
        for (int index = 0; index < source.length() && escaped.length() < MAX_ESCAPED_CHARS; index++) {
            char character = source.charAt(index);
            switch (character) {
                case '\0' -> escaped.append("\\0");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\\' -> escaped.append("\\\\");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        if (source.length() > MAX_ESCAPED_CHARS) {
            escaped.append("...");
        }
        return escaped.toString();
    }
}
