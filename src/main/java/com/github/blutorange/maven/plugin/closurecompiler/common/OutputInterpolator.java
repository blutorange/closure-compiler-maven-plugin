package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Emulates the behavior of the closure compile command line flag `output_wrapper`:
 *
 * <blockquote>
 *
 * Interpolate output into this string at the place denoted by the marker token %output%. Use marker token
 * %output|jsstring% to do js string escaping on the output.
 *
 * </blockquote>
 *
 * @author madgaksha
 */
final class OutputInterpolator implements UnaryOperator<String> {

    private static final Pattern PATTERN = Pattern.compile("%output%|%output\\|jsstring%");

    private static final String TYPE_OUTPUT = "%output%";
    private static final String TYPE_OUTPUT_JSSTRING = "%output|jsstring%";

    private static final Token TOKEN_OUTPUT = new TokenOutput();
    private static final Token TOKEN_OUTPUT_JSSTRING = new TokenOutputJsstring();

    private final String pattern;
    private final List<Token> tokens;

    private OutputInterpolator(String pattern) {
        this.pattern = pattern;
        this.tokens = parse(pattern);
    }

    @Override
    public String apply(String source) {
        return tokens.stream().map(token -> token.apply(source)).collect(Collectors.joining());
    }

    public static OutputInterpolator forIdentity() {
        return new OutputInterpolator(TYPE_OUTPUT);
    }

    public static OutputInterpolator forPattern(String pattern) {
        return new OutputInterpolator(pattern);
    }

    @Override
    public String toString() {
        return String.format("OutputInterpolator[%s]", pattern);
    }

    private static List<Token> parse(String pattern) {
        final var tokens = new ArrayList<Token>();
        var pos = 0;
        var outputCount = 0;
        final var m = PATTERN.matcher(pattern);
        while (m.find()) {
            // Add literal text before match
            if (pos < m.start()) {
                tokens.add(new TokenLiteral(pattern.substring(pos, m.start())));
            }
            // Add matches token
            final var type = pattern.substring(m.start(), m.end());
            if (type.equals(TYPE_OUTPUT)) {
                outputCount += 1;
                tokens.add(TOKEN_OUTPUT);
            } else if (type.equals(TYPE_OUTPUT_JSSTRING)) {
                outputCount += 1;
                tokens.add(TOKEN_OUTPUT_JSSTRING);
            } else {
                throw new RuntimeException("Unknown token type: " + type);
            }
            pos = m.end();
        }
        if (pos < pattern.length()) {
            tokens.add(new TokenLiteral(pattern.substring(pos)));
        }
        if (outputCount > 1) {
            throw new IllegalArgumentException(
                    "Invalid pattern, must contain at most one [%output%] or [%output|jsstring%].");
        }
        return tokens;
    }

    private interface Token extends UnaryOperator<String> {
        String toSource();
    }

    private static class TokenLiteral implements Token {
        private final String text;

        public TokenLiteral(String text) {
            this.text = text;
        }

        @Override
        public String apply(String source) {
            return text;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(text);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TokenLiteral)) {
                return false;
            }
            final var other = (TokenLiteral) obj;
            return Objects.equals(text, other.text);
        }

        @Override
        public String toSource() {
            return text;
        }
    }

    private static final class TokenOutput implements Token {
        public TokenOutput() {}

        @Override
        public String apply(String source) {
            return source;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TokenOutput;
        }

        @Override
        public String toSource() {
            return TYPE_OUTPUT;
        }
    }

    private static final class TokenOutputJsstring implements Token {
        public TokenOutputJsstring() {}

        @Override
        public String apply(String source) {
            return StringEscapeUtils.escapeEcmaScript(source);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TokenOutputJsstring;
        }

        @Override
        public String toSource() {
            return TYPE_OUTPUT_JSSTRING;
        }
    }

    public String getWrapperPrefix() {
        if (tokens.isEmpty()) {
            return StringUtils.EMPTY;
        }
        var index = -1;
        for (int i = 0, j = tokens.size(); i < j; ++i) {
            if (TOKEN_OUTPUT.equals(tokens.get(i)) || TOKEN_OUTPUT_JSSTRING.equals(tokens.get(i))) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            index = tokens.size() - 1;
        }
        if (index == 0) {
            return StringUtils.EMPTY;
        }
        final var sb = new StringBuilder();
        tokens.stream().limit(index).map(Token::toSource).forEach(sb::append);
        return sb.toString();
    }
}
