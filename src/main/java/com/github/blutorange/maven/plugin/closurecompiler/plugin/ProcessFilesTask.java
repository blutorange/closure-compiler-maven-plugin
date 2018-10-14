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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;

import com.github.blutorange.maven.plugin.closurecompiler.common.ClosureConfig;
import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import com.github.blutorange.maven.plugin.closurecompiler.common.FilenameInterpolator;
import com.github.blutorange.maven.plugin.closurecompiler.common.SourceFilesEnumeration;
import com.github.blutorange.maven.plugin.closurecompiler.common.TwoTuple;
import com.google.common.base.Predicate;

/**
 * Abstract class for merging and compressing a files list.
 */
public abstract class ProcessFilesTask implements Callable<Object> {

  private static final String DEFAULT_MERGED_FILENAME = "script.js";

  public static final String TEMP_SUFFIX = ".tmp";

  /**
   * Logs an addition of a new source file.
   * @param finalFilename the final file name
   * @param sourceFile the source file
   * @throws IOException
   */
  private static void addNewSourceFile(Collection<File> files, File sourceFile, MojoMetadata mojoMeta) throws IOException {
    if (sourceFile.exists()) {
      mojoMeta.getLog().debug("Adding source file [" + (mojoMeta.isVerbose() ? sourceFile.getPath() : sourceFile.getName()) + "].");
      files.add(sourceFile.getCanonicalFile());
    }
    else {
      throw new FileNotFoundException("The source file ["
          + (mojoMeta.isVerbose() ? sourceFile.getPath() : sourceFile.getName()) + "] does not exist.");
    }
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = new HashSet<>();
    return t -> seen.add(keyExtractor.apply(t));
  }

  /**
   * Returns the files to copy. Default exclusions are used when the excludes list is empty.
   * @param includes list of source files to include
   * @param excludes list of source files to exclude
   * @return the files to copy
   */
  private static List<File> getFilesToInclude(File sourceDir, List<String> includes, List<String> excludes) {
    if (includes == null || includes.isEmpty()) { return new ArrayList<>(); }

    String[] excludesArray = excludes.toArray(new String[excludes.size()]);

    // For each specified include, get all matching files, then
    // sort first by the specified order, then by file path. Finally,
    // filter out duplicate files.
    return IntStream.range(0, includes.size()) //
        .mapToObj(i -> new TwoTuple<>(i, includes.get(i))) //
        .flatMap(include -> {
          DirectoryScanner scanner = new DirectoryScanner();
          scanner.setIncludes(new String[] { include.getSecond() });
          scanner.setExcludes(excludesArray);
          scanner.addDefaultExcludes();
          scanner.setBasedir(sourceDir);
          scanner.scan();
          return Arrays.stream(scanner.getIncludedFiles()).map(includedFilename -> {
            File includedFile = new File(sourceDir, includedFilename);
            return new TwoTuple<>(include.getFirst(), includedFile);
          });
        }) //
        .sorted(TwoTuple.getComparator()) //
        .map(TwoTuple::getSecond) //
        .filter(distinctByKey(File::getAbsolutePath)) //
        .collect(Collectors.toList());
  }

  protected final Integer bufferSize;

  protected final ClosureConfig closureConfig;

  protected final Charset encoding;

  protected final List<File> files = new ArrayList<>();

  protected final boolean includesEmpty;

  protected final Log log;

  private final FilenameInterpolator outputFilenameInterpolator;

  protected final boolean skipMerge;

  protected final boolean skipMinify;

  protected final File sourceDir;

  protected final File targetDir;

  protected final boolean verbose;

  /**
   * Task constructor.
   * @param mojoMeta Base mojo data.
   * @param bufferSize size of the buffer used to read source files
   * @param suffix final file name suffix
   * @param nosuffix whether to use a suffix for the minified file name or not
   * @param skipMerge whether to skip the merge step or not
   * @param skipMinify whether to skip the minify step or not
   * @param baseSourceDir web resources source directory
   * @param baseTargetDir web resources target directory
   * @param sourceDir directory containing source files
   * @param includes list of source files to include
   * @param excludes list of source files to exclude
   * @param targetDir directory to write the final file
   * @param outputFilename the output file name
   * @param closureConfig Google closure configuration
   * @throws IOException
   */
  public ProcessFilesTask(MojoMetadata mojoMeta, int bufferSize,
      boolean skipMerge, boolean skipMinify,
      File baseSourceDir, File baseTargetDir,
      String sourceDir, String targetDir,
      List<String> includes, List<String> excludes,
      String outputFilename, ClosureConfig closureConfig) throws IOException {
    this.log = mojoMeta.getLog();
    this.verbose = mojoMeta.isVerbose();
    this.bufferSize = bufferSize;
    this.encoding = Charset.forName(mojoMeta.getEncoding());
    this.skipMerge = skipMerge;
    this.skipMinify = skipMinify;

    File projectBasedir = mojoMeta.getProject().getBasedir();
    this.sourceDir = FileUtils.getFile(FileHelper.getAbsoluteFile(projectBasedir, baseSourceDir), sourceDir).getAbsoluteFile().getCanonicalFile();
    this.targetDir = FileUtils.getFile(FileHelper.getAbsoluteFile(projectBasedir, baseTargetDir), targetDir).getAbsoluteFile().getCanonicalFile();
    this.outputFilenameInterpolator = new FilenameInterpolator(outputFilename);

    for (File include : getFilesToInclude(this.sourceDir, includes, excludes)) {
      if (!files.contains(include)) {
        addNewSourceFile(files, include, mojoMeta);
      }
    }

    this.includesEmpty = includes.isEmpty();
    this.closureConfig = closureConfig;
  }

