package com.github.blutorange.maven.plugin.closurecompiler.plugin;

/**
 * Key-values pairs used by the <code>closureSourceMapLocationMappings</code> option of the minify plugin. Each pair
 * defines a prefix mapping from the file system path to the web server path.
 */
public class ClosureSourceMapLocationMapping {
    @SuppressWarnings("unused")
    private String name;

    @SuppressWarnings("unused")
    private String value;

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }
}
