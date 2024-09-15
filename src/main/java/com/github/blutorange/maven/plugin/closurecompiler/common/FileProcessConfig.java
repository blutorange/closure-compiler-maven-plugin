package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.SkipMode;

public final class FileProcessConfig {
    private final boolean skipMerge;
    private final boolean skipMinify;
    private final boolean force;
    private final int bufferSize;
    private final String lineSeparator;
    private final SkipMode skipMode;
    private final boolean allowReplacingInputFiles;

    public FileProcessConfig(
            String lineSeparator,
            int bufferSize,
            boolean force,
            boolean skipMerge,
            boolean skipMinify,
            SkipMode skipMode,
            boolean allowReplacingInputFiles) {
        this.lineSeparator = lineSeparator;
        this.bufferSize = bufferSize;
        this.force = force;
        this.skipMerge = skipMerge;
        this.skipMinify = skipMinify;
        this.skipMode = skipMode;
        this.allowReplacingInputFiles = allowReplacingInputFiles;
    }

    public boolean isAllowReplacingInputFiles() {
        return allowReplacingInputFiles;
    }

    public boolean isSkipMerge() {
        return skipMerge;
    }

    public boolean isSkipMinify() {
        return skipMinify;
    }

    public boolean isForce() {
        return force;
    }

    public SkipMode getSkipMode() {
        return skipMode;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }
}
