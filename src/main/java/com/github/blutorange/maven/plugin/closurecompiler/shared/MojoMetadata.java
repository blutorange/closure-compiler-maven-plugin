package com.github.blutorange.maven.plugin.closurecompiler.shared;

import java.nio.charset.Charset;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

public interface MojoMetadata {
    MavenProject getProject();

    Log getLog();

    Charset getEncoding();

    BuildContext getBuildContext();
}
