package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.google.javascript.jscomp.DependencyOptions.DependencyMode;

public enum DependencyModeFlag {
    NONE,
    SORT_ONLY,
    PRUNE_LEGACY,
    PRUNE,
    LOOSE,
    STRICT;

    public static DependencyMode toDependencyMode(DependencyModeFlag flag) {
        if (flag == null) {
            return null;
        }
        switch (flag) {
            case NONE:
                return DependencyMode.NONE;
            case SORT_ONLY:
                return DependencyMode.SORT_ONLY;
            case PRUNE_LEGACY:
            case LOOSE:
                return DependencyMode.PRUNE_LEGACY;
            case PRUNE:
            case STRICT:
                return DependencyMode.PRUNE;
        }
        throw new AssertionError("Bad DependencyModeFlag");
    }
}
