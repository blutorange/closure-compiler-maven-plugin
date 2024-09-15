/*
 * Closure Compiler Maven Plugin https://github.com/blutorange/closure-compiler-maven-plugin Original license terms
 * below. Changes were made to this file.
 */

/*
 * Minify Maven Plugin https://github.com/samaxes/minify-maven-plugin Copyright (c) 2009 samaxes.com Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureCompileFileMessage;
import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileException;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileProcessConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileSpecifier;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileSystemLocationMapping;
import com.github.blutorange.maven.plugin.closurecompiler.common.ProcessingResult;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;
import eu.maxschuster.dataurl.DataUrlBuilder;
import eu.maxschuster.dataurl.DataUrlEncoding;
import eu.maxschuster.dataurl.DataUrlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoFailureException;

/** Task for merging and compressing JavaScript files. */
public class ProcessJSFilesTask extends ProcessFilesTask {

    /**
     * Task constructor.
     *
     * @param mojoMeta Mojo meta (for log, project etc.)
     * @param processConfig Details about the process files task.
     * @param fileSpecifier Details about the input / output files.
     * @param closureConfig Google Closure Compiler configuration
     * @throws IOException When an input file could not be read of an output file could not be written.
     */
    public ProcessJSFilesTask(
            MojoMetadata mojoMeta,
            FileProcessConfig processConfig,
            FileSpecifier fileSpecifier,
            ClosureConfig closureConfig)
            throws IOException {
        super(mojoMeta, processConfig, fileSpecifier, closureConfig);
    }

    /**
     * Minifies a JavaScript file. Create missing parent directories if needed.
     *
     * @param mergedFile input file resulting from the merged step
     * @param minifiedFile output file resulting from the minify step
     * @throws IOException when the minify step fails
     * @throws MojoFailureException When the file could not be minified, either due to an I/O error or due to a closure
     *     compiler failure.
     */
    @Override
    protected ProcessingResult minify(File mergedFile, File minifiedFile) throws IOException, MojoFailureException {
        final var srcFiles = new ArrayList<File>();
        srcFiles.add(mergedFile);
        return minify(srcFiles, minifiedFile);
    }

    @Override
    protected ProcessingResult minify(List<File> srcFiles, File minifiedFile) throws IOException, MojoFailureException {
        final var sourceMapFile = closureConfig
                .getSourceMapInterpolator()
                .interpolate(minifiedFile, minifiedFile.getParentFile(), minifiedFile.getParentFile());

        if (!haveFilesChanged(
                srcFiles,
                closureConfig.isCreateSourceMapFile()
                        ? Arrays.asList(minifiedFile, sourceMapFile)
                        : Collections.singleton(minifiedFile))) {
            return ProcessingResult.skipped(minifiedFile).build();
        }

        mkDir(targetDir);
        mkDir(minifiedFile.getParentFile());

        if (closureConfig.isCreateSourceMapFile()) {
            mkDir(sourceMapFile.getParentFile());
        }

        final var outputInterpolator = closureConfig.getOutputInterpolator();

        mojoMeta.getLog().info("Creating the minified file [" + minifiedFile.getName() + "].");
        mojoMeta.getLog().debug("Full path is [" + minifiedFile.getPath() + "].");

        final var baseDirForSourceFiles = getBaseDirForSourceFiles(minifiedFile, sourceMapFile);

        mojoMeta.getLog()
                .debug("Setting base dir for closure source files to [" + baseDirForSourceFiles.getAbsolutePath()
                        + "]");

        final var sourceFileList = new ArrayList<SourceFile>();
        for (final var srcFile : srcFiles) {
            try (InputStream in = new FileInputStream(srcFile)) {
                SourceFile input = SourceFile.builder()
                        .withPath(FileHelper.relativizePath(baseDirForSourceFiles, srcFile))
                        .withCharset(mojoMeta.getEncoding())
                        .withContent(in)
                        .build();
                sourceFileList.add(input);
            }
        }

        // Create compiler options
        final var fileSystemMapping =
                new FileSystemLocationMapping(mojoMeta.getLog(), baseDirForSourceFiles, sourceMapFile);
        final var options = closureConfig.getCompilerOptions(
                fileSystemMapping, minifiedFile, sourceMapFile, baseDirForSourceFiles, sourceDir);

        if (mojoMeta.getLog().isDebugEnabled()) {
            mojoMeta.getLog()
                    .debug("Transpiling with closure source files: ["
                            + sourceFileList.stream().map(SourceFile::toString).collect(Collectors.joining(", "))
                            + "]");
            mojoMeta.getLog()
                    .debug("Transpiling from [" + options.getLanguageIn() + "] to [" + closureConfig.getLanguageOut()
                            + "], strict=" + options.shouldEmitUseStrict());
            mojoMeta.getLog().debug("Starting compilations with closure compiler options: " + options);
        }

        // Set (external) libraries to be available
        final var externs = new ArrayList<SourceFile>();
        externs.addAll(CommandLineRunner.getBuiltinExterns(closureConfig.getEnvironment()));
        externs.addAll(closureConfig.getExterns());

        // Now compile
        final var compiler = new Compiler();
        compiler.compile(externs, sourceFileList, options);

        // Check for errors.
        checkForErrors(compiler, baseDirForSourceFiles);

        // Write compiled file to output file
        final var compiled = compiler.toSource();

        OutputStream output;
        Writer outputWriter = null;
        try {
            output = mojoMeta.getBuildContext().newFileOutputStream(minifiedFile);
            try {
                outputWriter = new OutputStreamWriter(output, mojoMeta.getEncoding());
            } finally {
                // When new OutputStreamWriter threw an exception, writer is null
                if (outputWriter == null && output != null) {
                    output.close();
                }
            }
            outputWriter.append(outputInterpolator.apply(compiled));

            // Create source map if configured.
            if (closureConfig.isCreateSourceMap()) {
                // Adjust source map for output wrapper.
                compiler.getSourceMap().setWrapperPrefix(outputInterpolator.getWrapperPrefix());
                fileSystemMapping.setTranspilationDone(true);
                createSourceMap(outputWriter, compiler, minifiedFile, sourceMapFile);
            }

            // Make sure we end with a new line
            outputWriter.append(processConfig.getLineSeparator());
        } finally {
            // Closing the OutputStream from m2e as well causes a StreamClosed exception in m2e
            // So we cannot use a try-with-resource
            if (outputWriter != null) {
                outputWriter.close();
            }
        }

        mojoMeta.getBuildContext().refresh(minifiedFile);

        logCompressionGains(srcFiles, compiled);

        return ProcessingResult.success(minifiedFile).build();
    }

