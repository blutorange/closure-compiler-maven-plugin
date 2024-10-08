package com.github.blutorange.maven.plugin.closurecompiler.plugin;

/** Enumeration for the possible options of whether processing a file should be skipped. */
public enum SkipMode {
    /** Do not recreate an output file when it is newer (more recently modified) that all input files. */
    NEWER,
    /** Do not recreate an output file when it exists already, irrespective of the modification date. */
    EXISTS;
}
