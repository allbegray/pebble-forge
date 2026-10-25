package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.escape.ContextEscaping;
import org.jsoup.nodes.Document;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class NaturalHtmlAnalyzer {

    private NaturalHtmlAnalyzer() {
    }

    static NaturalHtmlAnalysis analyze(String templateName, String source) {
        SourceText text = new SourceText(templateName, source);
        List<ExpressionTag> tags = ExpressionScanner.scan(text);
        rejectPebbleSyntax(text, tags);

        Parser parser = Parser.htmlParser().setTrackPosition(true).setTrackErrors(1000);
        Document document = parser.parseInput(source, templateName);
        HtmlScan html = HtmlRegions.collect(document, source);

        List<Classified> classified = new ArrayList<>();
        for (ExpressionTag tag : tags) {
            classified.add(classify(text, tag, html));
        }
        for (Classified site : classified) {
            if (site.region().context() == HtmlContext.URL_ATTRIBUTE_VALUE) {
                validateUrlValue(text, site.region(), tags);
            }
        }

        rejectParseErrors(text, document);
        Rewrite rewrite = rewrite(text, classified);
        return new NaturalHtmlAnalysis(
                templateName,
                source,
                rewrite.generatedSource(),
                rewrite.sourceMap(),
                classified.stream().map(Classified::site).toList());
    }

    private static Classified classify(SourceText text, ExpressionTag tag, HtmlScan html) {
        String source = text.source();
        if (immediatelyAfterTagOpen(source, tag.start())) {
            throw text.error(tag.start(),
                    "Expressions are not allowed immediately after < or </");
        }
        Optional<HtmlRegion.Forbidden> forbidden = html.forbiddenAt(tag.start());
        if (forbidden.isPresent()) {
            throw text.error(tag.start(), forbidden.get().reason());
        }
        Optional<HtmlRegion.Allowed> allowed = html.allowedAt(tag.start());
        if (allowed.isEmpty()) {
            String reason = html.hintAt(tag.start()).orElse(
                    "Expression is not in an HTML text, RCDATA, quoted attribute, "
                            + "or quoted URL attribute context");
            throw text.error(tag.start(), reason);
        }

        HtmlRegion.Allowed region = allowed.get();
        rejectTextMarkup(text, tag, region);
        if (tag.end() > region.end()) {
            throw text.error(tag.start(),
                    "Expression crosses an HTML boundary; keep the whole {{ ... }} "
                            + "inside one text node or quoted attribute value");
        }
        ExpressionInspector.rejectUnsupportedCallables(text, tag);

        String strategy = switch (region.context()) {
            case TEXT, RCDATA -> ContextEscaping.HTML;
            case ATTRIBUTE_VALUE -> ContextEscaping.HTML_ATTRIBUTE;
            case URL_ATTRIBUTE_VALUE -> ContextEscaping.URL;
        };
        NaturalHtmlSite site = new NaturalHtmlSite(
                tag.start(),
                tag.end(),
                text.position(tag.start()),
                region.context(),
                region.elementName(),
                region.attributeName(),
                tag.expression(),
                strategy);
        return new Classified(tag, site, region);
    }

    private static void rejectTextMarkup(SourceText text, ExpressionTag tag,
                                         HtmlRegion.Allowed region) {
        if (region.context() != HtmlContext.TEXT && region.context() != HtmlContext.RCDATA) {
            return;
        }
        String expression = tag.expression();
        for (int index = 0; index < expression.length(); index++) {
            if (expression.charAt(index) != '<' || index + 1 >= expression.length()) {
                continue;
            }
            char next = expression.charAt(index + 1);
            if (Character.isLetter(next) || next == '/' || next == '!' || next == '?') {
                throw text.error(tag.expressionStart() + index,
                        "Expression text contains '<' followed by '" + next
                                + "', which a browser treats as markup. Escape the value "
                                + "or keep it out of text and RCDATA");
            }
        }
    }

    private static boolean immediatelyAfterTagOpen(String source, int offset) {
        if (offset >= 1 && source.charAt(offset - 1) == '<') {
            return true;
        }
        return offset >= 2 && source.charAt(offset - 2) == '<'
                && source.charAt(offset - 1) == '/';
    }

    private static void rejectPebbleSyntax(SourceText text, List<ExpressionTag> tags) {
        String source = text.source();
        int segmentStart = 0;
        for (ExpressionTag tag : tags) {
            rejectPebbleMarker(text, segmentStart, tag.start());
            segmentStart = tag.end();
        }
        rejectPebbleMarker(text, segmentStart, source.length());
    }

    private static void rejectPebbleMarker(SourceText text, int from, int to) {
        int execute = text.source().indexOf("{%", from);
        if (execute >= 0 && execute < to) {
            throw text.error(execute,
                    "Pebble tags are not supported in Natural HTML; use expressions only");
        }
        int comment = text.source().indexOf("{#", from);
        if (comment >= 0 && comment < to) {
            throw text.error(comment,
                    "Pebble comments are not supported in Natural HTML");
        }
    }

    private static void validateUrlValue(SourceText text, HtmlRegion.Allowed region,
                                         List<ExpressionTag> tags) {
        String source = text.source();
        List<ExpressionTag> inRegion = tags.stream()
                .filter(tag -> tag.start() >= region.start() && tag.end() <= region.end())
                .toList();
        if (inRegion.isEmpty()) {
            return;
        }

        int firstContent = region.start();
        while (firstContent < region.end()
                && isAsciiWhitespace(source.charAt(firstContent))) {
            firstContent++;
        }
        int prefixEnd = inRegion.get(0).start();
        String prefix = source.substring(firstContent, prefixEnd);
        String trimmedPrefix = asciiStrip(prefix);
        if (!trimmedPrefix.isEmpty()) {
            if (!isAllowedUrlPrefix(trimmedPrefix)) {
                throw text.error(firstContent,
                        "URL attribute must start with a relative prefix (/ , ./, ../, #, ?) "
                                + "or hold a whole-value expression");
            }
            return;
        }

        int cursor = firstContent;
        for (ExpressionTag tag : inRegion) {
            if (containsNonWhitespace(source, cursor, tag.start())) {
                throw text.error(inRegion.get(0).start(),
                        "URL attribute must start with a relative prefix (/ , ./, ../, #, ?) "
                                + "or hold a whole-value expression");
            }
            cursor = tag.end();
        }
        if (containsNonWhitespace(source, cursor, region.end())) {
            throw text.error(inRegion.get(0).start(),
                    "URL attribute must start with a relative prefix (/ , ./, ../, #, ?) "
                            + "or hold a whole-value expression");
        }
    }

    private static boolean isAllowedUrlPrefix(String prefix) {
        if (prefix.startsWith("//") || prefix.startsWith("/\\")) {
            return false;
        }
        return prefix.startsWith("/")
                || prefix.startsWith("./")
                || prefix.startsWith("../")
                || prefix.startsWith("#")
                || prefix.startsWith("?");
    }

    private static boolean containsNonWhitespace(String source, int start, int end) {
        for (int index = start; index < end; index++) {
            if (!isAsciiWhitespace(source.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static String asciiStrip(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isAsciiWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isAsciiWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isAsciiWhitespace(char character) {
        return character == ' ' || character == '\t' || character == '\n'
                || character == '\u000B' || character == '\f' || character == '\r';
    }

    private static void rejectParseErrors(SourceText text, Document document) {
        List<ParseError> errors = document.parser().getErrors();
        if (errors.isEmpty()) {
            return;
        }
        ParseError first = errors.get(0);
        throw text.error(first.getPosition(), "Invalid HTML: " + first.getErrorMessage());
    }

    private static Rewrite rewrite(SourceText text, List<Classified> classified) {
        String source = text.source();
        StringBuilder generated = new StringBuilder(source.length() + classified.size() * 32);
        List<SourceMap.Insertion> insertions = new ArrayList<>();
        int cursor = 0;
        for (Classified site : classified) {
            int expressionStart = site.tag().expressionStart();
            int expressionEnd = site.tag().expressionEnd();
            generated.append(source, cursor, expressionStart);
            int generatedOffset = generated.length();
            generated.append('(');
            insertions.add(new SourceMap.Insertion(generatedOffset, expressionStart, 1));
            generated.append(source, expressionStart, expressionEnd);
            generatedOffset = generated.length();
            String filter = filterText(
                    site.site().escapeStrategy(), site.region().quote());
            generated.append(filter);
            insertions.add(new SourceMap.Insertion(
                    generatedOffset, expressionEnd, filter.length()));
            cursor = expressionEnd;
        }
        generated.append(source, cursor, source.length());
        return new Rewrite(
                generated.toString(),
                new SourceMap(source, insertions, generated.length()));
    }

    private static String filterText(String strategy, char attributeQuote) {
        char quote = attributeQuote == '\'' ? '"' : '\'';
        return ") | escape(" + quote + strategy + quote + ")";
    }

    private record Classified(ExpressionTag tag, NaturalHtmlSite site, HtmlRegion.Allowed region) {
    }

    private record Rewrite(String generatedSource, SourceMap sourceMap) {
    }
}
