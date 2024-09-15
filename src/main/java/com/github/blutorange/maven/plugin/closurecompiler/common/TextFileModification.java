package com.github.blutorange.maven.plugin.closurecompiler.common;

final class TextFileModification {
    private final int startPosition;
    private final int endPosition;
    private final String replacement;

    public TextFileModification(int startPosition, int endPosition, String replacement) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.replacement = replacement;
    }

    public String getReplacement() {
        return replacement;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public TextFileModification withOffset(int offset) {
        return new TextFileModification(startPosition + offset, endPosition + offset, replacement);
    }
}
