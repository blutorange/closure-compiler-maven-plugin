package com.github.blutorange.maven.plugin.closurecompiler.plugin;

public enum LogLevel {
    all(0),
    debug(1),
    info(2),
    warn(3),
    error(4),
    none(5);
    private final int order;

    LogLevel(int order) {
        this.order = order;
    }

    public boolean isErrorEnabled() {
        return order >= error.order;
    }

    public boolean isWarnEnabled() {
        return order >= warn.order;
    }

    public boolean isInfoEnabled() {
        return order >= info.order;
    }

    public boolean isDebugEnabled() {
        return order >= debug.order;
    }
}
