package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import java.io.File;
import java.util.List;
import org.apache.commons.collections4.ListUtils;

public class FileSet {
    @SuppressWarnings("unused")
    private List<String> includes;

    @SuppressWarnings("unused")
    private List<String> excludes;

    public List<String> getIncludes() {
        return ListUtils.emptyIfNull(includes);
    }

    public List<String> getExcludes() {
        return ListUtils.emptyIfNull(excludes);
    }

    public List<File> getFiles(File baseDir) {
        return FileHelper.getIncludedFiles(baseDir, getIncludes(), getExcludes());
    }

    @Override
    public String toString() {
        return String.format("[includes=%s,excludes=%s]", getIncludes(), getExcludes());
    }
}
