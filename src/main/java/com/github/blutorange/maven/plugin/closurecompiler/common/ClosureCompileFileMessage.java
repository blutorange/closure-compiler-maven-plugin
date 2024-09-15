package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.JSError;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

final class ClosureCompileFileMessage extends FileMessage {
    private ClosureCompileFileMessage(File file, int line, int column, String message, int severity, Throwable cause) {
        super(file, line, column, message, severity, cause);
    }

    private static FileMessage of(JSError error, Compiler compiler, File baseDir, int severity) {
        String message = format(compiler, error, severity, false);
        File file = StringUtils.isNotEmpty(error.getSourceName()) ? new File(baseDir, error.getSourceName()) : baseDir;
        return new ClosureCompileFileMessage(file, error.getLineNumber(), error.getCharno(), message, severity, null);
    }

    public static FileMessage ofError(JSError error, Compiler compiler, File baseDir) {
        return of(error, compiler, baseDir, BuildContext.SEVERITY_ERROR);
    }

    public static FileMessage ofWarning(JSError error, Compiler compiler, File baseDir) {
        return of(error, compiler, baseDir, BuildContext.SEVERITY_WARNING);
    }

    private static String format(Compiler source, JSError error, int severity, boolean includeLocation) {
        String sourceName = error.getSourceName();
        int lineNumber = error.getLineNumber();

        // Format the non-reverse-mapped position.
        StringBuilder b = new StringBuilder();
        StringBuilder boldLine = new StringBuilder();
        String nonMappedPosition = formatPosition(sourceName, lineNumber);

        // Check if we can reverse-map the source.
        if (includeLocation) {
            OriginalMapping mapping = source == null
                    ? null
                    : source.getSourceMapping(error.getSourceName(), error.getLineNumber(), error.getCharno());
            if (mapping == null) {
                boldLine.append(nonMappedPosition);
            } else {
                sourceName = mapping.getOriginalFile();
                lineNumber = mapping.getLineNumber();

                b.append(nonMappedPosition);
                b.append("\nOriginally at:\n");
                boldLine.append(formatPosition(sourceName, lineNumber));
            }
        }

        boldLine.append(severity == BuildContext.SEVERITY_WARNING ? "WARNING" : "ERROR");
        boldLine.append(" - ");

        boldLine.append(error.getDescription());

        b.append(boldLine);
        b.append('\n');
        return b.toString();
    }

    private static String formatPosition(String sourceName, int lineNumber) {
        StringBuilder b = new StringBuilder();
        if (sourceName != null) {
            b.append(sourceName);
            if (lineNumber > 0) {
                b.append(':');
                b.append(lineNumber);
            }
            b.append(": ");
        }
        return b.toString();
    }
}
