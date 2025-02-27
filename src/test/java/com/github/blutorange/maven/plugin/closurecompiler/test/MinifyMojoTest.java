package com.github.blutorange.maven.plugin.closurecompiler.test;

import static com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper.relativizePath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.takari.maven.testing.TestResources5;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinifyMojoTest {

    private static class EncodingProvider {
        private final File basedir;
        private final Map<String, Charset> encodingMap = new HashMap<>();

        EncodingProvider(File basedir) throws IOException {
            this.basedir = basedir;
            var encodingData = new File(basedir, "encoding.txt");
            if (!encodingData.exists()) {
                return;
            }

            var encodingLines = FileUtils.readLines(encodingData, UTF_8);
            for (var encodingLine : encodingLines) {
                var parts = encodingLine.split("=");
                if (parts.length == 2) {
                    var relativePath = parts[0];
                    var encoding = Charset.forName(parts[1]);
                    encodingMap.put(relativePath, encoding);
                }
            }
        }

        public Charset determineEncoding(File file) {
            var relativePath = relativizePath(basedir, file);
            return encodingMap.getOrDefault(relativePath, UTF_8);
        }
    }

    private static class MavenResult {
        final String errString;
        final String outString;

        public MavenResult(String outString, String errString) {
            this.outString = outString;
            this.errString = errString;
        }

        public String getErrString() {
            return errString;
        }

        public String getOutString() {
            return outString;
        }
    }

    private final Logger LOG = LoggerFactory.getLogger(MinifyMojoTest.class.getCanonicalName());

    @RegisterExtension
    final TestResources5 testResources = new TestResources5("src/test/resources/projects", "target/test-projects");

    private void assertDirContent(File basedir) throws IOException {
        var actual = new File(new File(basedir, "target"), "test");
        var expected = new File(basedir, "expected");
        assertThat(expected.exists())
                .as("Expected directory %s must exist", expected)
                .isTrue();

        var expectedFiles = listFiles(expected);
        assertThat(expectedFiles)
                .as("There must be at least one expected file. Add a file 'nofiles' if you expect there to be no files")
                .isNotEmpty();

        if (expectedFiles.size() == 1
                && "nofiles".equals(expectedFiles.values().iterator().next().getName())) {
            if (actual.exists()) {
                assertThat(listFiles(actual))
                        .as("Expecting no output files in %s", actual)
                        .isEmpty();
            }
            return;
        }

        assertThat(actual.exists()).as("Actual directory %s must exist", actual).isTrue();

        var actualFiles = listFiles(actual);
        assertThat(actualFiles).containsOnlyKeys(expectedFiles.keySet());

        var encodingProvider = new EncodingProvider(basedir);
        expectedFiles.forEach((key, expectedFile) -> {
            var actualFile = actualFiles.get(key);
            assertThatNoException()
                    .isThrownBy(() ->
                            compareFiles(expectedFile, actualFile, encodingProvider.determineEncoding(expectedFile)));
        });
    }

    private void clean(File basedir) throws IOException {
        var target = new File(basedir, "target");
        if (target.exists()) {
            FileUtils.forceDelete(target);
        }
        assertThat(target.exists())
                .as("Target folder must not exist anymore: %s", target)
                .isFalse();
    }

    private void compareFiles(File expectedFile, File actualFile, Charset charset) throws IOException {
        final List<String> expectedLines;
        final List<String> actualLines;

        assertThat(expectedFile).exists();
        assertThat(actualFile).exists();

        if (expectedFile.getAbsolutePath().endsWith(".gz")) {
            expectedLines = readLinesFromGzipFile(expectedFile, charset);
            actualLines = readLinesFromGzipFile(actualFile, charset);
        } else {
            expectedLines = FileUtils.readLines(expectedFile, charset);
            actualLines = FileUtils.readLines(actualFile, charset);
        }

        // Ignore empty lines
        expectedLines.removeIf(StringUtils::isBlank);
        actualLines.removeIf(StringUtils::isBlank);

        // Check file contents
        assertThat(expectedLines).as("Content of %s", expectedFile).isNotEmpty();
        assertThat(actualLines)
                .as("Content of %s has same number of non-empty lines as %s", actualFile, expectedFile)
                .hasSameSizeAs(expectedLines);

        var lines = expectedLines.size();
        for (int i = 0; i < lines; ++i) {
            assertThat(actualLines.get(i))
                    .as("Line from %s should be the same as line from %s", actualFile, expectedFile)
                    .isEqualToIgnoringNewLines(expectedLines.get(i));
        }
    }

    private List<String> readLinesFromGzipFile(File file, Charset charset) throws IOException {
        try (var fileInputStream = new FileInputStream(file);
                var gzipInputStream = new GZIPInputStream(fileInputStream)) {

            return IOUtils.readLines(gzipInputStream, charset);
        }
    }

    private MavenResult invokeMaven(File pom, String goal, Collection<String> profiles) throws IOException {
        final var args = new ArrayList<String>();
        args.add("clean");
        args.add(goal);
        args.add("-DskipTests");
        profiles.stream().flatMap(profile -> Stream.of("-P", profile)).forEach(args::add);
        System.setProperty("maven.multiModuleProjectDirectory", pom.getParent());
        LOG.info("Invoking maven: " + StringUtils.join(args, " "));
        try (final var out = new ByteArrayOutputStream();
                final var err = new ByteArrayOutputStream()) {

            // We don't want to close ChainedPrintStream as it would close System.out, so we don't use
            // try-with-resources
            final var outStream = new ChainedPrintStream(new PrintStream(out), System.out);
            final var errStream = new ChainedPrintStream(new PrintStream(err), System.err);
            new MavenCli().doMain(args.toArray(new String[0]), pom.getParent(), outStream, errStream);
            return new MavenResult(out.toString(UTF_8), err.toString(UTF_8));
        }
    }

    private Map<String, File> listFiles(File basedir) {
        return FileUtils.listFiles(basedir, null, true).stream()
                .collect(Collectors.toMap(file -> relativizePath(basedir, file), identity()));
    }

    private MavenResult runMinify(String projectName, Collection<String> profiles) throws Exception {
        final var parentDir = testResources.getBasedir("parent").getCanonicalFile();
        final var parentPom = new File(parentDir, "pom.xml");
        final var parentPomNew = new File(parentDir.getParentFile(), "pom.xml");
        assertThat(parentPom).exists();
        FileUtils.copyFile(parentPom, parentPomNew);

        final var basedir = testResources.getBasedir(projectName).getCanonicalFile();
        final var pom = new File(basedir, "pom.xml");
        assertThat(pom).exists();

        clean(basedir);
        invokeMaven(parentPomNew, "install", List.of());
        return invokeMaven(pom, "package", profiles);
    }

    private void runMinifyAndAssertDirContent(String projectName) throws Exception {
        runMinifyAndAssertDirContent(projectName, new HashSet<>());
    }

    private List<String> profiles(String... profiles) {
        return List.of(profiles);
    }

    private void runMinifyAndAssertDirContent(String projectName, Collection<String> profiles) throws Exception {
        var basedir = testResources.getBasedir(projectName).getCanonicalFile();
        runMinify(projectName, profiles);
        assertDirContent(basedir);
    }

    @Test
    public void testAllowDynamicImport() throws Exception {
        runMinifyAndAssertDirContent("allowdynamicimport");
    }

    @Test
    public void testAssumeFunctionWrapper() throws Exception {
        runMinifyAndAssertDirContent("assumeFunctionWrapper");
    }

    @Test
    public void testBundle() throws Exception {
        runMinifyAndAssertDirContent("bundle");
    }

    @Test
    public void testCompilationLevel() throws Exception {
        runMinifyAndAssertDirContent("compilationlevel");
    }

    @Test
    public void testDefine() throws Exception {
        runMinifyAndAssertDirContent("define");
    }

    @Test
    public void testDynamicImportAlias() throws Exception {
        runMinifyAndAssertDirContent("dynamicimportalias");
    }

    @Test
    public void testEmitUseStrict() throws Exception {
        runMinifyAndAssertDirContent("emitusestrict");
    }

    @Test
    public void testExterns() throws Exception {
        // No externs declared, variable cannot be found, so minification should fail
        assertThatThrownBy(() -> runMinifyAndAssertDirContent("externs", profiles("without-externs")))
                .isInstanceOf(AssertionError.class);

        // Externs declared, variable can be found, so minification should succeed
        runMinifyAndAssertDirContent("externs", profiles("createOlderFile", "with-externs"));
    }

    @Test
    public void testHtmlUpdate() throws Exception {
        runMinifyAndAssertDirContent("htmlUpdate");
    }

    @Test
    public void testJQuery() throws Exception {
        runMinifyAndAssertDirContent("jquery");
    }

    @Test
    public void testMinimal() throws Exception {
        runMinifyAndAssertDirContent("minimal");
    }

    @Test
    public void testNodeModules() throws Exception {
        runMinifyAndAssertDirContent("nodemodules");
    }

    @Test
    public void testOutputFilename() throws Exception {
        runMinifyAndAssertDirContent("outputfilename");
    }

    @Test
    public void testOutputWrapper() throws Exception {
        runMinifyAndAssertDirContent("outputwrapper");
    }

    @Test
    public void testOverwriteInputFilesDisabled() throws Exception {
        assertThat(runMinify("overwriteInputFilesDisabled", profiles()).getErrString())
                .contains("The source file [fileC.js] has the same name as the output file [fileC.js]");
    }

    @Test
    public void testOverwriteInputFilesEnabled() throws Exception {
        runMinifyAndAssertDirContent("overwriteInputFilesEnabled");
    }

    @Test
    public void testPreferSingleQuotes() throws Exception {
        runMinifyAndAssertDirContent("prefersinglequotes");
    }

    @Test
    public void testPrettyPrint() throws Exception {
        runMinifyAndAssertDirContent("prettyprint");
    }

    @Test
    public void testRewritePolyfills() throws Exception {
        runMinifyAndAssertDirContent("rewritepolyfills");
    }

    @Test
    public void testIsolatePolyfills() throws Exception {
        runMinifyAndAssertDirContent("isolatepolyfills");
    }

    @Test
    public void testSkip() throws Exception {
        runMinifyAndAssertDirContent("skip");
    }

    @Test
    public void testSkipAll() throws Exception {
        runMinifyAndAssertDirContent("skipall");
    }

    @Test
    public void testSkipIfExists() throws Exception {
        runMinify("skipif", List.of("skipIfExists"));

        // This creates the (older) output file, so the minification process should not run
        runMinifyAndAssertDirContent("skipif", profiles("createOlderFile", "skipIfExists"));

        // Now force is enabled, minification should run
        assertThatThrownBy(() ->
                        runMinifyAndAssertDirContent("skipif", profiles("createOlderFile", "skipIfExists", "force")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testSkipIfNewer() throws Exception {
        // Output file does not exist, minification should run
        assertThatThrownBy(() -> runMinifyAndAssertDirContent("skipif", profiles("skipIfNewer")))
                .isInstanceOf(AssertionError.class);

        // This creates the newer output file, so the minification process should not run
        runMinifyAndAssertDirContent("skipif", profiles("createNewerFile", "skipIfNewer"));

        // Now force is enabled, minification should run
        assertThatThrownBy(() ->
                        runMinifyAndAssertDirContent("skipif", profiles("createNewerFile", "skipIfNewer", "force")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testSkipSome() throws Exception {
        runMinifyAndAssertDirContent("skipsome");
    }

    @Test
    public void testSourceMap() throws Exception {
        runMinifyAndAssertDirContent("sourcemap");
    }

    @Test
    public void testSubdirs() throws Exception {
        runMinifyAndAssertDirContent("subdirs");
    }

    @Test
    public void testTrustedStrings() throws Exception {
        runMinifyAndAssertDirContent("trustedstrings");
    }

    @Test
    public void testUseTypesForOptimization() throws Exception {
        runMinifyAndAssertDirContent("usetypesforoptimization");
    }

    @Test
    public void testGzipCompression() throws Exception {
        runMinifyAndAssertDirContent("gzip-compression");
    }
}
