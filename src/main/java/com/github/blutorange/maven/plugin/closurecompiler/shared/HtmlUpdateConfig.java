package com.github.blutorange.maven.plugin.closurecompiler.shared;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.HtmlUpdate;
import java.io.File;
import java.util.List;

public interface HtmlUpdateConfig {
    Boolean isHtmlUsePhysicalRoot();

    String getHtmlSourcePath();

    List<HtmlUpdate> getHtmlUpdates();

    File getHtmlDir();

    File getHtmlRoot();

    File getHtmlScriptRoot();
}
