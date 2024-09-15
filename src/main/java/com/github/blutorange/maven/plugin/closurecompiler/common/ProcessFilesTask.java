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
package com.github.blutorange.maven.plugin.closurecompiler.common;

import static java.io.OutputStream.nullOutputStream;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.SkipMode;
import com.github.blutorange.maven.plugin.closurecompiler.shared.MojoMetadata;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.plexus.build.incremental.BuildContext;

/** Abstract class for merging and compressing a files list. */
public abstract class ProcessFilesTask implements Callable<List<ProcessingResult>> {
    private static final String DEFAULT_MERGED_FILENAME = "script.js";

    /**
     * Logs an addition of a new source file.
     *
     * @param files A list of files required for compilation, such as files imported by other files.
     * @param sourceFile The source file to process, i.e. the main entry point.
     * @param mojoMeta Mojo data with e.g. the logger.
     * @throws IOException When an input file could not be read or an output file could not be written.
     */
    private static void addNewSourceFile(Collection<File> files, File sourceFile, MojoMetadata mojoMeta)
            throws IOException {
        if (sourceFile.exists()) {
            mojoMeta.getLog().debug("Adding source file [" + sourceFile.getPath() + "].");
            files.add(sourceFile.getCanonicalFile());
        } else {
            throw new FileNotFoundException("The source file [" + sourceFile.getPath() + "] does not exist.");
        }
    }

    protected final MojoMetadata mojoMeta;

    protected final ClosureConfig closureConfig;

    protected final List<File> files = new ArrayList<>();

    protected final boolean includesEmpty;

    protected final FilenameInterpolator outputFilenameInterpolator;

    protected final FileProcessConfig processConfig;

    protected final File sourceDir;

    protected final File targetDir;

    /**
     * Task constructor.
     *
     * @param mojoMeta Base mojo data.
     * @param processConfig Configuration for this file task.
     * @param fileSpecifier Details about the input / output files.
     * @param closureConfig Google closure configuration
     * @throws IOException When an input file could not be read or an output file could not be written.
     */
    public ProcessFilesTask(
            MojoMetadata mojoMeta,
            FileProcessConfig processConfig,
            FileSpecifier fileSpecifier,
            ClosureConfig closureConfig)
            throws IOException {
        this.mojoMeta = mojoMeta;
        this.processConfig = processConfig;

        final var projectBasedir = mojoMeta.getProject().getBasedir();
        this.sourceDir = FileHelper.getFile(
                        FileHelper.getAbsoluteFile(projectBasedir, fileSpecifier.getBaseSourceDir()),
                        fileSpecifier.getSourceDir())
                .getAbsoluteFile()
                .getCanonicalFile();
        this.targetDir = FileHelper.getFile(
                        FileHelper.getAbsoluteFile(projectBasedir, fileSpecifier.getBaseTargetDir()),
                        fileSpecifier.getTargetDir())
                .getAbsoluteFile()
                .getCanonicalFile();
        this.outputFilenameInterpolator = new FilenameInterpolator(fileSpecifier.getOutputFilename());

        for (final var include :
                FileHelper.getIncludedFiles(this.sourceDir, fileSpecifier.getIncludes(), fileSpecifier.getExcludes())) {
            if (!files.contains(include)) {
                addNewSourceFile(files, include, mojoMeta);
            }
        }

        this.includesEmpty = fileSpecifier.getIncludes().isEmpty();
        this.closureConfig = closureConfig;
    }

    private void assertTarget(File source, File target) throws MojoFailureException {
        if (!processConfig.isAllowReplacingInputFiles()
                && target.getAbsolutePath().equals(source.getAbsolutePath())) {
            String msg;
            msg = "The source file [" + source.getName() + "] has the same name as the output file [" + target.getName()
                    + "].";
            mojoMeta.getLog().warn(msg);
            mojoMeta.getLog()
                    .debug("Full path for source file is [" + source.getPath() + "] and for target file ["
                            + target.getPath() + "]");
            throw new MojoFailureException(msg);
        }
    }

