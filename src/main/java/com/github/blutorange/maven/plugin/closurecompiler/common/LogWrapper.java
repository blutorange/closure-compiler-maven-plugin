package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.LogLevel;
import org.apache.maven.plugin.logging.Log;

/**
 * A wrapper for a logger that allows overwriting the log level.
 *
 * @author madgaksha
 */
public final class LogWrapper implements Log {
    private final Log wrapped;
    private final LogLevel level;

    /**
     * @param log Logger to wrap.
     * @param level Level to override the default log level of the wrapped logger. If <code>null</code>, do not
     *     overwrite the log level.
     */
    public LogWrapper(Log log, LogLevel level) {
        this.wrapped = log;
        this.level = level;
    }

    @Override
    public void debug(CharSequence content) {
        if (isDebugEnabled()) {
            wrapped.debug(content);
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (isDebugEnabled()) {
            wrapped.debug(content, error);
        }
    }

    @Override
    public void debug(Throwable error) {
        if (isDebugEnabled()) {
            wrapped.debug(error);
        }
    }

    @Override
    public void error(CharSequence content) {
        if (isErrorEnabled()) {
            wrapped.error(content);
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (isErrorEnabled()) {
            wrapped.error(content, error);
        }
    }

    @Override
    public void error(Throwable error) {
        if (isErrorEnabled()) {
            wrapped.error(error);
        }
    }

    @Override
    public void info(CharSequence content) {
        if (isInfoEnabled()) {
            wrapped.info(content);
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (isInfoEnabled()) {
            wrapped.info(content, error);
        }
    }

    @Override
    public void info(Throwable error) {
        if (isInfoEnabled()) {
            wrapped.info(error);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return level == null ? wrapped.isDebugEnabled() : level.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return level == null ? wrapped.isErrorEnabled() : level.isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return level == null ? wrapped.isInfoEnabled() : level.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return level == null ? wrapped.isWarnEnabled() : level.isWarnEnabled();
    }

    @Override
    public void warn(CharSequence content) {
        if (isWarnEnabled()) {
            wrapped.warn(content);
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (isWarnEnabled()) {
            wrapped.warn(content, error);
        }
    }

    @Override
    public void warn(Throwable error) {
        if (isWarnEnabled()) {
            wrapped.warn(error);
        }
    }
}
