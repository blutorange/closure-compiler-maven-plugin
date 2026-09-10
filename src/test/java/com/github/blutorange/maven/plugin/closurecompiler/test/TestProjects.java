package com.github.blutorange.maven.plugin.closurecompiler.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit extension that hands each test its own copy of a test project. The copy is named after the test that requested
 * it, so the same project can be used by several tests, and tests are free to modify their copy.
 *
 * <p>Register it as a test class field annotated with {@code @RegisterExtension}.
 */
final class TestProjects implements BeforeEachCallback {

    private final Path projectsDirectory;
    private final Path workDirectory;

    /** Name of the currently running test. Null until the first test started. */
    private String testName;

    TestProjects(String projectsDirectory, String workDirectory) {
        this.projectsDirectory = Path.of(projectsDirectory);
        this.workDirectory = Path.of(workDirectory);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        testName = context.getRequiredTestClass().getSimpleName() + "_"
                + context.getRequiredTestMethod().getName();
    }

    /**
     * Creates a fresh copy of the given test project and returns its base directory. An existing copy from a previous
     * run is removed first.
     *
     * @param project Name of the test project, i.e. the name of its directory in the projects directory.
     * @return The base directory of the copy that was created.
     * @throws IOException When the test project could not be copied.
     */
    File getBasedir(String project) throws IOException {
        if (testName == null) {
            throw new IllegalStateException(TestProjects.class.getSimpleName()
                    + " must be a test class field annotated with @RegisterExtension");
        }

        final var source = projectsDirectory.resolve(project).toFile();
        assertTrue(source.isDirectory(), () -> "Test project directory must exist: " + source);

        final var basedir = workDirectory
                .resolve(testName + "_" + project)
                .toAbsolutePath()
                .normalize()
                .toFile();
        FileUtils.deleteDirectory(basedir);
        FileUtils.copyDirectory(source, basedir, false);
        assertTrue(basedir.isDirectory(), () -> "Test project working directory must have been created: " + basedir);

        return basedir;
    }
}
