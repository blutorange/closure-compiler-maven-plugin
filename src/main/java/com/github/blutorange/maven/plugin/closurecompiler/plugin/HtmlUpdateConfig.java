package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import java.io.File;
import java.util.List;

public interface HtmlUpdateConfig {
    List<HtmlUpdate> getHtmlUpdates();

    File getHtmlDir();

    File getHtmlRoot();

    File getHtmlScriptRoot();
}
