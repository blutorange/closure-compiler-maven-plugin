package com.github.blutorange.maven.plugin.closurecompiler.common;

import static com.github.blutorange.maven.plugin.closurecompiler.common.HtmlModifier.setAttribute;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.function.BiFunction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.Test;

public class HtmlModifierTest {
    @Test
    public void testSetAttribute() {
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
                "<html><body><script src=\"\"></script></body></html>",
                "<html><body><script src></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"></script></body></html>",
                "<html><body><script src></script></body></html>",
                "qux");

        // <script src name=x></script>
        assertSetAttribute(
                "<html><body><script src=\"\" name=x></script></body></html>",
                "<html><body><script src name=x></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\" name=x></script></body></html>",
                "<html><body><script src name=x></script></body></html>",
                "qux");

        // <script src  ></script>
        assertSetAttribute(
                "<html><body><script src=\"\"  ></script></body></html>",
                "<html><body><script src  ></script></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"  ></script></body></html>",
                "<html><body><script src  ></script></body></html>",
                "qux");

        // <script src></script>
        assertSetAttribute(
                "<html><body><script src=\"\"/></body></html>", "<html><body><script src/></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"/></body></html>", "<html><body><script src/></body></html>", "qux");

        // <script src name=x></script>
        assertSetAttribute(
                "<html><body><script src=\"\" name=x/></body></html>",
                "<html><body><script src name=x/></body></html>",
                "");
        assertSetAttribute(
                "<html><body><script src=\"qux\" name=x/></body></html>",
                "<html><body><script src name=x/></body></html>",
                "qux");

        // <script src  />
        assertSetAttribute(
                "<html><body><script src=\"\"  /></body></html>", "<html><body><script src  /></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"  /></body></html>",
                "<html><body><script src  /></body></html>",
                "qux");

        // <script src=""></script>
        assertSetAttribute(
                "<html><body><script src=\"\"></script></body></html>",
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
                "<html><body><script src=\"\" name=x></script></body></html>",
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
                "<html><body><script src=\"\"></script></body></html>",
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
                "<html><body><script src=\"\"/></body></html>", "<html><body><script src=''/></body></html>", "");
        assertSetAttribute(
                "<html><body><script src=\"qux\"/></body></html>", "<html><body><script src=''/></body></html>", "qux");
        assertSetAttribute(
                "<html><body><script src=\"resulting\"/></body></html>",
                "<html><body><script src=''/></body></html>",
                "resulting");

        // <script src="" name=x/>
        assertSetAttribute(
                "<html><body><script src=\"\" name=x/></body></html>",
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
                "<html><body><script src=\"\"/></body></html>", "<html><body><script src  =  ''  /></body></html>", "");
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

        // Unicode characters
        assertSetAttribute(
                "<html><body><script src='&lt;&#39;&amp;&#34;>'></script></body></html>",
                "<html><body><script src='foobar'></script></body></html>",
                "<'&\">");
    }

    private void assertSetAttribute(String expected, String inputHtml, String value) {
        assertReplaces(expected, inputHtml, (element, html) -> setAttribute(element, "src", value, html));
    }

    private void assertReplaces(
            String expected, String inputHtml, BiFunction<Element, Boolean, TextFileModification> createModification) {
        assertReplaces(expected, inputHtml, createModification, true);
        assertReplaces(expected, inputHtml, createModification, false);
    }

    private void assertReplaces(
            String expected,
            String inputHtml,
            BiFunction<Element, Boolean, TextFileModification> createModification,
            boolean html) {
        var parser = html ? Parser.htmlParser() : Parser.xmlParser();
        parser.setTrackErrors(100);
        parser.setTrackPosition(true);
        var doc = Jsoup.parse(inputHtml, "utf-8", parser);
        var modifications = doc.select("script").stream()
                .map(element -> createModification.apply(element, html))
                .collect(toList());
        Collections.reverse(modifications);
        var actual = TextFileModifications.apply(inputHtml, modifications);
        assertEquals(expected, actual);
    }
}
