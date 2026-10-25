package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.Set;

final class ExpressionInspector {

    private static final Set<String> FORBIDDEN_IDENTIFIERS = Set.of(
            "raw", "escape", "date", "safe");

    private static final Set<String> FORBIDDEN_FUNCTIONS = Set.of(
            "slot", "block", "parent");

    private ExpressionInspector() {
    }

    static void rejectUnsupportedCallables(SourceText text, ExpressionTag tag) {
        String expression = tag.expression();
        int index = 0;
        while (index < expression.length()) {
            char character = expression.charAt(index);
            if (character == '\'' || character == '"') {
                index = skipString(expression, index);
                continue;
            }
            if (Character.isLetter(character) || character == '_') {
                int wordStart = index++;
                while (index < expression.length()
                        && (Character.isLetterOrDigit(expression.charAt(index))
                        || expression.charAt(index) == '_')) {
                    index++;
                }
                String word = expression.substring(wordStart, index);
                int offset = tag.expressionStart() + wordStart;
                if (FORBIDDEN_IDENTIFIERS.contains(word)) {
                    throw text.error(offset, "[" + word + "] is not supported in Natural HTML "
                            + "expressions; escaping is added by the loader");
                }
                if (FORBIDDEN_FUNCTIONS.contains(word) && nextNonWhitespaceIsParen(
                        expression, index)) {
                    throw text.error(offset, "Function [" + word + "()] is not supported in "
                            + "Natural HTML expressions");
                }
                continue;
            }
            index++;
        }
    }

    private static boolean nextNonWhitespaceIsParen(String expression, int index) {
        int cursor = index;
        while (cursor < expression.length()
                && Character.isWhitespace(expression.charAt(cursor))) {
            cursor++;
        }
        return cursor < expression.length() && expression.charAt(cursor) == '(';
    }

    private static int skipString(String expression, int index) {
        char quote = expression.charAt(index);
        int cursor = index + 1;
        while (cursor < expression.length()) {
            char character = expression.charAt(cursor);
            if (character == '\\') {
                cursor += 2;
                continue;
            }
            if (character == quote) {
                return cursor + 1;
            }
            cursor++;
        }
        return cursor;
    }
}
