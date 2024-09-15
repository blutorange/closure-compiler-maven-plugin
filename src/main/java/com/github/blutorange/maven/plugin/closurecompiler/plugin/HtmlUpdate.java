package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

public class HtmlUpdate {
    /** A list of attributes to set to the new script path. Defaults to <code>"src"</code> when empty. */
    @SuppressWarnings("unused")
    @Parameter(name = "attributes")
    private List<String> attributes;

    /**
     * File set with the HTML files to update. Relative file paths are evaluated relative to the <code>htmlDir</code>.
     * Both HTML and XHTML files are supported.
     *
     * <p>Defaults to <code>**&#47;*.html</code>, i.e. include all HTML files in the <code>htmlDir</code>.
     */
    @SuppressWarnings("unused")
    @Parameter(name = "htmlFiles")
    private FileSet htmlFiles;

    /**
     * Base directory relative to which the <code>htmlFiles</code> to process are evaluated. Relative paths are
     * evaluated relative to the <code>htmlDir</code> option of the plugin configuration.
     *
     * <p>When not given, defaults to the <code>htmlDir</code> option of the plugin configuration.
     */
    @SuppressWarnings("unused")
    @Parameter(name = "htmlDir")
    private String htmlDir;

    /**
     * When given, the <code>htmlRoot</code> and <code>htmlScriptRoot</code> options are ignored.
     *
     * <p>The path to use as the <code>src</code> attribute for the script file.
     *
     * <p>Variables are specified via <code>#{variableName}</code>. To insert a literal {@code #}, use {@code ##}. The
     * following variables are supported:
     *
     * <ul>
     *   <li>The variable {@code filename} is replaced with the name of the script file.
     *   <li>The variable {@code extension} is replaced with the extension of the script file (without the period)
     *   <li>The variable {@code basename} is replaced with the basename (name without the extension) of the script
     *   <li>The variable {@code path} is replaced with the path of the script file, relative to the <code>
     *       htmlScriptRoot</code>
     * </ul>
     */
    @SuppressWarnings("unused")
    @Parameter(name = "sourcePath")
    private String sourcePath;

    /**
     * This option is ignored when <code>scriptPath</code> is given.
     *
     * <p>The root directory of the HTML files. Used to construct a relative path from the HTML file the script file.
     *
     * <p>Relative paths are resolved against the <code>htmlRoot</code> option of the plugin configuration. Defaults to
     * the <code>htmlRoot</code> option of the plugin configuration.
     *
     * <p>Use a single slash (<code>/</code>) to use the physical path of the HTML file on the file system (works
     * regardless of the OS).
     *
     * <p>For example, assume the following configuration:
     *
     * <ul>
     *   <li>HTML file - <code>/home/user/project/src/main/resources/webapp/public/pages/profile/index.html</code>
     *   <li>Script file - <code>
     *       /home/user/project/target/generated-resources/frontend/js/public/resources/main/script.min.js</code>
     *   <li>HTML root - <code>/home/user/project/src/main/resources/webapp</code> and the <code>scriptRoot</code>
     *   <li>Script root - <code>/home/user/project/target/generated-resources/frontend/js</code>
     * </ul>
     *
     * Then, the relative path of the HTML file relative to the HTML root is <code>public/pages/profile/index.html
     * </code> and the relative of the JavaScript file is <code>public/resources/main/script.min.js</code>.
     *
     * <p>Finally, the script file is relativized against the HTML file; and the final relative path used to update the
     * script tag in the HTML file is <code>../../resources/main/script.min.js</code>
     */
    @SuppressWarnings("unused")
    @Parameter(name = "htmlRoot")
    private String htmlRoot;

    /** The encoding (charset) of the HTML files. Defaults to <code>UTF-8</code>. */
    @Parameter(name = "htmlEncoding")
    private String htmlEncoding;

    /**
     * This option is ignored when <code>scriptPath</code> is given.
     *
     * <p>The root directory of the script files. Used to construct a relative path from the HTML file the script file.
     *
     * <p>Relative paths are resolved against the <code>htmlScriptRoot</code> option of the plugin configuration.
     * Defaults to the <code>htmlScriptRoot</code> option of the plugin configuration.
     *
     * <p>Use a single slash (<code>/</code>) to use the physical path of the HTML file on the file system (works
     * regardless of the OS).
     *
     * <p>For example, assume the following configuration:
     *
     * <ul>
     *   <li>HTML file - <code>/home/user/project/src/main/resources/webapp/public/pages/profile/index.html</code>
     *   <li>Script file - <code>
     *       /home/user/project/target/generated-resources/frontend/js/public/resources/main/script.min.js</code>
     *   <li>HTML root - <code>/home/user/project/src/main/resources/webapp</code> and the <code>scriptRoot</code>
     *   <li>Script root - <code>/home/user/project/target/generated-resources/frontend/js</code>
     * </ul>
     *
     * Then, the relative path of the HTML file relative to the HTML root is <code>public/pages/profile/index.html
     * </code> and the relative of the JavaScript file is <code>public/resources/main/script.min.js</code>.
     *
     * <p>Finally, the script file is relativized against the HTML file; and the final relative path used to update the
     * script tag in the HTML file is <code>../../resources/main/script.min.js</code>
     */
    @SuppressWarnings("unused")
    @Parameter(name = "htmlScriptRoot")
    private String htmlScriptRoot;

    /**
     * Specifier for the scripts to update. May be one of the following:
     *
     * <ul>
     *   <li><code>id:[ID}</code> - Searches for a script element with the specified ID.
     *   <li><code>css:[selector]</code> - Searches for the script elements via the given CSS selector. Should match the
     *       scripts to update. For example, <code>css:.custom</code> will select all script elements with the class
     *       <code>custom</code>.
     *   <li><code>xpath:[xpath]</code> - Searches for the script elements via the given XPath selector. Should match
     *       the scripts to update. For example, <code>css://script</code> will select all script elements.
     * </ul>
     *
     * When not given, defaults to the first script element in the document.
     */
    @SuppressWarnings("unused")
    @Parameter(name = "scripts")
    private String scripts;

    public FileSet getHtmlFiles() {
        if (htmlFiles == null) {
            htmlFiles = new FileSet();
            htmlFiles.getIncludes().add("**/*.html");
        }
        return htmlFiles;
    }

    public String getHtmlDir() {
        return htmlDir;
    }

    public String getSourcePath() {
        if (sourcePath == null) {
            sourcePath = "";
        }
        return sourcePath;
    }

    public String getHtmlRoot() {
        return htmlRoot;
    }

    public String getHtmlScriptRoot() {
        return htmlScriptRoot;
    }

    public String getScripts() {
        if (scripts == null) {
            scripts = "";
        }
        return scripts;
    }

    public List<String> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return attributes.isEmpty() ? List.of("src") : attributes;
    }

    public String getHtmlEncoding() {
        if (htmlEncoding == null) {
            htmlEncoding = "";
        }
        return htmlEncoding.isEmpty() ? UTF_8.name() : htmlEncoding;
    }
}
