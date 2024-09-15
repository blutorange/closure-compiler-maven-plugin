package com.github.blutorange.maven.plugin.closurecompiler.common;

import static org.owasp.encoder.Encode.forHtmlAttribute;
import static org.owasp.encoder.Encode.forXmlAttribute;

import java.util.Objects;
import org.jsoup.nodes.Element;

final class HtmlModifier {
    public static TextFileModification setAttribute(Element element, String name, String newValue, boolean html) {
        final var escapedValue = html ? forHtmlAttribute(newValue) : forXmlAttribute(newValue);
        final var attributes = element.attributes();
        final var attributesIterator = attributes.iterator();
        final var selfClosed = element.sourceRange().equals(element.endSourceRange());
        while (attributesIterator.hasNext()) {
            final var attribute = attributesIterator.next();
            if (Objects.equals(name, attribute.getKey())) {
                if (Objects.equals(newValue, attribute.getValue())) {
                    return null;
                }
                final var sourceRange = attribute.sourceRange();
                final var nameRange = sourceRange.valueRange();
                final var valueRange = sourceRange.valueRange();
                final var hasValue = element.attributes().hasDeclaredValueForKey(name);
                if (hasValue) {
                    final var badEmptyRange = nameRange.endPos() == valueRange.startPos();
                    if (badEmptyRange) {
                        // E.g. for <script src   =    ''>, JSoup does not report the actual position
                        // of the empty string (''), but the position immediately after the attribute name.
                        final var nextAttribute = attributesIterator.hasNext() ? attributesIterator.next() : null;
                        final var endPos = nextAttribute != null
                                ? nextAttribute.sourceRange().nameRange().startPos()
                                : element.sourceRange().endPos() - (selfClosed ? 2 : 1);
                        final var quotedValue = "=\"" + escapedValue + "\"" + (nextAttribute != null ? " " : "");
                        return new TextFileModification(valueRange.startPos(), endPos, quotedValue);
                    } else {
                        return new TextFileModification(valueRange.startPos(), valueRange.endPos(), escapedValue);
                    }
                } else {
                    final var quotedValue = "=\"" + escapedValue + "\"";
                    return new TextFileModification(valueRange.endPos(), valueRange.endPos(), quotedValue);
                }
            }
        }
        // Attribute not found
        final var endPos = selfClosed
                ? element.sourceRange().endPos() - 1
                : element.sourceRange().endPos();
        final var attributeValue = " " + name + "=\"" + escapedValue + "\"";
        return new TextFileModification(endPos - 1, endPos - 1, attributeValue);
    }
}
