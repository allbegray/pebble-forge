package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.ArrayList;
import java.util.List;

final class ExpressionScanner {

    private ExpressionScanner() {
    }

    static List<ExpressionTag> scan(SourceText text) {
        String source = text.source();
        List<ExpressionTag> tags = new ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int start = source.indexOf("{{", searchFrom);
            if (start < 0) {
                return List.copyOf(tags);
            }
            ExpressionTag tag = parse(text, start);
            tags.add(tag);
            searchFrom = tag.end();
        }
    }

    private static ExpressionTag parse(SourceText text, int start) {
        String source = text.source();
        int length = source.length();
        int contentStart = start + 2;
        if (contentStart + 1 < length
                && source.charAt(contentStart) == '-'
                && isPebbleWhitespace(source.charAt(contentStart + 1))) {
            contentStart++;
        }

        int braceDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int quoteStart = -1;

        for (int index = contentStart; index < length; index++) {
            char character = source.charAt(index);

            if (inSingleQuote || inDoubleQuote) {
                if (character == '\\') {
                    index++;
                } else if (inSingleQuote && character == '\'') {
                    inSingleQuote = false;
                } else if (inDoubleQuote && character == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (character == '\'') {
                inSingleQuote = true;
                quoteStart = index;
                continue;
            }
            if (character == '"') {
                inDoubleQuote = true;
                quoteStart = index;
                continue;
            }
            if (character == '{') {
                if (index + 1 < length) {
                    char next = source.charAt(index + 1);
                    if (next == '{') {
                        throw text.error(index,
                                "Nested {{ is not a valid expression");
                    }
                    if (next == '%') {
                        throw text.error(index,
                                "Pebble tags are not supported in Natural HTML; "
                                        + "use expressions only");
                    }
                    if (next == '#') {
                        throw text.error(index,
                                "Pebble comments are not supported in Natural HTML");
                    }
                }
                braceDepth++;
                continue;
            }
            if (character == '}') {
                if (braceDepth > 0) {
                    braceDepth--;
                    continue;
                }
                if (index + 1 < length && source.charAt(index + 1) == '}') {
                    return finish(text, start, index + 2, contentStart, index);
                }
                continue;
            }
            if (character == '-' && braceDepth == 0
                    && index + 2 < length
                    && source.charAt(index + 1) == '}'
                    && source.charAt(index + 2) == '}') {
                return finish(text, start, index + 3, contentStart, index);
            }
        }

        if (inSingleQuote || inDoubleQuote) {
            throw text.error(quoteStart, "Unterminated string literal in expression");
        }
        throw text.error(start, "Unclosed expression; expected }}");
    }

    private static ExpressionTag finish(SourceText text, int start, int end, int contentStart,
                                        int contentEnd) {
        int expressionStart = contentStart;
        int expressionEnd = contentEnd;
        while (expressionStart < expressionEnd
                && Character.isWhitespace(text.source().charAt(expressionStart))) {
            expressionStart++;
        }
        while (expressionEnd > expressionStart
                && Character.isWhitespace(text.source().charAt(expressionEnd - 1))) {
            expressionEnd--;
        }
        if (expressionStart == expressionEnd) {
            throw text.error(start, "Empty expression");
        }
        String expression = text.source().substring(expressionStart, expressionEnd);
        return new ExpressionTag(start, end, expressionStart, expressionEnd, expression);
    }

    private static boolean isPebbleWhitespace(char character) {
        return character == ' ' || character == '\t' || character == '\n'
                || character == '\u000B' || character == '\f' || character == '\r';
    }
}
