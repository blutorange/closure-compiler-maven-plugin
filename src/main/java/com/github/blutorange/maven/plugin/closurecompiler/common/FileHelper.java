package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.plexus.util.DirectoryScanner;

/** Helper methods for working with files. */
public class FileHelper {
    private FileHelper() {}

    /** @return The path of the given {@code target}, relative to the specified {@code base} file. */
    public static String relativizePath(File base, File target) {
        final var targetPath = absoluteFileToCanonicalFile(target.getAbsoluteFile()).toPath();
        if (base == null) {
            return targetPath.toString();
        }

        final var basePath = absoluteFileToCanonicalFile(base.getAbsoluteFile()).toPath();
        return basePath.relativize(targetPath).toString().replace(File.separatorChar, '/');
    }

    /**
     * Makes target relative to base. Allows base and target to be relative paths.
     *
     * @return The path of the given {@code target}, relative to the specified {@code base} file.
     */
    public static String relativizeRelativePath(File base, File target) {
        final var targetPath = target.toPath();
        if (base == null) {
            return targetPath.toString();
        }

        final var basePath = base.toPath();
        return basePath.relativize(targetPath).toString().replace(File.separatorChar, '/');
    }

    /** Sames as {@link FileUtils#getFile(File, String...)}, but ignores empty names. */
    public static File getFile(final File directory, final String... names) {
        return FileUtils.getFile(
                directory, Arrays.stream(names).filter(StringUtils::isNotEmpty).toArray(String[]::new));
    }

    /**
     * If the last file does not refer to an absolute path, return the file relative to the given base directories. If
     * the last file already refers to an absolute path, return the last file.
     *
     * @param basedir Directory relative to which to resolve relative files.
     * @param files Files to make absolute.
     */
    public static File getAbsoluteFile(File basedir, File... files) {
        if (files == null) {
            return basedir;
        }
        var result = basedir;
        for (final var file : files) {
            if (file == null) {
                continue;
            }
            if (file.isAbsolute()) {
                result = file;
            } else {
                result = new File(result, file.getPath());
            }
        }
        return result;
    }

    /**
     * If the last file does not refer to an absolute path, return the file relative to the given base directories. If
     * the last file already refers to an absolute path, return the last file.
     *
     * @param basedir Directory relative to which to resolve relative files.
     * @param files Files to make absolute.
     */
    public static File getAbsoluteFile(File basedir, String... files) {
        if (files == null) {
            return basedir;
        }
        final var fileObjects = new File[files.length];
        for (var i = 0; i < files.length; i += 1) {
            fileObjects[i] = files[i] != null ? new File(files[i]) : null;
        }
        return getAbsoluteFile(basedir, fileObjects);
    }

    /**
     * Processes the includes and excludes relative to the given base directory, and returns all included files.
     *
     * @param baseDir Base directory of the includes and excludes.
     * @param includes List of specified includes
     * @param excludes List of specified excludes
     * @return A list of all files matching the given includes and excludes.
     */
    public static List<File> getIncludedFiles(File baseDir, List<String> includes, List<String> excludes) {
        if (CollectionUtils.isEmpty(includes)) {
            return new ArrayList<>();
        }
        if (!baseDir.exists()) {
            return new ArrayList<>();
        }
        final var excludesArray = excludes.toArray(new String[0]);

        return IntStream.range(0, includes.size())
                .mapToObj(i -> Pair.of(i, includes.get(i)))
                .flatMap(include -> {
                    final var scanner = new DirectoryScanner();
                    scanner.setIncludes(new String[] {include.getRight()});
                    scanner.setExcludes(excludesArray);
                    scanner.addDefaultExcludes();
                    scanner.setBasedir(baseDir);
                    scanner.scan();
                    return Arrays.stream(scanner.getIncludedFiles()).map(includedFilename -> {
                        final var includedFile = new File(baseDir, includedFilename);
                        return Pair.of(include.getLeft(), absoluteFileToCanonicalFile(includedFile));
                    });
                })
                .sorted()
                .map(Pair::getRight)
                .filter(distinctByKey(File::getAbsolutePath))
                .collect(Collectors.toList());
    }

    /**
     * When the file is an absolute path, return its canonical representation, if possible.
     *
     * @param file The file to process.
     * @return The canonical path of the file, if absolute, or the file itself otherwise.
     */
    public static File absoluteFileToCanonicalFile(File file) {
        try {
            return file != null && file.isAbsolute() ? file.getCanonicalFile() : file;
        } catch (final IOException e) {
            return file;
        }
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final var seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static boolean startsWithBom(File file, Charset encoding) throws IOException {
        try (final var input = new FileInputStream(file)) {
            try (final var reader = new InputStreamReader(input, encoding)) {
                final var firstChar = reader.read();
                return firstChar == '\ufeff';
            }
        }
    }
}
