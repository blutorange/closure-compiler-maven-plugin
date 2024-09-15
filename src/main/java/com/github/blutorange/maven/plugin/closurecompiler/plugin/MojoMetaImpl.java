package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.shared.MojoMetadata;
import java.nio.charset.Charset;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

final class MojoMetaImpl implements MojoMetadata {

    private final MavenProject project;
    private final Log log;
    private final Charset encoding;
    private final BuildContext buildContext;

    public MojoMetaImpl(MavenProject project, Log log, String encoding, BuildContext buildContext) {
        this.project = project;
        this.log = log;
        this.encoding = Charset.forName(encoding);
        this.buildContext = buildContext;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public Log getLog() {
        return log;
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public BuildContext getBuildContext() {
        return buildContext;
    }
}
