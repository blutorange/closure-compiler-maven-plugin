package com.github.blutorange.maven.plugin.closurecompiler.common;

import static com.github.blutorange.maven.plugin.closurecompiler.common.HtmlModifier.clearTextContent;
import static com.github.blutorange.maven.plugin.closurecompiler.common.HtmlModifier.setAttribute;
import static java.nio.charset.StandardCharsets.*;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.Test;

public class HtmlModifierTest {
    @Test
    public void testClearTextContent() throws IOException {
        // <div></div>
        assertClearTextContent("<div></div>", "<div></div>");
        assertClearTextContent("<div></div>", "<div>  </div>");
        assertClearTextContent("<div></div>", "<div>foo</div>");

        // <div/>
        assertClearTextContent("<div />", "<div />");

        // <div><span></span></div>
        assertClearTextContent("<div></div>", "<div><span></span></div>");
        assertClearTextContent("<div></div>", "<div><span></span><span></span></div>");
        assertClearTextContent("<div></div>", "<div><span>1</span>2<span>3</span></div>");
        assertClearTextContent("<div></div>", "<div> <span> 1 </span> 2 <span> 3 </span> </div>");

        // <div><span/></div>
        assertClearTextContent("<div></div>", "<div><span/></div>");
    }

    @Test
    public void testSetAttribute() throws IOException {
        // <script></script>
        assertSetAttribute(
                "<html><body><script src=\"qux\"></script></body></html>",
                "<html><body><script></script></body></html>",
                "qux");

        // <script  ></script  >
        assertSetAttribute(
                "<html><body><script   src=\"qux\"></script  ></body></html>",
                "<html><body><script  ></script  ></body></html>",
                "qux");

        // <script/>
        assertSetAttribute(
                "<html><body><script src=\"qux\"/></body></html>", "<html><body><script/></body></html>", "qux");

        // <script  />
        assertSetAttribute(
                "<html><body><script   src=\"qux\"/></body></html>", "<html><body><script  /></body></html>", "qux");

        // <script src="value"></script>
        assertSetAttribute(
                "<html><body><script src=''></script></body></html>",
                "<html><body><script src='foobar'></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src='qux'></script></body></html>",
                "<html><body><script src='foobar'></script></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src='resulting'></script></body></html>",
                "<html><body><script src='foobar'></script></body></html>",
                "resulting");

        // <script src  =  "value"></script>
        assertSetAttribute(
                "<html><body><script src  =  ''></script></body></html>",
                "<html><body><script src  =  'foobar'></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src  =  'qux'></script></body></html>",
                "<html><body><script src  =  'foobar'></script></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src  =  'resulting'></script></body></html>",
                "<html><body><script src  =  'foobar'></script></body></html>",
                "resulting");

        // <script src="value"/>
        assertSetAttribute(
                "<html><body><script src=''/></body></html>", "<html><body><script src='foobar'/></body></html>", "");
        assertSetAttribute(
                "<html><body><script src='qux'/></body></html>",
                "<html><body><script src='foobar'/></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src='resulting'/></body></html>",
                "<html><body><script src='foobar'/></body></html>",
                "resulting");

        // <script src  =  "value"  />
        assertSetAttribute(
                "<html><body><script src  =  ''  /></body></html>",
                "<html><body><script src  =  'foobar'  /></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src  =  'qux'  /></body></html>",
                "<html><body><script src  =  'foobar'  /></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src  =  'resulting'  /></body></html>",
                "<html><body><script src  =  'foobar'  /></body></html>",
                "resulting");

        // <script src></script>
        assertSetAttribute(
                "<html><body><script src></script></body></html>",
                "<html><body><script src></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"></script></body></html>",
                "<html><body><script src></script></body></html>",
                "qux");

        // <script src name=x></script>
        assertSetAttribute(
                "<html><body><script src name=x></script></body></html>",
                "<html><body><script src name=x></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\" name=x></script></body></html>",
                "<html><body><script src name=x></script></body></html>",
                "qux");

        // <script src  ></script>
        assertSetAttribute(
                "<html><body><script src  ></script></body></html>",
                "<html><body><script src  ></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"  ></script></body></html>",
                "<html><body><script src  ></script></body></html>",
                "qux");

        // <script src></script>
        assertSetAttribute("<html><body><script src/></body></html>", "<html><body><script src/></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"/></body></html>", "<html><body><script src/></body></html>", "qux");

        // <script src name=x></script>
        assertSetAttribute(
                "<html><body><script src name=x/></body></html>", "<html><body><script src name=x/></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\" name=x/></body></html>",
                "<html><body><script src name=x/></body></html>",
                "qux");

        // <script src  />
        assertSetAttribute(
                "<html><body><script src  /></body></html>", "<html><body><script src  /></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"  /></body></html>",
                "<html><body><script src  /></body></html>",
                "qux");

        // <script src=""></script>
        assertSetAttribute(
                "<html><body><script src=''></script></body></html>",
                "<html><body><script src=''></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"></script></body></html>",
                "<html><body><script src=''></script></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\"></script></body></html>",
                "<html><body><script src=''></script></body></html>",
                "resulting");

        // <script src="" name=x></script>
        assertSetAttribute(
                "<html><body><script src='' name=x></script></body></html>",
                "<html><body><script src='' name=x></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\" name=x></script></body></html>",
                "<html><body><script src='' name=x></script></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\" name=x></script></body></html>",
                "<html><body><script src='' name=x></script></body></html>",
                "resulting");

        // <script src  =  ""></script>
        assertSetAttribute(
                "<html><body><script src  =  ''></script></body></html>",
                "<html><body><script src  =  ''></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"></script></body></html>",
                "<html><body><script src  =  ''></script></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\"></script></body></html>",
                "<html><body><script src  =  ''></script></body></html>",
                "resulting");

        // <script src=""/>
        assertSetAttribute(
                "<html><body><script src=''/></body></html>", "<html><body><script src=''/></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"/></body></html>", "<html><body><script src=''/></body></html>", "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\"/></body></html>",
                "<html><body><script src=''/></body></html>",
                "resulting");

        // <script src="" name=x/>
        assertSetAttribute(
                "<html><body><script src='' name=x/></body></html>",
                "<html><body><script src='' name=x/></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\" name=x/></body></html>",
                "<html><body><script src='' name=x/></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\" name=x/></body></html>",
                "<html><body><script src='' name=x/></body></html>",
                "resulting");

        // <script src  =  ""  />
        assertSetAttribute(
                "<html><body><script src  =  ''  /></body></html>",
                "<html><body><script src  =  ''  /></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"/></body></html>",
                "<html><body><script src  =  ''  /></body></html>",
                "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\"/></body></html>",
                "<html><body><script src  =  ''  /></body></html>",
                "resulting");

        // Multiple script elements
        assertSetAttribute(
                "<html><head><script id='x' src='qux'></script></head><body><script id='y' src='qux'></script></body></html>",
                "<html><head><script id='x' src='a'></script></head><body><script id='y' src='b'></script></body></html>",
                "qux");

        // Unicode characters
        assertSetAttribute(
                "<html><body><script src='海猫'></script></body></html>",
                "<html><body><script src='foobar'></script></body></html>",
                "海猫");

        // Byte-order mark
        assertSetAttribute(
                "\ufeff<html><body><script src='海猫'></script></body></html>",
                "\ufeff<html><body><script src='foobar'></script></body></html>",
                "海猫");

        // Unicode characters
        assertSetAttribute(
                "<html><body><script src='&lt;&#39;&amp;&#34;>'></script></body></html>",
                "<html><body><script src='foobar'></script></body></html>",
                "<'&\">");
    }

    private void assertSetAttribute(String expected, String inputHtml, String value) throws IOException {
        assertReplaces(expected, inputHtml, "script", (element, html) -> setAttribute(element, "src", value, html));
    }

    private void assertClearTextContent(String expected, String inputHtml) throws IOException {
        assertReplaces(expected, inputHtml, "div", (element, html) -> clearTextContent(element));
    }

    private void assertReplaces(
            String expected,
            String inputHtml,
            String tagName,
            BiFunction<Element, Boolean, TextFileModification> createModification)
            throws IOException {
        assertReplaces(expected, inputHtml, tagName, createModification, UTF_8, true);
        assertReplaces(expected, inputHtml, tagName, createModification, UTF_8, false);
        assertReplaces(expected, inputHtml, tagName, createModification, UTF_16BE, true);
        assertReplaces(expected, inputHtml, tagName, createModification, UTF_16BE, false);
        assertReplaces(expected, inputHtml, tagName, createModification, UTF_16LE, true);
        assertReplaces(expected, inputHtml, tagName, createModification, UTF_16LE, false);
    }

    private void assertReplaces(
            String expected,
            String inputHtml,
            String tagName,
            BiFunction<Element, Boolean, TextFileModification> createModification,
            Charset encoding,
            boolean html)
            throws IOException {
        var parser = html ? Parser.htmlParser() : Parser.xmlParser();
        parser.setTrackErrors(100);
        parser.setTrackPosition(true);
        try (var input = new ByteArrayInputStream(inputHtml.getBytes(encoding))) {
            var doc = Jsoup.parse(input, encoding.name(), "mem:file.html", parser);
            var modifications = doc.select(tagName).stream()
                    .map(element -> createModification.apply(element, html))
                    .filter(Objects::nonNull)
                    .collect(toList());
            Collections.reverse(modifications);
            var adjustedModifications = adjustModifications(inputHtml, modifications);
            var actual = TextFileModifications.apply(inputHtml, adjustedModifications);
            assertEquals(expected, actual);
        }
    }

    private List<TextFileModification> adjustModifications(String inputHtml, List<TextFileModification> modifications) {
        final var startsWithBom = inputHtml.startsWith("\ufeff");
        if (startsWithBom) {
            return modifications.stream()
                    .map(modification -> modification.withOffset(1))
                    .collect(toList());
        } else {
            return modifications;
        }
    }
}
