package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A file set consisting of includes and excludes. The includes are evaluated first, then the excludes are subtracted.
 */
public class FileSet {
    @SuppressWarnings("unused")
    private List<String> includes;

    @SuppressWarnings("unused")
    private List<String> excludes;

    /**
     * Gets the list of include patterns. Each pattern may use wildcards.
     *
     * @return Include patterns for scanning for matching files.
     */
    public List<String> getIncludes() {
        if (includes == null) {
            includes = new ArrayList<>();
        }
        return includes;
    }

    /**
     * Gets the list of exclude patterns. Each pattern may use wildcards. The excludes are applied after the includes.
     *
     * @return Exclude patterns for scanning for matching files.
     */
    public List<String> getExcludes() {
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        return excludes;
    }

    /**
     * Gets all files in the given base directory that match the includes and excludes patterns.
     *
     * @param baseDir Base directory to scan for files.
     * @return All matching files.
     */
    public List<File> getFiles(File baseDir) {
        return FileHelper.getIncludedFiles(baseDir, getIncludes(), getExcludes());
    }

    @Override
    public String toString() {
        return String.format("[includes=%s,excludes=%s]", getIncludes(), getExcludes());
    }
}
