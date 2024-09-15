package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.google.common.base.Preconditions;
import java.io.File;

public class ProcessingResult {
    private final boolean wasSkipped;
    private final File output;

    public ProcessingResult(Builder builder) {
        this.wasSkipped = builder.wasSkipped;
        this.output = builder.output;
    }

    public static Builder skipped() {
        final var builder = new ProcessingResult.Builder();
        builder.wasSkipped = true;
        return builder;
    }

    public static Builder success(File file) {
        Preconditions.checkNotNull(file, "Output file must not be null");
        final var builder = new ProcessingResult.Builder();
        builder.output = file;
        return builder;
    }

    public boolean isWasSkipped() {
        return wasSkipped;
    }

    public File getOutput() {
        return output;
    }

    public static final class Builder {
        private boolean wasSkipped;
        private File output;

        public ProcessingResult build() {
            return new ProcessingResult(this);
        }
    }
}