    /**
     * Method executed by the thread.
     *
     * @throws IOException When an input file could not be read or an output file could not be written.
     * @throws MojoFailureException When the merge or minify steps fail
     */
    @Override
    public List<ProcessingResult> call() throws IOException, MojoFailureException {
        synchronized (mojoMeta.getLog()) {
            mojoMeta.getLog().info("Starting JavaScript task:");
            if (!files.isEmpty()) {
                try {
                    return processFiles();
                } catch (FileException e) {
                    logFailure(e);
                    throw new MojoFailureException("Closure compilation failure", e);
                } catch (final Exception e) {
                    logFailure(e);
                    files.forEach(file -> mojoMeta.getBuildContext()
                            .addMessage(file, 1, 1, e.getMessage(), BuildContext.SEVERITY_ERROR, e));
                    throw e;
                }
            }
            if (!includesEmpty) {
                // 'files' list will be empty if source file paths or names added to the project's POM are invalid.
                mojoMeta.getLog().warn("No valid JavaScript source files found to process.");
            }
            return List.of();
        }
    }

    private void logFailure(Exception e) {
        if (e instanceof FileException) {
            ((FileException) e).getFileErrors().forEach(fileError -> fileError.addTo(mojoMeta.getBuildContext()));
        }
        mojoMeta.getLog()
                .error(
                        "Failed to process the source files ["
                                + files.stream().map(File::getName).collect(Collectors.joining(", ")) + "].",
                        e);
        mojoMeta.getLog()
                .debug("Full path is [" + files.stream().map(File::getPath).collect(Collectors.joining(", ")) + "]");
    }

    /**
     * Copy, merge and / or minify the input files.
     *
     * @return A list with all processed files.
     * @throws IOException When an input file could not be read or an output file could not be written.
     * @throws MojoFailureException When the file could not be processed, such as when the output file is the same as
     *     the input file.
     */
    private List<ProcessingResult> processFiles() throws IOException, MojoFailureException {
        // No merge
        final var results = new ArrayList<ProcessingResult>();
        if (processConfig.isSkipMerge()) {
            mojoMeta.getLog().info("Skipping the merge step...");

            for (final var sourceFile : files) {
                final var minifiedFile = outputFilenameInterpolator.interpolate(sourceFile, sourceDir, targetDir);
                assertTarget(sourceFile, minifiedFile);
                // Neither merge nor minify
                if (processConfig.isSkipMinify()) {
                    results.add(copy(sourceFile, minifiedFile));
                }
                // Minify-only
                else {
                    results.add(minify(sourceFile, minifiedFile));
                }
            }
        }
        // Merge-only
        else if (processConfig.isSkipMinify()) {
            final var mergedFile = outputFilenameInterpolator.interpolate(
                    new File(targetDir, DEFAULT_MERGED_FILENAME), targetDir, targetDir);
            results.add(merge(mergedFile));
            mojoMeta.getLog().info("Skipping the minify step...");
        }
        // Minify + merge
        else {
            final var minifiedFile = outputFilenameInterpolator.interpolate(
                    new File(targetDir, DEFAULT_MERGED_FILENAME), targetDir, targetDir);
            results.add(minify(files, minifiedFile));
        }

        logResults(results);
        return List.copyOf(results);
    }

    protected final void removeMessages(Collection<File> files) {
        files.forEach(file -> mojoMeta.getBuildContext().removeMessages(file));
    }

    /**
     * Logs compression gains.
     *
     * @param srcFiles list of input files to compress
     * @param minified output file resulting from the minify step
     */
    protected final void logCompressionGains(List<File> srcFiles, String minified) {
        if (!mojoMeta.getLog().isInfoEnabled() || mojoMeta.getBuildContext().isIncremental()) {
            return;
        }
        try {
            final var minifiedData = minified.getBytes(mojoMeta.getEncoding());
            final var compressedSize = minifiedData.length;

            long compressedSizeGzip;
            try (final var input = new ByteArrayInputStream(minifiedData);
                    final var countingOutputStream = new CountingOutputStream(nullOutputStream());
                    final var gzipOutputStream = new GZIPOutputStream(countingOutputStream)) {
                IOUtils.copy(input, gzipOutputStream, processConfig.getBufferSize());
                gzipOutputStream.finish();
                compressedSizeGzip = countingOutputStream.getByteCount();
            }

            var uncompressedSize = 0L;
            if (srcFiles != null) {
                for (File srcFile : srcFiles) {
                    uncompressedSize += srcFile.length();
                }
            }

            mojoMeta.getLog().info("Uncompressed size: " + uncompressedSize + " bytes.");
            mojoMeta.getLog()
                    .info("Compressed size: " + compressedSize + " bytes minified (" + compressedSizeGzip
                            + " bytes gzipped).");
        } catch (IOException e) {
            mojoMeta.getLog().debug("Failed to calculate the gzipped file size.", e);
        }
    }