    private File getBaseDirForSourceFiles(File minifiedFile, File sourceMapFile) {
        return this.sourceDir;
    }

    private void checkForErrors(Compiler compiler, File baseDirForSourceFiles) {
        // Add warning to build context, so it shows up in IDEs etc.
        for (final var warning : compiler.getWarnings()) {
            ClosureCompileFileMessage.ofWarning(warning, compiler, baseDirForSourceFiles)
                    .addTo(mojoMeta.getBuildContext());
        }

        final var errors = new ArrayList<>(compiler.getErrors());
        if (!errors.isEmpty()) {
            final var fileErrors = errors.stream()
                    .map(error -> ClosureCompileFileMessage.ofError(error, compiler, baseDirForSourceFiles));
            throw new FileException(fileErrors::iterator);
        }
    }

    private void createSourceMap(Writer writer, Compiler compiler, File minifiedFile, File sourceMapFile)
            throws IOException {
        final var pathToSource =
                FilenameUtils.separatorsToUnix(FileHelper.relativizePath(sourceMapFile.getParentFile(), minifiedFile));
        mojoMeta.getLog().debug("Setting path to source in source map to [" + pathToSource + "].");
        switch (closureConfig.getSourceMapOutputType()) {
            case inline: {
                mojoMeta.getLog().info("Creating the inline source map.");
                final var sb = new StringBuilder();
                compiler.getSourceMap().appendTo(sb, pathToSource);
                final var dataUrl = new DataUrlBuilder()
                        .setMimeType("application/json")
                        .setEncoding(DataUrlEncoding.BASE64)
                        .setData(sb.toString().getBytes(StandardCharsets.UTF_8))
                        .setHeader("charset", "utf-8")
                        .build();
                final var serializer = new DataUrlSerializer();
                final var serializedDataUrl = serializer.serialize(dataUrl);
                writer.append(processConfig.getLineSeparator());
                writer.append("//# sourceMappingURL=").append(serializedDataUrl);
                break;
            }
            case file:
                flushSourceMap(sourceMapFile, pathToSource, compiler.getSourceMap());
                break;
            case reference: {
                mojoMeta.getLog().info("Creating reference to source map.");
                final var pathToMap = FilenameUtils.separatorsToUnix(
                        FileHelper.relativizePath(minifiedFile.getParentFile(), sourceMapFile));
                flushSourceMap(sourceMapFile, pathToSource, compiler.getSourceMap());
                writer.append(processConfig.getLineSeparator());
                writer.append("//# sourceMappingURL=").append(pathToMap);
                break;
            }
            default:
                mojoMeta.getLog()
                        .warn("Unknown source map inclusion type [" + closureConfig.getSourceMapOutputType() + "]");
                throw new RuntimeException(
                        "unknown source map inclusion type: " + closureConfig.getSourceMapOutputType());
        }
    }

    private void flushSourceMap(File sourceMapFile, String pathToSource, SourceMap sourceMap) throws IOException {
        mojoMeta.getLog().info("Creating the source map [" + sourceMapFile.getName() + "].");
        mojoMeta.getLog().debug("Full path is [" + sourceMapFile.getPath() + "].");

        OutputStream output;
        Writer outputWriter = null;
        try {
            output = mojoMeta.getBuildContext().newFileOutputStream(sourceMapFile);
            try {
                outputWriter = new OutputStreamWriter(output, mojoMeta.getEncoding());
            } finally {
                // When new OutputStreamWriter threw an exception, writer is null
                if (outputWriter == null && output != null) output.close();
            }

            sourceMap.appendTo(outputWriter, pathToSource);
        } catch (IOException e) {
            mojoMeta.getLog()
                    .error("Failed to write the JavaScript Source Map file [" + sourceMapFile.getName() + "].", e);
            mojoMeta.getLog().debug("Full path is [" + sourceMapFile.getPath() + "]");
        } finally {
            // Closing the OutputStream from m2e as well causes a StreamClosed exception in m2e
            // So we cannot use a try-with-resource
            if (outputWriter != null) {
                outputWriter.close();
            }
        }
    }
}
