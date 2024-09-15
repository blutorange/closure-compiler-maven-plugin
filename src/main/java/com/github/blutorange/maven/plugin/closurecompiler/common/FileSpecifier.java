package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.util.List;

public final class FileSpecifier {
    private final File baseSourceDir;
    private final File baseTargetDir;
    private final String sourceDir;
    private final String targetDir;
    private final List<String> includes;
    private final List<String> excludes;
    private final String outputFilename;

    public FileSpecifier(
            File baseSourceDir,
            File baseTargetDir,
            String sourceDir,
            String targetDir,
            List<String> includes,
            List<String> excludes,
            String outputFilename) {
        this.baseSourceDir = baseSourceDir;
        this.baseTargetDir = baseTargetDir;
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.includes = includes;
        this.excludes = excludes;
        this.outputFilename = outputFilename;
    }

    public File getBaseSourceDir() {
        return baseSourceDir;
    }

    public File getBaseTargetDir() {
        return baseTargetDir;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public String getOutputFilename() {
        return outputFilename;
    }
}
