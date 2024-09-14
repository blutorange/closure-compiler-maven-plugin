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
import com.github.blutorange.maven.plugin.closurecompiler.common.FileMessage;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileProcessConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileSpecifier;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileSystemLocationMapping;
import com.github.blutorange.maven.plugin.closurecompiler.common.OutputInterpolator;
import com.github.blutorange.maven.plugin.closurecompiler.common.ProcessingResult;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlBuilder;
import eu.maxschuster.dataurl.DataUrlEncoding;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
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
     * @throws IOException
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
     * @throws MojoFailureException
     */
    @Override
    protected ProcessingResult minify(File mergedFile, File minifiedFile) throws IOException, MojoFailureException {
        List<File> srcFiles = new ArrayList<File>();
        srcFiles.add(mergedFile);
        return minify(srcFiles, minifiedFile);
    }

    @Override
    protected ProcessingResult minify(List<File> srcFiles, File minifiedFile) throws IOException, MojoFailureException {
        File sourceMapFile = closureConfig
                .getSourceMapInterpolator()
                .interpolate(minifiedFile, minifiedFile.getParentFile(), minifiedFile.getParentFile());

        if (!haveFilesChanged(
                srcFiles,
                closureConfig.isCreateSourceMapFile()
                        ? Arrays.asList(minifiedFile, sourceMapFile)
                        : Collections.singleton(minifiedFile))) {
            return new ProcessingResult().setWasSkipped(true);
        }

        mkDir(targetDir);
        mkDir(minifiedFile.getParentFile());

        if (closureConfig.isCreateSourceMapFile()) {
            mkDir(sourceMapFile.getParentFile());
        }

        OutputInterpolator outputInterpolator = closureConfig.getOutputInterpolator();

        mojoMeta.getLog().info("Creating the minified file [" + minifiedFile.getName() + "].");
        mojoMeta.getLog().debug("Full path is [" + minifiedFile.getPath() + "].");

        File baseDirForSourceFiles = getBaseDirForSourceFiles(minifiedFile, sourceMapFile);

        mojoMeta.getLog()
                .debug("Setting base dir for closure source files to [" + baseDirForSourceFiles.getAbsolutePath()
                        + "]");

        List<SourceFile> sourceFileList = new ArrayList<SourceFile>();
        for (File srcFile : srcFiles) {
            try (InputStream in = new FileInputStream(srcFile)) {
                SourceFile input = SourceFile.builder() //
                        .withPath(FileHelper.relativizePath(baseDirForSourceFiles, srcFile)) //
                        .withCharset(mojoMeta.getEncoding()) //
                        .withContent(in) //
                        .build();
                sourceFileList.add(input);
            }
        }

        // Create compiler options
        FileSystemLocationMapping fileSystemMapping =
                new FileSystemLocationMapping(mojoMeta.getLog(), baseDirForSourceFiles, minifiedFile, sourceMapFile);
        CompilerOptions options = closureConfig.getCompilerOptions(
                fileSystemMapping, minifiedFile, sourceMapFile, baseDirForSourceFiles, sourceDir);

        if (mojoMeta.getLog().isDebugEnabled()) {
            mojoMeta.getLog()
                    .debug("Transpiling with closure source files: ["
                            + sourceFileList.stream().map(SourceFile::toString).collect(Collectors.joining(", "))
                            + "]");
            mojoMeta.getLog()
                    .debug("Transpiling from [" + options.getLanguageIn() + "] to [" + closureConfig.getLanguageOut()
                            + "], strict=" + options.shouldEmitUseStrict());
            mojoMeta.getLog().debug("Starting compilations with closure compiler options: " + options.toString());
        }

        // Set (external) libraries to be available
        List<SourceFile> externs = new ArrayList<>();
        externs.addAll(CommandLineRunner.getBuiltinExterns(closureConfig.getEnvironment()));
        externs.addAll(closureConfig.getExterns());

        // Now compile
        final Compiler compiler = new Compiler();
        compiler.compile(externs, sourceFileList, options);

        // Check for errors.
        checkForErrors(compiler, baseDirForSourceFiles);

        // Write compiled file to output file
        String compiled = compiler.toSource();

        OutputStream out = null;
        Writer writer = null;
        try {
            out = mojoMeta.getBuildContext().newFileOutputStream(minifiedFile);
            try {
                writer = new OutputStreamWriter(out, mojoMeta.getEncoding());
            } finally {
                // When new OutputStreamWriter threw an exception, writer is null
                if (writer == null && out != null) out.close();
            }
            writer.append(outputInterpolator.apply(compiled));

            // Create source map if configured.
            if (closureConfig.isCreateSourceMap()) {
                // Adjust source map for output wrapper.
                compiler.getSourceMap().setWrapperPrefix(outputInterpolator.getWrapperPrefix());
                fileSystemMapping.setTranspilationDone(true);
                createSourceMap(writer, compiler, minifiedFile, sourceMapFile);
            }

            // Make sure we end with a new line
            writer.append(processConfig.getLineSeparator());
        } finally {
            // Closing the OutputStream from m2e as well causes a StreamClosed exception in m2e
            // So we cannot use a try-with-resource
            if (writer != null) writer.close();
        }

        mojoMeta.getBuildContext().refresh(minifiedFile);

        logCompressionGains(srcFiles, compiled);

        return new ProcessingResult().setWasSkipped(false);
    }

    private File getBaseDirForSourceFiles(File minifiedFile, File sourceMapFile) throws IOException {
        return this.sourceDir;
    }

    private void checkForErrors(Compiler compiler, File baseDirForSourceFiles) {
        // Add file warnings
        compiler.getWarnings().stream().forEach(warning -> {
            ClosureCompileFileMessage.ofWarning(warning, compiler, baseDirForSourceFiles)
                    .addTo(mojoMeta.getBuildContext());
        });

        List<JSError> errors = new ArrayList<>(compiler.getErrors());
        if (!errors.isEmpty()) {
            Iterable<FileMessage> fileErrors = errors.stream()
                    .map(error -> ClosureCompileFileMessage.ofError(error, compiler, baseDirForSourceFiles))::iterator;
            throw new FileException(fileErrors);
        }
    }

    private void createSourceMap(Writer writer, Compiler compiler, File minifiedFile, File sourceMapFile)
            throws IOException {
        String pathToSource =
                FilenameUtils.separatorsToUnix(FileHelper.relativizePath(sourceMapFile.getParentFile(), minifiedFile));
        mojoMeta.getLog().debug("Setting path to source in source map to [" + pathToSource + "].");
        switch (closureConfig.getSourceMapOutputType()) {
            case inline:
                mojoMeta.getLog().info("Creating the inline source map.");
                StringBuilder sb = new StringBuilder();
                compiler.getSourceMap().appendTo(sb, pathToSource);
                DataUrl unserialized = new DataUrlBuilder() //
                        .setMimeType("application/json") //
                        .setEncoding(DataUrlEncoding.BASE64) //
                        .setData(sb.toString().getBytes(StandardCharsets.UTF_8)) //
                        .setHeader("charset", "utf-8") //
                        .build();
                IDataUrlSerializer serializer = new DataUrlSerializer();
                String dataUrl = serializer.serialize(unserialized);
                writer.append(processConfig.getLineSeparator());
                writer.append("//# sourceMappingURL=" + dataUrl);
                break;
            case file:
                flushSourceMap(sourceMapFile, pathToSource, compiler.getSourceMap());
                break;
            case reference:
                mojoMeta.getLog().info("Creating reference to source map.");
                String pathToMap = FilenameUtils.separatorsToUnix(
                        FileHelper.relativizePath(minifiedFile.getParentFile(), sourceMapFile));
                flushSourceMap(sourceMapFile, pathToSource, compiler.getSourceMap());
                writer.append(processConfig.getLineSeparator());
                writer.append("//# sourceMappingURL=" + pathToMap);
                break;
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

        OutputStream out = null;
        Writer writer = null;
        try {
            out = mojoMeta.getBuildContext().newFileOutputStream(sourceMapFile);
            try {
                writer = new OutputStreamWriter(out, mojoMeta.getEncoding());
            } finally {
                // When new OutputStreamWriter threw an exception, writer is null
                if (writer == null && out != null) out.close();
            }

            sourceMap.appendTo(writer, pathToSource);
        } catch (IOException e) {
            mojoMeta.getLog()
                    .error("Failed to write the JavaScript Source Map file [" + sourceMapFile.getName() + "].", e);
            mojoMeta.getLog().debug("Full path is [" + sourceMapFile.getPath() + "]");
        } finally {
            // Closing the OutputStream from m2e as well causes a StreamClosed exception in m2e
            // So we cannot use a try-with-resource
            if (writer != null) writer.close();
        }
    }
}
