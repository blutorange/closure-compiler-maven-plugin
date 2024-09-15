/*
 * Closure Compiler Maven Plugin https://github.com/blutorange/closure-compiler-maven-plugin
 * Original license terms below. Changes were made to this file.
 *
 * <p>Minify Maven Plugin https://github.com/samaxes/minify-maven-plugin Copyright (c) 2009 samaxes.com
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.common.*;
import com.google.gson.Gson;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.ChunkOutputType;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.WarningLevel;
import com.google.javascript.jscomp.deps.ModuleLoader.ResolutionMode;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/** Goal for combining and/or minifying JavaScript files with closure compiler. */
@Mojo(name = "minify", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class MinifyMojo extends AbstractMojo {

    private final BuildContext buildContext;

    /**
     * By default, when the output file is the same as the input file, compilation is terminated with an error. This is
     * done to prevent source files from being overwritten accidentally with a bad configuration. If you are certain you
     * want to replace the input files (such as when the input files themselves are temporary files that have been
     * generated), set this option to {@code true}. Defaults to {@code false}.
     *
     * <p>Note: When enabling this option, you might also want to set <code>skipMerge</code> to <code>true</code> and
     * the <code>outputFilename</code> to <code>#{path}/#{basename}.#{extension}</code>.
     */
    @SuppressWarnings("unused")
    @Parameter(property = "allowReplacingInputFiles", defaultValue = "false")
    private boolean allowReplacingInputFiles;

    /**
     * Base directory for source files. This should be an absolute path; if not, it must be relative to the project base
     * directory. Use variables such as {@code basedir} to make it relative to the current directory.
     */
    @SuppressWarnings("unused")
    @Parameter(property = "baseSourceDir", defaultValue = "${basedir}/src/main/webapp")
    private File baseSourceDir;

    /**
     * Base directory for output files. This should be an absolute path; if not, it must be relative to the project base
     * directory. Use variables such as {@code project.build.directory} to make it relative to the current directory.
     */
    @SuppressWarnings("unused")
    @Parameter(property = "baseTargetDir", defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File baseTargetDir;

    /** Size of the buffer used to read source files. */
    @SuppressWarnings("unused")
    @Parameter(property = "bufferSize", defaultValue = "4096")
    private int bufferSize;

    /**
     * Specify aggregations in an external JSON formatted config file. If not an absolute path, it must be relative to
     * the project base directory.
     *
     * @since 1.7.5
     */
    @SuppressWarnings("unused")
    @Parameter(property = "bundleConfiguration")
    private String bundleConfiguration;

    /**
     * Enables experimental support for allowing dynamic import expressions {@code import('./path')} to pass through the
     * compiler unchanged. Enabling this requires setting {@link #getClosureLanguageOut() closureLanguageOut} to at
     * least {@link LanguageMode#ECMASCRIPT_2020 ECMASCRIPT_2020}.
     *
     * @since 2.21.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureAllowDynamicImport", defaultValue = "false")
    private boolean closureAllowDynamicImport;

    /**
     * Generate {@code $inject} properties for AngularJS for functions annotated with {@code @ngInject}.
     *
     * @since 1.7.3
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureAngularPass", defaultValue = "false")
    private boolean closureAngularPass;

    /**
     * Enable additional optimizations based on the assumption that the output will be wrapped with a function wrapper.
     * This flag is used to indicate that "global" declarations will not actually be global but instead isolated to the
     * compilation unit. This enables additional optimizations.
     *
     * @since 2.1
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureAssumeFunctionWrapper", defaultValue = "false")
    private boolean closureAssumeFunctionWrapper;

    /**
     * Regardless of input type, the compiler will normalize all files and bundle them together. By default, a single
     * output file is produced. However, this may not work for you if your application is big. In that case, you may
     * want to have the compiler break your code up into multiple chunks that can be loaded separately. You will design
     * your application so that the code you always need gets loaded in an initial chunk, probably from a &lt;script&gt;
     * tag, then that chunk will load others (which may load still others) as needed in order to support the user's
     * actions. (e.g. The user may request a new display view, which requires you to load the code for showing that
     * view.)
     *
     * <ul>
     *   <li>GLOBAL_NAMESPACE (Chunks as Scripts using a Global Namespace): This is the default option and the compiler
     *       will produce standard scripts. This mode is normally paired with the <code>closureOutputWrapper</code> flag
     *       for script isolation and the <code>closureRenamePrefixNamespace</code> flag so that symbols can be
     *       referenced across chunks.
     *   <li>ES_MODULES (Chunks as EcmaScript modules): The compiler will output es modules and cross chunk references
     *       will utilize the <code>import</code> and <code>export</code> statements. Since modules have built in
     *       isolation and modern browsers know how to load them, this option is by far the easiest.
     * </ul>
     *
     * @since 2.27.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureChunkOutputType", defaultValue = "GLOBAL_NAMESPACE")
    private ChunkOutputType closureChunkOutputType;

    /**
     * Whether the error output from the closure compiler is colorized. Color codes may not be supported by all
     * terminals.
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureColorizeErrorOutput", defaultValue = "true")
    private boolean closureColorizeErrorOutput;

    /**
     * The degree of compression and optimization to apply to your JavaScript.<br>
     * There are three possible compilation levels:
     *
     * <ul>
     *   <li>{@code WHITESPACE_ONLY}: Just removes whitespace and comments from your JavaScript.
     *   <li>{@code SIMPLE_OPTIMIZATIONS}: Performs compression and optimization that does not interfere with the
     *       interaction between the compiled JavaScript and other JavaScript. This level renames only local variables.
     *   <li>{@code ADVANCED_OPTIMIZATIONS}: Achieves the highest level of compression by renaming symbols in your
     *       JavaScript. When using {@code ADVANCED_OPTIMIZATIONS} compilation you must perform extra steps to preserve
     *       references to external symbols. See <a href="/closure/compiler/docs/api-tutorial3">Advanced Compilation and
     *       Externs</a> for more information about {@code ADVANCED_OPTIMIZATIONS}.
     *   <li>{@code BUNDLE}: Leaves all compiler options unchanged. For advanced usage if you want to set the relevant
     *       options yourself.
     * </ul>
     *
     * @since 1.7.2
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureCompilationLevel", defaultValue = "SIMPLE_OPTIMIZATIONS")
    private CompilationLevel closureCompilationLevel;

    /**
     * Collects information mapping the generated (compiled) source back to its original source for debugging purposes.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureCreateSourceMap", defaultValue = "false")
    private boolean closureCreateSourceMap;

    /**
     * Enable debugging options. Property renaming uses long mangled names which can be mapped back to the original
     * name.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureDebug", defaultValue = "false")
    private boolean closureDebug;

    /**
     * Override the value of variables annotated with {@code @define}.<br>
     * The format is:
     *
     * <pre>
     * <code class="language-java">
     * &lt;define>
     *     &lt;name>value&lt;/name>
     * &lt;/define>
     * </code>
     * </pre>
     *
     * where {@code <name>} is the name of a {@code @define} variable and {@code value} is a JavaScript boolean, number
     * or string literal. That is, use quotation marks to specify a string: {@code "First line\nseconds line"}
     *
     * @since 1.7.5
     */
    @Parameter(property = "closureDefineReplacements")
    private HashMap<String, String> closureDefineReplacements;

    /**
     * When you use {@code closureDependencyMode} PRUNE or PRUNE_LEGACY, you must specify to the compiler what the entry
     * points of your application are. Beginning at those entry points, it will trace through the files to discover what
     * sources are actually referenced and will drop all other files.
     *
     * <p>Adds a collection of symbols to always keep. In dependency pruning mode, we will automatically keep all the
     * transitive dependencies of these symbols. The syntactic form of a symbol depends on the type of dependency
     * primitives we're using. For example, {@code goog.provide('foo.bar')} provides the symbol {@code foo.bar}. Entry
     * points can be scoped to a module by specifying {@code mod2:foo.bar}.
     *
     * <p>There are two different types of entry points, closures and modules:
     *
     * <ul>
     *   <li>{@code closure}: A closure namespace used as an entry point. May start with {@code goog:} when provided as
     *       a flag from the command line. Closure entry points may also be formatted as:
     *       {@code goog:moduleName:name.space} which specifies that the module name and provided namespace are
     *       different
     *   <li>{@code file}: Must start with the prefix {@code file:}. AES6 or CommonJS modules used as an entry point.
     *       The file path is relative to the {@code sourceDir}.
     * </ul>
     *
     * @since 2.0.0
     */
    @Parameter(property = "closureDependencyEntryPoints")
    private ArrayList<String> closureDependencyEntryPoints;

    /**
     * How compiler should prune files based on the provide-require dependency graph.
     *
     * <ul>
     *   <li>{@code NONE} All input files will be included in the compilation in the order they were specified in.
     *   <li>{@code SORT_ONLY} All input files will be included in the compilation in dependency order.
     *   <li>{@code PRUNE} Input files that are transitive dependencies of the entry points will be included in the
     *       compilation in dependency order. All other input files will be dropped. All entry points must be explicitly
     *       defined.
     *   <li>{@code PRUNE_LEGACY} (deprecated) Input files that are transitive dependencies of the entry points will be
     *       included in the compilation in dependency order. All other input files will be dropped. In addition to the
     *       explicitly defined entry points, moochers (files not explicitly defining a module) are implicit entry
     *       points.
     * </ul>
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureDependencyMode", defaultValue = "NONE")
    private DependencyModeFlag closureDependencyMode;

    /**
     * Instructs the compiler to replace dynamic import expressions with a function call using the specified name. This
     * allows dynamic import expressions to be externally polyfilled when the output language level does not natively
     * support them.
     *
     * @since 2.22.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureDynamicImportAlias")
    private String closureDynamicImportAlias;

    /**
     * Start output with <code>'use strict';</code>.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureEmitUseStrict", defaultValue = "true")
    private boolean closureEmitUseStrict;

    /**
     * Determines the set of builtin externs to load.<br>
     * Options: BROWSER, CUSTOM.
     *
     * @since 1.7.5
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureEnvironment", defaultValue = "BROWSER")
    private CompilerOptions.Environment closureEnvironment;

    /**
     * List of JavaScript files containing code that declares function names or other symbols. Use
     * {@code closureExterns} to preserve symbols that are defined outside the code you are compiling. The
     * {@code closureExterns} parameter only has an effect if you are using a {@code CompilationLevel} of
     * {@code ADVANCED_OPTIMIZATIONS}.<br>
     * These file names are relative to {@link #baseSourceDir} directory.
     *
     * <pre>
     * &lt;closureExternDeclarations&gt;
     *   &lt;closureExternDeclaration&gt;
     *     &lt;includes&gt;
     *       &lt;include&gt;externs/*.js&lt;/include&gt;
     *     &lt;/includes&gt;
     *     &lt;excludes&gt;
     *       &lt;exclude&gt;externs/doNotInclude.js&lt;/exclude&gt;
     *     &lt;/excludes&gt;
     *   &lt;/closureExternDeclaration&gt;
     * &lt;/closureExternDeclarations&gt;
     * </pre>
     *
     * @since 2.16.0
     */
    @Parameter(property = "closureExternDeclarations")
    private ArrayList<FileSet> closureExternDeclarations;

    /**
     * Deprecated, use {@link #closureExternDeclarations} instead, it lets you specify includes and excludes.
     *
     * @deprecated Deprecated, use {@link #closureExternDeclarations} instead, it lets you specify includes and
     *     excludes.
     * @since 1.7.2
     */
    @Deprecated
    @Parameter(property = "closureExterns")
    private ArrayList<String> closureExterns;

    /**
     * A whitelist of tag names in JSDoc. Needed to support JSDoc extensions like <code>ngdoc</code>.
     *
     * @since 1.7.5
     */
    @Parameter(property = "closureExtraAnnotations")
    private ArrayList<String> closureExtraAnnotations;

    /**
     * Force injection of named runtime libraries. The format is &lt;name&gt; where &lt;name&gt; is the name of a
     * runtime library. Possible libraries include: <code>base</code>, <code>es6_runtime</code>, <code>
     * runtime_type_check</code>
     *
     * @since 2.1.0
     */
    @Parameter(property = "closureForceInjectLibs")
    private ArrayList<String> closureForceInjectLibs;

    /**
     * If {@code true}, include the content of the source file in the source map directly (via the {@code sourceContent}
     * property). This makes the source file bigger, but does not require the original source file to be added to the
     * browser dev tools.
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureIncludeSourcesContent", defaultValue = "false")
    private boolean closureIncludeSourcesContent;

    /**
     * Allow injecting runtime libraries.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureInjectLibraries", defaultValue = "true")
    private boolean closureInjectLibraries;

    /**
     * Path prefixes to be removed from ES6 & CommonJS modules.
     *
     * @since 2.5.0
     */
    @Parameter(property = "closureJsModuleRoots")
    private ArrayList<String> closureJsModuleRoots;

    /**
     * Whether to isolate polyfills from the global scope.
     *
     * <p>Polyfill isolation is an output mode that may optionally be used alongside polyfill injection.
     *
     * <p>Polyfill isolation was motivated by two related issues.
     *
     * <ul>
     *   <li>sometimes, the existence of Closure polyfills on the page would cause other non-Closure-compiled code to
     *       break, because of conflicting assumptions in a polyfill implementation used by third-party code.
     *   <li>
     *   <li>sometimes, Closure-compiled code would break, because of existing polyfills on the page that violated some
     *       assumption Closure Compiler makes.
     * </ul>
     *
     * These issues were generally seen by projects compiling code for inclusion as a script on third-party websites,
     * along with arbitrary JavaScript not under their control.
     *
     * <p>Polyfill isolation mode attempts to solve these problems by "isolating" Closure polyfills and code from other
     * code & polyfills. It is not intended to protect against malicious actors; it is instead intended to solve cases
     * where other polyfill implementations are either buggy or (more likely) make conflicting assumptions.
     *
     * @since 2.23.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureIsolatePolyfills", defaultValue = "false")
    private boolean closureIsolatePolyfills;

    /**
     * Refers to which version of ECMAScript to assume when checking for errors in your code.<br>
     * Possible values are:
     *
     * <ul>
     *   <li>{@code ECMASCRIPT3}: Checks code assuming ECMAScript 3 compliance, and gives errors for code using features
     *       only present in later versions of ECMAScript.
     *   <li>{@code ECMASCRIPT5}: Checks code assuming ECMAScript 5 compliance, allowing new features not present in
     *       ECMAScript 3, and gives errors for code using features only present in later versions of ECMAScript.
     *   <li>{@code ECMASCRIPT5_STRICT}: Like {@code ECMASCRIPT5} but assumes compliance with strict mode ({@code 'use
     *       strict';}).
     *   <li>{@code ECMASCRIPT_2015}: Checks code assuming ECMAScript 2015 compliance.
     *   <li>{@code ECMASCRIPT_2016}: Checks code assuming ECMAScript 2016 compliance.
     *   <li>{@code ECMASCRIPT_2017}: Checks code assuming ECMAScript 2017 compliance.
     *   <li>{@code ECMASCRIPT_2018}: Checks code assuming ECMAScript 2018 compliance.
     *   <li>{@code ECMASCRIPT_2019}: Checks code assuming ECMAScript 2019 compliance.
     *   <li>{@code ECMASCRIPT_2020}: Checks code assuming ECMAScript 2020 compliance.
     *   <li>{@code ECMASCRIPT_2021}: Checks code assuming ECMAScript 2021 compliance.
     *   <li>{@code ECMASCRIPT_NEXT}: Checks code assuming ECMAScript latest draft standard.
     *   <li>{@code STABLE} Use stable features
     * </ul>
     *
     * @since 1.7.2
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureLanguageIn", defaultValue = "ECMASCRIPT_NEXT")
    private LanguageMode closureLanguageIn;

    /**
     * Refers to which version of ECMAScript your code will be returned in.<br>
     * It is used to transpile between different levels of ECMAScript. Possible values are
     *
     * <ul>
     *   <li>{@code ECMASCRIPT3}: Outputs code with ECMAScript 3 compliance.
     *   <li>{@code ECMASCRIPT5}: Outputs code with ECMAScript 2015.
     *   <li>{@code ECMASCRIPT5_STRICT}: Like {@code ECMASCRIPT5} but assumes compliance with strict mode ({@code 'use
     *       strict';}).
     *   <li>{@code ECMASCRIPT_2015}: Outputs code with ECMAScript 2015.
     *   <li>{@code ECMASCRIPT_2016}: Outputs code with ECMAScript 2016.
     *   <li>{@code ECMASCRIPT_2017}: Outputs code with ECMAScript 2017.
     *   <li>{@code ECMASCRIPT_2018}: Outputs code with ECMAScript 2018.
     *   <li>{@code ECMASCRIPT_2019}: Outputs code with ECMAScript 2019.
     *   <li>{@code STABLE}: Use stable features
     *   <li>{@code NO_TRANSPILE}: Do not perform any transpilation.
     * </ul>
     *
     * @since 1.7.5
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureLanguageOut", defaultValue = "ECMASCRIPT_2015")
    private LanguageMode closureLanguageOut;

    /**
     * Specifies how the compiler locates modules.
     *
     * <ul>
     *   <li><code>BROWSER</code>: Requires all module imports to begin with a '.' or '/' and have a file extension.
     *       Mimics the behavior of MS Edge.
     *   <li><code>BROWSER_WITH_TRANSFORMED_PREFIXES</code>: A limited superset of <code>BROWSER</code> that transforms
     *       some path prefixes. For example, one could configure this so that "@root/" is replaced with
     *       "/my/path/to/project/" within import paths.
     *   <li><code>NODE</code>: Uses the node module rules. Modules which do not begin with a "." or "/" character are
     *       looked up from the appropriate node_modules folder. Includes the ability to require directories and JSON
     *       files. Exact match, then ".js", then ".json" file extensions are searched.
     *   <li><code>WEBPACK</code>: Looks up modules from a special lookup map. Uses a lookup map provided by webpack to
     *       locate modules from a numeric id used during import.
     * </ul>
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureModuleResolution", defaultValue = "BROWSER")
    private ResolutionMode closureModuleResolution;

    /**
     * If not an empty or blank string, interpolate output into this string at the place denoted by the marker token
     * {@code %output%}. Use marker token {@code %output|jsstring%} to perform JavaScript string escaping on the output.
     *
     * <p>When using this options with a source map, the map is adjusted appropriately to match the code.
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureOutputWrapper")
    private String closureOutputWrapper;

    /**
     * Normally, when there are an equal number of single and double quotes in a string, the compiler will use double
     * quotes. Set this to true to prefer single quotes.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closurePreferSingleQuotes", defaultValue = "false")
    private boolean closurePreferSingleQuotes;

    /**
     * If {@code true}, the processed ("minified") file is pretty printed (formatted with new lines).
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closurePrettyPrint", defaultValue = "false")
    private boolean closurePrettyPrint;

    /**
     * Processes built-ins from the Closure library, such as <code>goog.require()</code>, <code>goog.provide()</code>,
     * and <code>goog.exportSymbol( )</code>.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureProcessClosurePrimitives", defaultValue = "true")
    private boolean closureProcessClosurePrimitives;

    /**
     * Process CommonJS modules to a concatenable form.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureProcessCommonJsModules", defaultValue = "false")
    private boolean closureProcessCommonJsModules;

    /**
     * Specifies the name of an object that will be used to store all non-extern globals.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureRenamePrefixNamespace")
    private String closureRenamePrefixNamespace;

    /**
     * Specifies a prefix that will be prepended to all variables.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureRenameVariablePrefix")
    private String closureRenameVariablePrefix;

    /**
     * If {@code true}, ES6 polyfills are written to the output file (such as for Set, Map etc.)
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureRewritePolyfills", defaultValue = "true")
    private boolean closureRewritePolyfills;

    /**
     * Source map location mapping. This is a prefix mapping from the file system path to the web server path. The
     * source map contains a reference to the original source files; and this may be different on the web server. The
     * location of the source file is always relative to the given {@code sourceDir}. This defines a list of
     * replacements. For each source file, the first matching replacement is used. If the source file starts with the
     * prefix as given by the name, it matches and is replaced with the value. For example:
     *
     * <pre>
     * &lt;closureSourceMapLocationMappings&gt;
     *   &lt;closureSourceMapLocationMapping&gt;
     *     &lt;name>js/&lt;/name&gt;
     *     &lt;value>/web/www/js&lt;/value&gt;
     *   &lt;/closureSourceMapLocationMapping&gt;
     * &lt;/closureSourceMapLocationMappings&gt;
     * </pre>
     *
     * Assume the source files are {@code js/file1.js} and {@code js/file2.js}. The above replaces them with
     * {@code /web/www/js/file1.js} and {@code /web/www/js/file2.js}. This is then path that will be used in the source
     * map to reference the original source file. If no location mappings are specified, the path of the source files
     * relative to the created source map is used instead.
     *
     * <p>If you set the name to an empty string, it matches all paths.
     *
     * @since 2.5.0
     */
    @Parameter(property = "closureSourceMapLocationMappings")
    private ArrayList<ClosureSourceMapLocationMapping> closureSourceMapLocationMappings;

    /**
     * Name of the source map, if one is created. This is interpreted as a relative path to where the processed
     * JavaScript file is written to. By default, the extension {@code .map} is added to the minified file. Variables
     * are specified via <code>#{variableName}</code>. To insert a literal {@code #}, use {@code ##}. The following
     * variables are available:
     *
     * <ul>
     *   <li>The variable {@code filename} is replaced with the name of the minified file
     *   <li>The variable {@code extension} is replaced with the extension of the file
     *   <li>The variable {@code basename} is replaced with the basename (name without the extension) of the file.
     * </ul>
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureSourceMapName", defaultValue = "#{filename}.map")
    private String closureSourceMapName;

    /**
     * After creating the source map, the browser needs to find it. There are several options available:
     *
     * <ul>
     *   <li>{@code reference} (the default): Create a source map named [originalFile].map, and add a reference to it in
     *       the minified file.
     *   <li>{@code file}: Just create a source map named [originalFile].map, do not add a reference in the minified
     *       file. This may be useful when you want to add the {@code Source-Map} HTTP header.
     *   <li>{@code inline}: Do not write a separate source map file, but instead include the source map content in the
     *       minified file (as base64). This makes it easier for the browser to find the source map. Especially useful
     *       when used with JSF/Primefaces or other frameworks that do not use standard URLs.
     * </ul>
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureSourceMapOutputType", defaultValue = "reference")
    private SourceMapOutputType closureSourceMapOutputType;

    /**
     * Assume input sources are to run in strict mode.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureStrictModeInput", defaultValue = "true")
    private boolean closureStrictModeInput;

    /**
     * If {@code false}, converts some characters such as '&lt;' and '&gt;' to '\x3c' and '\x3d' so that they are safe
     * to put inside a script tag in an HTML file.
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureTrustedStrings", defaultValue = "true")
    private boolean closureTrustedStrings;

    /**
     * Enable or disable the optimizations based on available type information. Inaccurate type annotations may result
     * in incorrect results.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureUseTypesForOptimization", defaultValue = "false")
    private boolean closureUseTypesForOptimization;

    /**
     * Specifies the warning level to use: <code>QUIET</code>, <code>DEFAULT</code>, or <code>VERBOSE</code>. You can
     * override specific warnings via {@link #closureWarningLevels}.
     *
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "closureWarningLevel", defaultValue = "DEFAULT")
    private WarningLevel closureWarningLevel;

    /**
     * Treat certain warnings as the specified CheckLevel:
     *
     * <ul>
     *   <li>{@code ERROR}: Makes all warnings of the given group to build-breaking error.
     *   <li>{@code WARNING}: Makes all warnings of the given group a non-breaking warning.
     *   <li>{@code OFF}: Silences all warnings of the given group.
     * </ul>
     *
     * Example:
     *
     * <pre>
     * <code class="language-java">
     * &lt;closureWarningLevels>
     *     &lt;nonStandardJsDocs>OFF&lt;/nonStandardJsDocs>
     * &lt;/closureWarningLevels>
     * </code>
     * </pre>
     *
     * For the complete list of diagnostic groups please visit <a href=
     * "https://github.com/google/closure-compiler/wiki/Warnings">https://github.com/google/closure-compiler/wiki/Warnings</a>.
     *
     * @since 1.7.5
     */
    @Parameter(property = "closureWarningLevels")
    private HashMap<String, String> closureWarningLevels;

    /**
     * If a supported character set is specified, it will be used to read the input file. Otherwise, it will assume that
     * the platform's default character set is being used. The output file is encoded using the same character set.<br>
     * See the <a href="http://www.iana.org/assignments/character-sets">IANA Charset Registry</a> for a list of valid
     * encoding types.
     *
     * @since 1.3.2
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}", alias = "charset")
    private String encoding;

    /**
     * JavaScript files to exclude. Specified as fileset patterns which are relative to the JavaScript source directory.
     *
     * @since 1.2
     */
    @Parameter(property = "excludes")
    private ArrayList<String> excludes;

    /**
     * For each bundle, this plugin performs a check whether the input or output files have changed and skips the
     * execution in case they haven't. Set this flag to {@code true} to force the execution.
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "force", defaultValue = "false")
    private boolean force;

    /**
     * JavaScript files to include. Specified as fileset patterns which are relative to the JavaScript source directory.
     *
     * @since 1.2
     */
    @Parameter(property = "includes")
    private ArrayList<String> includes;

    /**
     * Optional. Allows you to update script tags in an HTML file with the path of the processed file(s).
     *
     * <p>Both HTML and XHTML files are supported.
     *
     * @since 2.32.0
     */
    @Parameter(property = "htmlUpdates")
    private ArrayList<HtmlUpdate> htmlUpdates;

    /**
     * Base directory relative to which the <code>htmlFiles</code> to process are evaluated, see the option <code>
     * htmlUpdates</code>. Relative paths are evaluated relative to the project's base directory.
     */
    @SuppressWarnings("unused")
    @Parameter(name = "baseHtmlDir", defaultValue = "${project.basedir}/src/main/resources")
    private String baseHtmlDir;

    /**
     * Base directory relative to which the <code>htmlFiles</code> to process are evaluated, see the option <code>
     * htmlUpdates</code>. Relative paths are evaluated relative to the <code>baseHtmlDir</code> option. When not given,
     * defaults to <code>baseHtmlDir</code>.
     */
    @SuppressWarnings("unused")
    @Parameter(name = "htmlDir")
    private String htmlDir;

    /**
     * The root directory of the HTML files, see the option <code>htmlUpdates</code>. Used to construct a relative path
     * from the HTML file the script file.
     *
     * <p>Relative paths are resolved against the project's base directory.
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
    @Parameter(name = "baseHtmlRoot", defaultValue = "${project.basedir}/src/main/resources")
    private String baseHtmlRoot;

    /**
     * The root directory of the HTML files, see the option <code>htmlUpdates</code>. Used to construct a relative path
     * from the HTML file the script file.
     *
     * <p>Relative paths are resolved against the <code>baseHtmlRoot</code> option. Defaults to <code>baseHtmlRoot
     * </code>.
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

    /**
     * The root directory of the script files, see the option <code>htmlUpdates</code>. Used to construct a relative
     * path from the HTML file the script file.
     *
     * <p>Relative paths are resolved against the project's base directory.
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
    @Parameter(name = "baseHtmlScriptRoot", defaultValue = "${project.build.directory}/generated-resources")
    private String baseHtmlScriptRoot;

    /**
     * The root directory of the script files, see the option <code>htmlUpdates</code>. Used to construct a relative
     * path from the HTML file the script file.
     *
     * <p>Relative paths are resolved against the <code>baseHtmlScriptRoot</code>. Defaults to the <code>
     * baseHtmlScriptRoot</code>.
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
     * The line separator to be used when merging files etc. Defaults to the default system line separator. Special
     * characters are entered escaped. So for example, to use a new line feed as the separator, set this property to
     * {@code \n} (two characters, a backslash and the letter n).
     *
     * @since 2.0.0
     */
    @Parameter(property = "lineSeparator")
    private String lineSeparator;

    /**
     * By default, messages are logged at the log level set by maven. This option allows you to change the log level.
     * Valid options are {@code all}, {@code debug}, {@code info}, {@code warn}, {@code error}, {@code none}. Leave
     * empty to use the default log level. Please note that you can only decrease, not increase, the log level.
     */
    @SuppressWarnings("unused")
    @Parameter(property = "logLevel")
    private LogLevel logLevel;

    private Log logWrapper;

    /**
     * The output file name of the processed files. This is interpreted as a path relative to the {@code targetDir}.
     *
     * <p>Variables are specified via <code>#{variableName}</code>. To insert a literal {@code #}, use {@code ##}. The
     * following variables are supported:
     *
     * <ul>
     *   <li>The variable {@code filename} is replaced with the name of the minified file.
     *   <li>The variable {@code extension} is replaced with the extension of the file (without the period)
     *   <li>The variable {@code basename} is replaced with the basename (name without the extension) of the file.
     *   <li>In case the files are not merged (option {@code skipMerge} is activated): The variable {@code path} is
     *       replaced with the path of the current file, relative to the {@code sourceDir}.
     * </ul>
     *
     * <p>If merging files, by default the basename is set to {@code script} and the extension to {@code js}, so that
     * the resulting merged file is called {@code script.min.js}.
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "outputFilename", defaultValue = "#{path}/#{basename}.min.#{extension}")
    private String outputFilename;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * When set to `true`, the plugin exits immediately without doing any work at all.
     *
     * @since 2.7.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    /**
     * Skip the merge step. Minification will be applied to each source file individually.
     *
     * @since 1.5.2
     */
    @SuppressWarnings("unused")
    @Parameter(property = "skipMerge", defaultValue = "false")
    private boolean skipMerge;

    /**
     * Skip the minify step. Useful when merging files that are already minified.
     *
     * @since 1.5.2
     */
    @SuppressWarnings("unused")
    @Parameter(property = "skipMinify", defaultValue = "false")
    private boolean skipMinify;

    /**
     * This options lets configure how this plugin checks whether it should skip an execution in case the target file
     * exists already. Usually you do not want to spend unnecessary processing time on transpiling JavaScript files when
     * the input files themselves have not changed. Available options are:
     *
     * <ul>
     *   <li>NEWER - Skip execution if the the target file exists already and all input files are older. Whether a file
     *       is older is judged according to their modification date.
     *   <li>EXISTS - Skip execution if the target file exists already, irrespective of when thee files were last
     *       modified.
     * </ul>
     *
     * These options only apply when {@code force} is set to {@code false}. In case you never want to skip execution,
     * set the option {@code force} to {@code true}.
     *
     * @since 2.9.0
     */
    @SuppressWarnings("unused")
    @Parameter(property = "skipMode", defaultValue = "NEWER")
    private SkipMode skipMode;

    /**
     * Deprecated. For Eclipse with m2e, you can now use <code>&lt;?m2e ignore?&gt;</code> etc. on an execution tag to
     * configure the m2e lifecycle.
     *
     * <p>When this plugin is executed as part of an incremental build (such as me2) and this option is set to <code>
     * true</code>, skip the execution of this plugin.
     *
     * <p>For the m2e integration, this plugin is configured by default to run on incremental builds. When having a
     * project opened in Eclipse, this recreates the minified files every time a source file is changed.
     *
     * <p>You can disable this behavior via the org.eclipse.m2e/lifecycle-mapping plugin. As this is rather verbose,
     * this option offers a convenient way of disabling incremental builds. Please note that tecnically this plugin is
     * still executed on every incremental build cycle, but exits immediately without doing any work.
     *
     * @deprecated For Eclipse with m2e, you can now use <code>&lt;?m2e ignore?&gt;</code> etc. on an execution tag to
     *     configure the m2e lifecycle.
     */
    @SuppressWarnings("unused")
    @Parameter(property = "skipRunOnIncremental", defaultValue = "false")
    @Deprecated
    private boolean skipRunOnIncremental;

    /** JavaScript source directory. This is relative to the {@link #baseSourceDir}. */
    @SuppressWarnings("unused")
    @Parameter(property = "sourceDir", defaultValue = "js")
    private String sourceDir;

    /**
     * JavaScript target directory. Takes the same value as {@code jsSourceDir} when empty. This is relative to the
     * {@link #baseTargetDir}.
     *
     * @since 1.3.2
     */
    @Parameter(property = "targetDir", defaultValue = "js")
    private String targetDir;

    @Inject
    public MinifyMojo(BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    private ProcessFilesTask createJSTask(
            ClosureConfig closureConfig, List<String> includes, List<String> excludes, String outputFilename)
            throws IOException {
        final var processConfig = new FileProcessConfig(
                lineSeparator, bufferSize, force, skipMerge, skipMinify, skipMode, allowReplacingInputFiles);
        final var fileSpecifier = new FileSpecifier(
                baseSourceDir, baseTargetDir, sourceDir, targetDir, includes, excludes, outputFilename);
        final var mojoMeta = new MojoMetaImpl(project, getLog(), encoding, buildContext);
        return new ProcessJSFilesTask(mojoMeta, processConfig, fileSpecifier, closureConfig);
    }

    private Collection<ProcessFilesTask> createTasks(ClosureConfig closureConfig)
            throws MojoFailureException, IOException {
        List<ProcessFilesTask> tasks = new ArrayList<>();

        // If a bundleConfiguration is defined, attempt to use that
        if (StringUtils.isNotBlank(bundleConfiguration)) {
            for (Aggregation aggregation : getAggregations()) {
                tasks.add(createJSTask(
                        closureConfig, aggregation.getIncludes(), aggregation.getExcludes(), aggregation.getName()));
            }
        }
        // Otherwise, fallback to the default behavior
        else {
            tasks.add(createJSTask(closureConfig, includes, excludes, outputFilename));
        }

        return tasks;
    }

    /** Executed when the goal is invoked, it will first invoke a parallel lifecycle, ending at the given phase. */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("skip was to true, so skipping plugin execution.");
            return;
        }

        if (buildContext.isIncremental() && skipRunOnIncremental) {
            getLog().info("skipRunOnIncremental was to true, so skipping incremental build.");
            return;
        }

        if (skipMerge && skipMinify) {
            getLog().warn(
                            "Both merge and minify steps are configured to be skipped. Files will only be copied to their destination without any processing");
        }

        fillOptionalValues();

        final var closureConfig = new ClosureConfig(this);
        final var htmlUpdater = createHtmlUpdater();
        Collection<ProcessFilesTask> processFilesTasks;
        try {
            processFilesTasks = createTasks(closureConfig);
        } catch (final IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        try {
            for (final var task : processFilesTasks) {
                final var processingResults = task.call();
                htmlUpdater.process(processingResults);
            }
        } catch (Exception e) {
            if (e.getCause() instanceof MojoFailureException) {
                throw (MojoFailureException) e.getCause();
            }
            if (e.getCause() instanceof MojoExecutionException) {
                throw (MojoExecutionException) e.getCause();
            }
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private HtmlUpdater createHtmlUpdater() {
        final var mojoMeta = new MojoMetaImpl(project, getLog(), encoding, buildContext);
        final var projectBasedir = project.getBasedir();
        final var resolvedHtmlDir = FileHelper.getAbsoluteFile(projectBasedir, baseHtmlDir, htmlDir);
        final var resolvedHtmlRoot = FileHelper.getAbsoluteFile(projectBasedir, baseHtmlRoot, htmlRoot);
        final var resolvedHtmlScriptRoot =
                FileHelper.getAbsoluteFile(projectBasedir, baseHtmlScriptRoot, htmlScriptRoot);
        final var updateConfig =
                new HtmlUpdateConfigImpl(htmlUpdates, resolvedHtmlDir, resolvedHtmlRoot, resolvedHtmlScriptRoot);
        return new HtmlUpdater(mojoMeta, updateConfig);
    }

    private void fillOptionalValues() {
        if (StringUtils.isBlank(targetDir)) {
            targetDir = sourceDir;
        }
        if (StringUtils.isBlank(encoding)) {
            encoding = Charset.defaultCharset().name();
        }
        if (StringUtils.isBlank(lineSeparator)) {
            lineSeparator = System.lineSeparator();
        } else {
            lineSeparator = StringEscapeUtils.unescapeJava(lineSeparator);
        }
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        if (includes == null) {
            includes = new ArrayList<>();
        }
        if (closureJsModuleRoots == null) {
            closureJsModuleRoots = new ArrayList<>();
        }
        if (closureExtraAnnotations == null) {
            closureExtraAnnotations = new ArrayList<>();
        }
        if (closureExterns == null) {
            closureExterns = new ArrayList<>();
        }
        if (closureExternDeclarations == null) {
            closureExternDeclarations = new ArrayList<>();
        }
        if (closureDependencyEntryPoints == null) {
            closureDependencyEntryPoints = new ArrayList<>();
        }
        if (closureForceInjectLibs == null) {
            closureForceInjectLibs = new ArrayList<>();
        }

        if (closureWarningLevels == null) {
            closureWarningLevels = new HashMap<>();
        }
        if (closureDefineReplacements == null) {
            closureDefineReplacements = new HashMap<>();
        }
        if (closureSourceMapLocationMappings == null) {
            closureSourceMapLocationMappings = new ArrayList<>();
        }
        if (htmlUpdates == null) {
            htmlUpdates = new ArrayList<>();
        }
    }

    private Collection<Aggregation> getAggregations() throws MojoFailureException {
        if (StringUtils.isBlank(bundleConfiguration)) {
            return Collections.emptySet();
        }
        AggregationConfiguration aggregationConfiguration;
        try (Reader bundleConfigurationReader =
                new FileReader(FileHelper.getAbsoluteFile(project.getBasedir(), bundleConfiguration))) {
            aggregationConfiguration = new Gson().fromJson(bundleConfigurationReader, AggregationConfiguration.class);
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Failed to open the bundle configuration file [" + bundleConfiguration + "].", e);
        }
        return CollectionUtils.emptyIfNull(aggregationConfiguration.getBundles());
    }

    public File getBaseSourceDir() {
        return baseSourceDir;
    }

    public ChunkOutputType getClosureChunkOutputType() {
        return closureChunkOutputType;
    }

    public CompilationLevel getClosureCompilationLevel() {
        return closureCompilationLevel;
    }

    public HashMap<String, String> getClosureDefineReplacements() {
        return closureDefineReplacements;
    }

    public ArrayList<String> getClosureDependencyEntryPoints() {
        return closureDependencyEntryPoints;
    }

    public DependencyModeFlag getClosureDependencyMode() {
        return closureDependencyMode;
    }

    public String getClosureDynamicImportAlias() {
        return closureDynamicImportAlias;
    }

    public CompilerOptions.Environment getClosureEnvironment() {
        return closureEnvironment;
    }

    public ArrayList<FileSet> getClosureExternDeclarations() {
        return closureExternDeclarations;
    }

    public ArrayList<String> getClosureExterns() {
        return closureExterns;
    }

    public ArrayList<String> getClosureExtraAnnotations() {
        return closureExtraAnnotations;
    }

    public ArrayList<String> getClosureForceInjectLibs() {
        return closureForceInjectLibs;
    }

    public ArrayList<String> getClosureJsModuleRoots() {
        return closureJsModuleRoots;
    }

    public LanguageMode getClosureLanguageIn() {
        return closureLanguageIn;
    }

    public LanguageMode getClosureLanguageOut() {
        return closureLanguageOut;
    }

    public ResolutionMode getClosureModuleResolution() {
        return closureModuleResolution;
    }

    public String getClosureOutputWrapper() {
        return closureOutputWrapper;
    }

    public String getClosureRenamePrefixNamespace() {
        return closureRenamePrefixNamespace;
    }

    public String getClosureRenameVariablePrefix() {
        return closureRenameVariablePrefix;
    }

    public ArrayList<ClosureSourceMapLocationMapping> getClosureSourceMapLocationMappings() {
        return closureSourceMapLocationMappings;
    }

    public String getClosureSourceMapName() {
        return closureSourceMapName;
    }

    public SourceMapOutputType getClosureSourceMapOutputType() {
        return closureSourceMapOutputType;
    }

    public WarningLevel getClosureWarningLevel() {
        return closureWarningLevel;
    }

    public HashMap<String, String> getClosureWarningLevels() {
        return closureWarningLevels;
    }

    public String getEncoding() {
        return encoding;
    }

    @Override
    public Log getLog() {
        if (logWrapper == null) {
            logWrapper = new LogWrapper(super.getLog(), logLevel);
        }
        return logWrapper;
    }

    public boolean isClosureAllowDynamicImport() {
        return closureAllowDynamicImport;
    }

    public boolean isClosureAngularPass() {
        return closureAngularPass;
    }

    public boolean isClosureAssumeFunctionWrapper() {
        return closureAssumeFunctionWrapper;
    }

    public boolean isClosureColorizeErrorOutput() {
        return closureColorizeErrorOutput;
    }

    public boolean isClosureCreateSourceMap() {
        return closureCreateSourceMap;
    }

    public boolean isClosureDebug() {
        return closureDebug;
    }

    public boolean isClosureEmitUseStrict() {
        return closureEmitUseStrict;
    }

    public boolean isClosureIncludeSourcesContent() {
        return closureIncludeSourcesContent;
    }

    public boolean isClosureInjectLibraries() {
        return closureInjectLibraries;
    }

    public boolean isClosureIsolatePolyfills() {
        return closureIsolatePolyfills;
    }

    public boolean isClosurePreferSingleQuotes() {
        return closurePreferSingleQuotes;
    }

    public boolean isClosurePrettyPrint() {
        return closurePrettyPrint;
    }

    public boolean isClosureProcessClosurePrimitives() {
        return closureProcessClosurePrimitives;
    }

    public boolean isClosureProcessCommonJsModules() {
        return closureProcessCommonJsModules;
    }

    public boolean isClosureRewritePolyfills() {
        return closureRewritePolyfills;
    }

    public boolean isClosureStrictModeInput() {
        return closureStrictModeInput;
    }

    public boolean isClosureTrustedStrings() {
        return closureTrustedStrings;
    }

    public boolean isClosureUseTypesForOptimization() {
        return closureUseTypesForOptimization;
    }
}
