package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.IOException;
import org.sonatype.plexus.build.incremental.BuildContext;

class FileMessage {
    private final Throwable cause;

    private final int column;

    private final File file;

    private final int line;

    private final String message;

    private final int severity;

    public FileMessage(File file, int line, int column, String message, int severity, Throwable cause) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.severity = severity;
        this.cause = cause;
        this.message = message;
    }

    public Throwable getCause() {
        return cause;
    }

    public int getColumn() {
        return column;
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    public int getSeverity() {
        return severity;
    }

    public void addTo(BuildContext context) {
        if (context == null) {
            return;
        }
        File cFile = null;
        try {
            cFile = file.getCanonicalFile();
        } catch (IOException e) {
            cFile = file;
        }
        try {
            context.addMessage(cFile, line, column, message, severity, cause);
        } catch (Exception e) {
            // too bad...
            e.printStackTrace();
        }
    }
}
