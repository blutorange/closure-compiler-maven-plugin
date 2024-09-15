package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;

final class FilenameInterpolator {
    private final String pattern;
    private final String prefix;
    private final String suffix;
    private final char escapeChar;

    public FilenameInterpolator(String pattern) {
        this(pattern, "#{", "}", '#');
    }

    public FilenameInterpolator(String pattern, String prefix, String suffix, char escapeChar) {
        this.pattern = pattern;
        this.prefix = prefix;
        this.suffix = suffix;
        this.escapeChar = escapeChar;
    }

    public File interpolate(File inputFile, File inputBaseDir, File targetDirectory) {
        return interpolate(inputFile, inputBaseDir, targetDirectory, null);
    }

    public File interpolate(
            File inputFile, File inputBaseDir, File targetDirectory, Map<String, String> additionalData) {
        final var interpolatedFilename = interpolateRelative(inputFile, inputBaseDir, additionalData);
        return new File(targetDirectory, interpolatedFilename);
    }

    public String interpolateRelative(File inputFile, File inputBaseDir) {
        return interpolateRelative(inputFile, inputBaseDir, null);
    }

    public String interpolateRelative(File inputFile, File inputBaseDir, Map<String, String> additionalData) {
        final var inputFilename = inputFile.getName();
        final var data = new HashMap<String, String>();
        data.put("filename", inputFilename);
        data.put("extension", FilenameUtils.getExtension(inputFilename));
        data.put("basename", FilenameUtils.getBaseName(inputFilename));
        data.put("path", FileHelper.relativizePath(inputBaseDir, inputFile.getParentFile()));
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        final var stringSubstitutor = new StringSubstitutor(data, prefix, suffix, escapeChar);
        return stringSubstitutor.replace(pattern);
    }
}
