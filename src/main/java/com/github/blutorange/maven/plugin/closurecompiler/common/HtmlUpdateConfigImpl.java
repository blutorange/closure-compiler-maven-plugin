package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.HtmlUpdate;
import com.github.blutorange.maven.plugin.closurecompiler.plugin.HtmlUpdateConfig;
import java.io.File;
import java.util.List;

public final class HtmlUpdateConfigImpl implements HtmlUpdateConfig {
    private final List<HtmlUpdate> htmlUpdates;
    private final File htmlDir;
    private final File htmlRoot;
    private final File htmlScriptRoot;
    private final String htmlSourcePath;
    private final Boolean htmlUsePhysicalRoot;

    public HtmlUpdateConfigImpl(
            List<HtmlUpdate> htmlUpdates,
            File htmlDir,
            File htmlRoot,
            File htmlScriptRoot,
            String htmlSourcePath,
            Boolean htmlUsePhysicalRoot) {
        this.htmlUpdates = htmlUpdates != null ? htmlUpdates : List.of();
        this.htmlDir = htmlDir;
        this.htmlRoot = htmlRoot;
        this.htmlScriptRoot = htmlScriptRoot;
        this.htmlSourcePath = htmlSourcePath;
        this.htmlUsePhysicalRoot = htmlUsePhysicalRoot;
    }

    @Override
    public Boolean isHtmlUsePhysicalRoot() {
        return htmlUsePhysicalRoot;
    }

    @Override
    public String getHtmlSourcePath() {
        return htmlSourcePath;
    }

    @Override
    public List<HtmlUpdate> getHtmlUpdates() {
        return htmlUpdates;
    }

    @Override
    public File getHtmlDir() {
        return htmlDir;
    }

    @Override
    public File getHtmlRoot() {
        return htmlRoot;
    }

    @Override
    public File getHtmlScriptRoot() {
        return htmlScriptRoot;
    }
}
