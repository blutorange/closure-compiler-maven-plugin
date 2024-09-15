package com.github.blutorange.maven.plugin.closurecompiler.common;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substring;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;

final class TextFileModifications {
    public static void applyAndWrite(File file, Charset encoding, List<TextFileModification> modifications)
            throws IOException {
        final var textContent = FileUtils.readFileToString(file, encoding);
        final var modifiedContent = apply(textContent, modifications);
        FileUtils.writeStringToFile(file, modifiedContent, encoding);
    }

    public static String apply(String textContent, List<TextFileModification> modifications) {
        final var sortedModifications = modifications.stream()
                .sorted(comparing(TextFileModification::getStartPosition).reversed())
                .collect(toList());
        for (final var modification : sortedModifications) {
            textContent = applyModification(textContent, modification);
        }
        return textContent;
    }

    private static String applyModification(String content, TextFileModification modification) {
        return substring(content, 0, modification.getStartPosition())
                + modification.getReplacement()
                + substring(content, modification.getEndPosition());
    }
}