  private void assertTarget(File source, File target) throws MojoFailureException {
    if (target.getAbsolutePath().equals(source.getAbsolutePath())) {
      String msg = "The source file [" + (verbose ? source.getPath() : source.getName())
          + "] has the same name as the output file [" + (verbose ? target.getPath() : target.getName()) + "].";
      log.warn(msg);
      throw new MojoFailureException(msg);
    }
  }

  /**
   * Method executed by the thread.
   * @throws IOException when the merge or minify steps fail
   * @throws MojoFailureException
   */
  @Override
  public Object call() throws IOException, MojoFailureException {
    synchronized (log) {
      log.info("Starting JavaScript task:");

      if (!targetDir.exists() && !targetDir.mkdirs()) { throw new RuntimeException("Unable to create target directory for: " + targetDir); }

      if (!files.isEmpty()) {

        // Minify only
        if (skipMerge) {
          log.info("Skipping the merge step...");

          for (File sourceFile : files) {
            // Create folders to preserve sub-directory structure when only minifying
            File targetPath = sourceFile.getParentFile();
            if (!targetPath.exists() && !targetPath.mkdirs()) { throw new RuntimeException("Unable to create target directory for: " + targetPath); }
            File minifiedFile = outputFilenameInterpolator.apply(sourceFile);
            assertTarget(sourceFile, minifiedFile);
            if (skipMinify) {
              FileUtils.copyFile(sourceFile, minifiedFile);
            }
            else {
              minify(sourceFile, minifiedFile);
            }
          }
        }
        // Merge-only
        else if (skipMinify) {
          File mergedFile = outputFilenameInterpolator.apply(new File(targetDir, DEFAULT_MERGED_FILENAME));
          merge(mergedFile);
          log.info("Skipping the minify step...");
        }
        // Minify + merge
        else {
          File minifiedFile = outputFilenameInterpolator.apply(new File(targetDir, DEFAULT_MERGED_FILENAME));
          minify(files, minifiedFile);
        }
      }
      else if (!includesEmpty) {
        // 'files' list will be empty if source file paths or names added to the project's POM are invalid.
        log.error("No valid JavaScript source files found to process.");
      }
    }

    return null;
  }

  /**
   * Logs compression gains.
   * @param mergedFile input file resulting from the merged step
   * @param minifiedFile output file resulting from the minify step
   */
  void logCompressionGains(File mergedFile, File minifiedFile) {
    List<File> srcFiles = new ArrayList<File>();
    srcFiles.add(mergedFile);
    logCompressionGains(srcFiles, minifiedFile);
  }

  /**
   * Logs compression gains.
   * @param srcFiles list of input files to compress
   * @param minifiedFile output file resulting from the minify step
   */
  void logCompressionGains(List<File> srcFiles, File minifiedFile) {
    try {
      File temp = File.createTempFile(minifiedFile.getName(), ".gz");

      try (InputStream in = new FileInputStream(minifiedFile);
          OutputStream out = new FileOutputStream(temp);
          GZIPOutputStream outGZIP = new GZIPOutputStream(out)) {
        IOUtils.copy(in, outGZIP, bufferSize);
      }

      long uncompressedSize = 0;
      if (srcFiles != null) {
        for (File srcFile : srcFiles) {
          uncompressedSize += srcFile.length();
        }
      }

      log.info("Uncompressed size: " + uncompressedSize + " bytes.");
      log.info("Compressed size: " + minifiedFile.length() + " bytes minified (" + temp.length()
          + " bytes gzipped).");

      temp.deleteOnExit();
    }
    catch (IOException e) {
      log.debug("Failed to calculate the gzipped file size.", e);
    }
  }

  /**
   * Merges a list of source files. Create missing parent directories if needed.
   * @param mergedFile output file resulting from the merged step
   * @throws IOException when the merge step fails
   */
  protected void merge(File mergedFile) throws IOException {
    if (!mergedFile.getParentFile().exists() && !mergedFile.getParentFile().mkdirs()) { throw new RuntimeException("Unable to create target directory for: " + mergedFile.getParentFile()); }

    try (InputStream sequence = new SequenceInputStream(new SourceFilesEnumeration(log, files, verbose, encoding));
        OutputStream out = new FileOutputStream(mergedFile);
        InputStreamReader sequenceReader = new InputStreamReader(sequence, encoding);
        OutputStreamWriter outWriter = new OutputStreamWriter(out, encoding)) {
      log.info("Creating the merged file [" + (verbose ? mergedFile.getPath() : mergedFile.getName()) + "].");

      IOUtils.copyLarge(sequenceReader, outWriter, new char[bufferSize]);
    }
    catch (IOException e) {
      log.error("Failed to concatenate files.", e);
      throw e;
    }
  }

  /**
   * Minifies a source file. Create missing parent directories if needed.
   * @param mergedFile input file resulting from the merged step
   * @param minifiedFile output file resulting from the minify step
   * @throws IOException when the minify step fails
   */
  abstract void minify(File mergedFile, File minifiedFile) throws IOException;

  /**
   * Minifies a list of source files into a single file. Create missing parent directories if needed.
   * @param srcFiles list of input files
   * @param minifiedFile output file resulting from the minify step
   * @throws IOException when the minify step fails
   */
  abstract void minify(List<File> srcFiles, File minifiedFile) throws IOException;
}