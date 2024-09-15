package com.github.blutorange.maven.plugin.closurecompiler.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import io.takari.maven.testing.TestResources5;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MinifyMojoTest {

    protected interface Action {
        void run() throws Throwable;
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

    private Logger LOG = Logger.getLogger(MinifyMojoTest.class.getCanonicalName());

    @RegisterExtension
    final TestResources5 testResources = new TestResources5("src/test/resources/projects", "target/test-projects");

    private void assertDirContent(File basedir) {
        File expected = new File(basedir, "expected");
        File actual = new File(new File(basedir, "target"), "test");
        Map<String, File> expectedFiles = expected.exists() ? listFiles(expected) : new HashMap<>();
        Map<String, File> actualFiles = actual.exists() ? listFiles(actual) : new HashMap<>();
        LOG.info("Comparing actual files [\n"
                + actualFiles.values().stream().map(File::getAbsolutePath).collect(Collectors.joining(",\n")) + "\n]");
        LOG.info("to the expected files [\n"
                + expectedFiles.values().stream().map(File::getAbsolutePath).collect(Collectors.joining(",\n"))
                + "\n]");
        assertTrue(
                expectedFiles.size() > 0,
                "There must be at least one expected file. Add a file 'nofiles' if you expect there to be no files");
        if (expectedFiles.size() == 1
                && "nofiles".equals(expectedFiles.values().iterator().next().getName())) {
            // Expect there to be no output files
            assertEquals(0, actualFiles.size());
        } else {
            assertEquals(
                    expectedFiles.size(),
                    actualFiles.size(),
                    "Number of expected files must match the number of produced files");
            assertTrue(
                    CollectionUtils.isEqualCollection(expectedFiles.keySet(), actualFiles.keySet()),
                    "Expected file names must match the produced file names");
            expectedFiles.forEach((key, expectedFile) -> {
                File actualFile = actualFiles.get(key);
                try {
                    compareFiles(expectedFile, actualFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void clean(File basedir) throws IOException {
        File target = new File(basedir, "target");
        if (target.exists()) {
            FileUtils.forceDelete(target);
        }
        assertFalse(target.exists());
    }

    private void compareFiles(File expectedFile, File actualFile) throws IOException {
        List<String> expectedLines = FileUtils.readLines(expectedFile, UTF_8);
        List<String> actualLines = FileUtils.readLines(actualFile, UTF_8);
        assertTrue(
                expectedFile.exists(),
                "File with expected content does not exist: '" + actualFile.getAbsolutePath() + "'");
        assertTrue(
                actualFile.exists(),
                "File with produced content does not exist: '" + actualFile.getAbsolutePath() + "'");
        // Ignore empty lines
        expectedLines.removeIf(StringUtils::isBlank);
        actualLines.removeIf(StringUtils::isBlank);
        // Check file contents
        assertTrue(
                expectedLines.size() > 0,
                "Expected file must contain at least one non-empty line: '" + actualFile.getAbsolutePath() + "'");
        assertEquals(
                expectedLines.size(),
                actualLines.size(),
                "Number of non-empty lines in expected file must match the generated number of lines: '"
                        + actualFile.getAbsolutePath() + "'");
        for (int i = 0, j = expectedLines.size(); i < j; ++i) {
            assertEquals(
                    expectedLines.get(i).trim(),
                    actualLines.get(i).trim(),
                    "Actual content of file '" + actualFile.getAbsolutePath() + "' differs from the expected content");
        }
    }

    private <T extends Throwable> void expectError(Class<T> error, Action runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (error.isInstance(e)) {
                return;
            }
            fail("Action threw an error of type " + e.getClass().getSimpleName()
                    + ", but it is not of the expected type " + error.getSimpleName());
        }
        fail("Action did not throw the expected error type " + error.getSimpleName());
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
            try (final var outStream = new ChainedPrintStream(new PrintStream(out), System.out);
                    final var errStream = new ChainedPrintStream(new PrintStream(err), System.err)) {
                final var cli = new MavenCli();
                cli.doMain(args.toArray(new String[0]), pom.getParent(), outStream, errStream);
                return new MavenResult(out.toString(UTF_8), err.toString(UTF_8));
            }
        }
    }

    private Map<String, File> listFiles(File basedir) {
        return FileUtils.listFiles(basedir, null, true).stream()
                .collect(Collectors.toMap(file -> FileHelper.relativizePath(basedir, file), identity()));
    }

    private MavenResult runMinify(String projectName) throws Exception {
        return runMinify(projectName, new HashSet<>());
    }

    private MavenResult runMinify(String projectName, Collection<String> profiles) throws Exception {
        final var parentDir = testResources.getBasedir("parent").getCanonicalFile();
        final var parentPom = new File(parentDir, "pom.xml");
        final var parentPomNew = new File(parentDir.getParentFile(), "pom.xml");
        assertTrue(parentPom.exists());
        FileUtils.copyFile(parentPom, parentPomNew);

        final var basedir = testResources.getBasedir(projectName).getCanonicalFile();
        final var pom = new File(basedir, "pom.xml");
        assertTrue(pom.exists());

        clean(basedir);
        invokeMaven(parentPomNew, "install", Collections.emptySet());
        return invokeMaven(pom, "package", profiles);
    }

    private void runMinifyAndAssertDirContent(String projectName) throws Exception {
        runMinifyAndAssertDirContent(projectName, new HashSet<>());
    }

    private void runMinifyAndAssertDirContent(String projectName, Collection<String> profiles) throws Exception {
        File basedir = testResources.getBasedir(projectName).getCanonicalFile();
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
        expectError(
                AssertionError.class, () -> runMinifyAndAssertDirContent("externs", Arrays.asList("without-externs")));

        // Externs declared, variable can be found, so minification should succeed
        runMinifyAndAssertDirContent("externs", Arrays.asList("createOlderFile", "with-externs"));
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
        MavenResult result = runMinify("overwriteInputFilesDisabled");
        assertTrue(result.getErrString()
                .contains("The source file [fileC.js] has the same name as the output file [fileC.js]"));
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
        // Output file does not exists, minification should run
        expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("skipIfExists")));

        // This create the (older) output file, so the minification process should not run
        runMinifyAndAssertDirContent("skipif", Arrays.asList("createOlderFile", "skipIfExists"));

        // Now force is enabled, minification should run
        expectError(
                AssertionError.class,
                () -> runMinifyAndAssertDirContent(
                        "skipif", Arrays.asList("createOlderFile", "skipIfExists", "force")));
    }

    @Test
    public void testSkipIfNewer() throws Exception {
        // Output file does not exists, minification should run
        expectError(AssertionError.class, () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("skipIfNewer")));

        // This create the newer output file, so the minification process should not run
        runMinifyAndAssertDirContent("skipif", Arrays.asList("createNewerFile", "skipIfNewer"));

        // Now force is enabled, minification should run
        expectError(
                AssertionError.class,
                () -> runMinifyAndAssertDirContent("skipif", Arrays.asList("createNewerFile", "skipIfNewer", "force")));
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
}