    /**
     * Creates the given directory (and parents) and informs the build context.
     *
     * @param directory Path of the directory to create.
     */
    protected final void mkDir(File directory) {
        if (directory.exists()) {
            return;
        }
        var firstThatExists = directory;
        do {
            firstThatExists = firstThatExists.getParentFile();
        } while (firstThatExists != null && !firstThatExists.exists());
        try {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Unable to create target directory: " + directory.getPath());
            }
        } finally {
            if (firstThatExists != null) {
                mojoMeta.getBuildContext().refresh(firstThatExists);
            }
        }
    }

    /**
     * Copies sourceFile to targetFile, making sure to inform the build context of the change.
     *
     * @param sourceFile The source file to copy.
     * @param targetFile The target file to which to copy the source file.
     * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
     *     change).
     * @throws IOException When an input file could not be read or an output file could not be written.
     */
    protected final ProcessingResult copy(File sourceFile, File targetFile) throws IOException {
        if (!haveFilesChanged(Collections.singleton(sourceFile), Collections.singleton(targetFile))) {
            return ProcessingResult.skipped(targetFile).build();
        }

        mkDir(targetDir);
        mkDir(targetFile.getParentFile());

        InputStream input;
        OutputStream output;
        Reader inputReader = null;
        Writer outputWriter = null;
        try {
            input = new FileInputStream(sourceFile);
            output = mojoMeta.getBuildContext().newFileOutputStream(targetFile);
            try {
                inputReader = new InputStreamReader(input, mojoMeta.getEncoding());
            } finally {
                // When new InputStreamReader threw an exception, reader is null
                if (inputReader == null && input != null) {
                    input.close();
                }
            }
            try {
                outputWriter = new OutputStreamWriter(output, mojoMeta.getEncoding());
            } finally {
                // When new OutputStreamWriter threw an exception, writer is null
                if (outputWriter == null && output != null) output.close();
            }

            IOUtils.copy(inputReader, outputWriter);
        } finally {
            // Closing the OutputStream from m2e as well causes a StreamClosed exception in m2e
            // So we cannot use a try-with-resource
            try {
                if (inputReader != null) inputReader.close();
            } finally {
                if (outputWriter != null) outputWriter.close();
            }
        }

        mojoMeta.getLog().info("Creating the copied file [" + targetFile.getName() + "].");
        mojoMeta.getLog().debug("Full path is [" + targetFile.getPath() + "].");

        return ProcessingResult.success(targetFile).build();
    }

    /**
     * Merges a list of source files. Create missing parent directories if needed.
     *
     * @param mergedFile output file resulting from the merged step
     * @throws IOException when the merge step fails
     */
    protected final ProcessingResult merge(File mergedFile) throws IOException {
        if (!haveFilesChanged(files, Collections.singleton(mergedFile))) {
            return ProcessingResult.skipped(mergedFile).build();
        }

        mkDir(targetDir);
        mkDir(mergedFile.getParentFile());

        mojoMeta.getLog().info("Creating the merged file [" + mergedFile.getName() + "].");
        mojoMeta.getLog().debug("Full path is [" + mergedFile.getPath() + "].");

        InputStream input;
        OutputStream output;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputWriter = null;
        try {
            input = new SequenceInputStream(new SourceFilesEnumeration(
                    mojoMeta.getLog(), files, mojoMeta.getEncoding(), processConfig.getLineSeparator()));
            output = mojoMeta.getBuildContext().newFileOutputStream(mergedFile);

            try {
                inputStreamReader = new InputStreamReader(input, mojoMeta.getEncoding());
            } finally {
                // When new InputStreamReader threw an exception, sequenceReader is null
                if (inputStreamReader == null && input != null) {
                    input.close();
                }
            }
            try {
                outputWriter = new OutputStreamWriter(output, mojoMeta.getEncoding());
            } finally {
                // When new OutputStreamWriter threw an exception, outWriter is null
                if (outputWriter == null && output != null) {
                    output.close();
                }
            }

            IOUtils.copyLarge(inputStreamReader, outputWriter, new char[processConfig.getBufferSize()]);

            // Make sure we end with a new line
            outputWriter.append(processConfig.getLineSeparator());
        } finally {
            // Closing the OutputStream from m2e as well causes a StreamClosed exception in m2e
            // So we cannot use a try-with-resource
            try {
                if (inputStreamReader != null) inputStreamReader.close();
            } finally {
                if (outputWriter != null) outputWriter.close();
            }
        }

        return ProcessingResult.success(mergedFile).build();
    }

    /**
     * Checks whether any change was made to the source / output file pairs. If not, then we can skip the execution of
     * the current bundle.
     *
     * @param sourceFiles Source files to check.
     * @param outputFiles Output files corresponding to the source files, in that order. Must have the same size as the
     *     source files list.
     * @return Whether any change was made to the source / output file pairs
     */
    protected final boolean haveFilesChanged(Collection<File> sourceFiles, Collection<File> outputFiles) {
        boolean changed;
        if (processConfig.isForce() && mojoMeta.getBuildContext().isIncremental()) {
            mojoMeta.getLog()
                    .warn(
                            "Force is enabled, but building incrementally. Using the force option in an m2e incremental build will result in an endless build loop.");
        }

        if (processConfig.isForce()) {
            mojoMeta.getLog().info("Force is enabled, skipping check for changed files.");
            changed = true;
        } else {
            if (mojoMeta.getBuildContext().isIncremental()) {
                changed = sourceFiles.stream().anyMatch(mojoMeta.getBuildContext()::hasDelta);
            } else {
                changed = checkFilesForChanges(sourceFiles, outputFiles);
            }
        }
        if (changed) {
            removeMessages(sourceFiles);
        }
        if (mojoMeta.getLog().isDebugEnabled()) {
            String prefix = changed
                    ? "Changes since last build, processing bundle with output files ["
                    : "No changes since last build, skipping bundle with output files [";
            mojoMeta.getLog()
                    .debug(prefix + outputFiles.stream().map(File::getPath).collect(Collectors.joining(", ")) + "].");
        }
        return changed;
    }

    private boolean checkFilesForChanges(Collection<File> sourceFiles, Collection<File> outputFiles) {
        final var outputFilesExist = outputFiles.stream().allMatch(File::exists);
        switch (ObjectUtils.defaultIfNull(processConfig.getSkipMode(), SkipMode.NEWER)) {
            case NEWER:
                if (outputFilesExist) {
                    final var oldestOutputFile = outputFiles.stream()
                            .map(File::lastModified)
                            .min(Long::compare)
                            .orElse(0L);
                    final var youngestSourceFile = sourceFiles.stream()
                            .map(File::lastModified)
                            .max(Long::compare)
                            .orElse(Long.MAX_VALUE);
                    mojoMeta.getLog().debug("Date of oldest output file is" + new Date(oldestOutputFile));
                    mojoMeta.getLog().debug("Date of youngest source file is " + new Date(youngestSourceFile));
                    return !(oldestOutputFile > youngestSourceFile);
                } else {
                    return true;
                }
            case EXISTS:
                return !outputFilesExist;
            default:
                throw new RuntimeException("Unhandled enum: " + processConfig.getSkipMode());
        }
    }

    private void logResults(List<ProcessingResult> results) {
        final long skippedCount =
                results.stream().filter(ProcessingResult::isWasSkipped).count();
        final long processedCount = results.size() - skippedCount;
        mojoMeta.getLog().info("Processed " + (processedCount + skippedCount) + " output files");
        if (processedCount > 0) {
            mojoMeta.getLog().info("Created " + processedCount + " output files");
        }
        if (skippedCount > 0) {
            mojoMeta.getLog().info("Skipped " + skippedCount + " output files (" + processConfig.getSkipMode() + ")");
        }
    }

    /**
     * Minifies a source file. Create missing parent directories if needed.
     *
     * @param mergedFile input file resulting from the merged step
     * @param minifiedFile output file resulting from the minify step
     * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
     *     change).
     * @throws IOException When an input file could not be read or an output file could not be written.
     * @throws MojoFailureException When the minify step fails
     */
    abstract ProcessingResult minify(File mergedFile, File minifiedFile) throws IOException, MojoFailureException;

    /**
     * Minifies a list of source files into a single file. Create missing parent directories if needed.
     *
     * @param srcFiles list of input files
     * @param minifiedFile output file resulting from the minify step
     * @return <code>true</code> if execution was performed, <code>false</code> if it was skipped (because files did not
     *     change).
     * @throws IOException When an input file could not be read or an output file could not be written.
     * @throws MojoFailureException When the minify step fails
     */
    abstract ProcessingResult minify(List<File> srcFiles, File minifiedFile) throws IOException, MojoFailureException;
}
