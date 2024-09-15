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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.apache.maven.plugin.logging.Log;

/**
 * Used to initialize a {@code SequenceInputStream} with a {@code Enumeration<? extends InputStream>}. The input streams
 * that are produced by the enumeration will be read, in order, to provide the bytes to be read from the
 * {@code SequenceInputStream}.
 */
final class SourceFilesEnumeration implements Enumeration<InputStream> {

    private final List<InputStreamSupplier> suppliers;

    private int current = 0;

    /**
     * Enumeration public constructor.
     *
     * @param log Maven plugin log
     * @param files list of files
     * @param charset Encoding to be used.
     * @param lineSeparator String used to separate lines.
     */
    public SourceFilesEnumeration(Log log, List<File> files, Charset charset, String lineSeparator) {
        this.suppliers = new ArrayList<>();
        for (int i = 0, j = files.size(); i < j; ++i) {
            final var file = files.get(i);
            log.info("Processing source file [" + file.getName() + "].");
            log.debug("Full path is [" + file.getPath() + "].");
            this.suppliers.add(new FileInputStreamSupplier(file));
            if (i < j - 1) {
                this.suppliers.add(new NewlineInputStreamSupplier(charset, lineSeparator));
            }
        }
    }

    /**
     * Tests if this enumeration contains more elements.
     *
     * @return {@code true} if and only if this enumeration object contains at least one more element to provide;
     *     {@code false} otherwise.
     */
    @Override
    public boolean hasMoreElements() {
        return (current < suppliers.size());
    }

    /**
     * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
     *
     * @return the next element of this enumeration.
     * @throws NoSuchElementException if no more elements exist.
     */
    @Override
    public InputStream nextElement() {
        if (!hasMoreElements()) {
            throw new NoSuchElementException("No more files!");
        }
        final var nextElement = suppliers.get(current);
        current += 1;
        return nextElement.get();
    }

    private interface InputStreamSupplier extends Supplier<InputStream> {}

    /**
     * Supplies an input stream with the content of a newline separator.
     *
     * @author madgaksha
     */
    private static class NewlineInputStreamSupplier implements InputStreamSupplier {
        private final Charset charset;
        private final String lineSeparator;

        public NewlineInputStreamSupplier(Charset charset, String lineSeparator) {
            this.charset = charset;
            this.lineSeparator = lineSeparator;
        }

        @Override
        public InputStream get() {
            return new ByteArrayInputStream(lineSeparator.getBytes(charset));
        }
    }

    /**
     * Supplies an input stream with the content of the given file.
     *
     * @author madgaksha
     */
    private static class FileInputStreamSupplier implements InputStreamSupplier {
        private final File file;

        public FileInputStreamSupplier(File file) {
            this.file = file;
        }

        @Override
        public InputStream get() {
            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException e) {
                throw new NoSuchElementException("The path [" + file.getPath() + "] cannot be found.");
            }
        }
    }
}
