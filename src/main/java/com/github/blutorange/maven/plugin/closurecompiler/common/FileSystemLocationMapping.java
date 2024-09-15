package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.google.javascript.jscomp.SourceMap.LocationMapping;
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

/** Makes the source file relative to the location of the source map. The paths are taken from the file system. */
final class FileSystemLocationMapping implements LocationMapping {
    private final File baseDirForSourceFiles;
    private final File sourceMapDir;
    private final Log log;
    private boolean transpilationDone;

    public FileSystemLocationMapping(Log log, File baseDirForSourceFiles, File sourceMapFile) {
        this.log = log;
        this.baseDirForSourceFiles = baseDirForSourceFiles;
        this.sourceMapDir = sourceMapFile.getParentFile();
    }

    @Override
    public String map(String location) {
        final String mapped;
        if (transpilationDone) {
            // This is the source file relative to the source map
            mapped = location;
        } else if (StringUtils.startsWith(StringUtils.trim(location), "[")) {
            // Internal files from closure compiler, such as "[synthetic:base]"
            mapped = location;
        } else {
            final File file = new File(baseDirForSourceFiles, location);
            mapped = FilenameUtils.separatorsToUnix(FileHelper.relativizePath(sourceMapDir, file));
        }
        log.debug("Source map: mapping location [" + location + "] to [" + mapped + "]");
        return mapped;
    }

    public void setTranspilationDone(boolean transpilationDone) {
        this.transpilationDone = transpilationDone;
    }
}
