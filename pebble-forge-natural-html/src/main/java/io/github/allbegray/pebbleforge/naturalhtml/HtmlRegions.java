package io.github.allbegray.pebbleforge.naturalhtml;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Range;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class HtmlRegions {

    private static final Set<String> FORBIDDEN_ELEMENTS = Set.of(
            "script", "style", "meta", "base", "iframe", "object", "embed", "svg", "math",
            "template", "noscript", "xmp", "plaintext", "noembed", "noframes", "frame",
            "frameset", "applet");

    private static final Set<String> FORBIDDEN_ATTRIBUTES = Set.of(
            "style", "srcdoc", "srcset", "imagesrcset", "ping", "archive");

    private static final Set<String> URL_ATTRIBUTES = Set.of(
            "href", "src", "action", "formaction", "poster", "cite", "data", "background",
            "longdesc", "usemap", "manifest");

    private HtmlRegions() {
    }

    static HtmlScan collect(Document document, String source) {
        List<HtmlRegion> regions = new ArrayList<>();
        for (Node child : document.childNodes()) {
            collect(child, source, regions);
        }
        return new HtmlScan(List.copyOf(regions));
    }

    private static void collect(Node node, String source, List<HtmlRegion> regions) {
        if (node instanceof Element element) {
            String elementName = element.normalName();
            if (FORBIDDEN_ELEMENTS.contains(elementName)) {
                regions.add(new HtmlRegion.Forbidden(
                        element.sourceRange().startPos(),
                        nodeEnd(element),
                        "<" + elementName + "> is not supported in Natural HTML"));
                return;
            }
            addTagHints(element, regions);
            for (Attribute attribute : element.attributes()) {
                addAttribute(element, attribute, source, regions);
            }
            for (Node child : element.childNodes()) {
                collect(child, source, regions);
            }
            return;
        }
        if (node instanceof CDataNode) {
            addForbidden(node, "CDATA sections", regions);
            return;
        }
        if (node instanceof DataNode) {
            addForbidden(node, "raw text elements", regions);
            return;
        }
        if (node instanceof Comment) {
            addForbidden(node, "HTML comments", regions);
            return;
        }
        if (node instanceof DocumentType) {
            addForbidden(node, "doctypes", regions);
            return;
        }
        if (node instanceof TextNode) {
            Range range = node.sourceRange();
            if (!range.isTracked()) {
                return;
            }
            String parentName = node.parent() instanceof Element parent
                    ? parent.normalName()
                    : null;
            HtmlContext context = "title".equals(parentName) || "textarea".equals(parentName)
                    ? HtmlContext.RCDATA
                    : HtmlContext.TEXT;
            regions.add(new HtmlRegion.Allowed(
                    range.startPos(), range.endPos(), context, parentName, null, '\0'));
            return;
        }
        for (Node child : node.childNodes()) {
            collect(child, source, regions);
        }
    }

    private static void addForbidden(Node node, String what, List<HtmlRegion> regions) {
        Range range = node.sourceRange();
        if (!range.isTracked()) {
            return;
        }
        regions.add(new HtmlRegion.Forbidden(
                range.startPos(), range.endPos(), "Expressions are not allowed in " + what));
    }

    private static void addTagHints(Element element, List<HtmlRegion> regions) {
        Range startTag = element.sourceRange();
        if (!startTag.isTracked() || startTag.startPos() == startTag.endPos()) {
            return;
        }
        regions.add(new HtmlRegion.Hint(
                startTag.startPos(), startTag.endPos(),
                "Expressions are not allowed inside an HTML tag"));
        int nameEnd = Math.min(startTag.endPos(), startTag.startPos() + 1
                + element.tagName().length());
        if (nameEnd > startTag.startPos() + 1) {
            regions.add(new HtmlRegion.Hint(
                    startTag.startPos() + 1, nameEnd,
                    "Expressions are not allowed in tag names"));
        }
    }

    private static void addAttribute(Element element, Attribute attribute, String source,
                                     List<HtmlRegion> regions) {
        Range.AttributeRange attributeRange =
                element.attributes().sourceRange(attribute.getKey());
        Range nameRange = attributeRange.nameRange();
        if (nameRange.isTracked()) {
            regions.add(new HtmlRegion.Hint(
                    nameRange.startPos(), nameRange.endPos(),
                    "Expressions are not allowed in attribute names"));
        }

        Range valueRange = attributeRange.valueRange();
        if (!valueRange.isTracked() || valueRange.startPos() >= valueRange.endPos()) {
            return;
        }
        int start = valueRange.startPos();
        int end = valueRange.endPos();
        char quote = start > 0 ? source.charAt(start - 1) : '\0';
        String name = attribute.getKey().toLowerCase(Locale.ROOT);
        if (quote != '"' && quote != '\'') {
            regions.add(new HtmlRegion.Forbidden(
                    start, end, "Unquoted attribute values cannot contain expressions"));
            return;
        }
        if (name.startsWith("on") || FORBIDDEN_ATTRIBUTES.contains(name)) {
            regions.add(new HtmlRegion.Forbidden(
                    start, end, "Attribute [" + name + "] does not support expressions"));
            return;
        }
        HtmlContext context = URL_ATTRIBUTES.contains(name)
                ? HtmlContext.URL_ATTRIBUTE_VALUE
                : HtmlContext.ATTRIBUTE_VALUE;
        regions.add(new HtmlRegion.Allowed(
                start, end, context, element.normalName(), name, quote));
    }

    private static int nodeEnd(Node node) {
        Range range = node.sourceRange();
        int end = range.isTracked() ? range.endPos() : 0;
        if (node instanceof Element element) {
            Range endTag = element.endSourceRange();
            if (endTag.isTracked()) {
                end = Math.max(end, endTag.endPos());
            }
        }
        for (Node child : node.childNodes()) {
            end = Math.max(end, nodeEnd(child));
        }
        return end;
    }
}
