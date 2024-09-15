package com.github.blutorange.maven.plugin.closurecompiler.plugin;

public enum SourceMapOutputType {
    /** Just create a source map file named *.map. */
    file(true),
    /** Inline the content of the source map in the minified file. */
    inline(false),
    /** Create a source map file *.map and add a reference to it in the minified file. */
    reference(true);

    private final boolean createFile;

    SourceMapOutputType(boolean createFile) {
        this.createFile = createFile;
    }

    public boolean isCreateFile() {
        return createFile;
    }
}
