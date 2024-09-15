package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSet {
    @SuppressWarnings("unused")
    private List<String> includes;

    @SuppressWarnings("unused")
    private List<String> excludes;

    public List<String> getIncludes() {
        if (includes == null) {
            includes = new ArrayList<>();
        }
        return includes;
    }

    public List<String> getExcludes() {
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        return excludes;
    }

    public List<File> getFiles(File baseDir) {
        return FileHelper.getIncludedFiles(baseDir, getIncludes(), getExcludes());
    }

    @Override
    public String toString() {
        return String.format("[includes=%s,excludes=%s]", getIncludes(), getExcludes());
    }
}
