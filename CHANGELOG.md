# Closure Compiler Maven Plugin

See also the [closure compiler changelog](https://github.com/google/closure-compiler/wiki/Releases).

## 2.32.0 (unreleased)

* feat: Add new option `htmlUpdates` for updating HTML files with the generated files, #4
* feat: Add new option `gzip` to create gzip files next to each ouptut file, #5
* chore: Add spotless code formatter.
* chore: Minor code cleanup.
* chore: Update dependencies.

## 2.31.0

* Update to closure compiler `v20240317`
* Improve error message when path relativization fails #68

## 2.30.0

* Support an empty prefix location mapping name, relates to #65. An empty name matches all paths.

## 2.29.0

* Update to closure compiler `v20231112`

## 2.28.0

* Fixes #64 - do not fail when input directory does not exist and reduce log level to `warn` when no input files were found.

## 2.27.0

* Update to closure compiler `v20230802`.
* Update dependencies
* Add option `closureChunkOutputType`, see (the wiki for Chunk output for dynamic loading)[https://github.com/google/closure-compiler/wiki/Chunk-output-for-dynamic-loading]. For `closureAllowDynamicImport` to work, this needs to be
  set to `ES_MODULES`.

## 2.26.0

* Update to closure compiler `v20230228`.
* Update dependencies

## 2.25.0

* Update to closure compiler `v20230103`

## 2.24.0

* Update to closure compiler `v20220905`

## 2.23.0

* Update to closure compiler `v20220601`
* Set default input language of JSCompiler to `ES_NEXT`
* Add flag for isolating polyfills (`closureIsolatePolyfills`)

## 2.22.0

* Compiled against Java 11.
* Update to closure compiler version `v20211006`
* Add flag for setting the dynamic import alias (`closureDynamicImportAlias`)

## 2.21.0

* Update to closure compiler version `v20210202`
* Add flag for allowing dynamic import expression (`closureAllowDynamicImport`) #57

## 2.20.0

* Update to closure compiler version `v20210106`

## 2.19.0

* Update to closure compiler version `v20201006`
* Add new flag `allowReplacingInputFiles`, see issue #56

## 2.18.0

* Update to closure compiler version `v20200719`

## 2.17.0

* Update to closure compiler version `v20200614`

## 2.16.0

* Update to closure compiler version `v20200426`
* Options `closureExterns` is now deprecated in favor of `closureExternDeclarations`. The latter lets you specify
  includes and excludes. This also lets you include all extern files in a directory.
* Add test for `externDeclarations`.

## 2.15.0

* Update to closure compiler version `v20200224`
* Add test for `assumeFunctionWrapper`

## 2.14.0

* Update to closure compiler version `v20200101`

## 2.13.0

* Update to closure compiler version `v20191111`

## 2.12.0

* Update to closure compiler version `v20190929`

## 2.11.0

* Update to closure compiler version `v20190909`

## 2.10.0

* Update to closure compiler version `v20190819`

## 2.9.0

* Update to closure compiler version `v20190729`
* Add a new option `skipMode` with the options `NEWER` (default) and `EXISTS`. Even outside of Eclipse incremental builds, this plugin
  now skips an execution if the output files exist (`skipMode=EXISTS`) and are younger than the input files (`skipMode=NEWER`). In case you
  do not want to skip an execution no matter what, set the option `force` to `true`. See also #51

## 2.8.0

* Update to closure compiler version `v20190709`
* Fix #50

## 2.7.0

* Update to closure compiler version `v20190618`
* Add option `skip`, see ticket #49

## 2.6.0

* Update to closure compiler version `v20190528`.
* Update some maven dependencies.
* Add a few more examples / tests.

## 2.5.0

* Add new option `closureJsModuleRoot` (see #48).
* When passing source files to closure compiler, their file names are now relative to the specified source directory (`srcDir`). When the source
directory is set to the npm directory with the `node_modules` folder, closure compiler is now able to resolve installed node modules.
* Add a new option `closureSourceMapLocationMappings`. When not set, the references to the source files in the source map are relative
to their locations on the file system (as in previous versions)
* Update to closure compiler `v20190415`

## 2.4.0

* Update to closure compiler `v20190325`
* Ticket #47: Preserve relative file names when not merging the files
* The option `outputFilename` now allows for the additional variable `#{path}`: When not
  merging the input files, this is the relative path from the source directory to the source
  file. The default for this options is now `#{path}/#{basename}.min.#{extension}`
  For example: Assume the source dir is `/my/dir` and the target dir is `/my/target`. Also assume
  that you want to minify the two files `/my/dir/a/file1.js` and `/my/dir/b/file2.js`, without merging them.
  Then by default, the minified files are written to `/my/target/a/file1.min.js` and `/my/target/b/file1.min.js`.
  Previously, they would have been written to `/my/target/file1.min.js` and `/my/target/file2.min.js`.

## 2.3.0

* Update to closure compiler `v20190301`
* Default language for `closureLanguageIn` is now `ECMASCRIPT_2018`
* Add test for `closureSourceMapOutputType`

## 2.2.0

* Update to closure compiler `v20190215`
* Fix issue #46 for good

## 2.1.1

* Fix issue #46
* Update some maven dependencies (Apache Collections)
* Remove a stray `System.out.println`

## 2.1.0

* Update closure-compiler to latest version (v20181210)
* Removed obsolete option `nosuffix`. The option `outputFilename` lets you now specify a pattern for the filename.
* Removed obsolete option `jsSourceFiles` (alias `jsFiles`). Files are now merged in 
  the order as specified by `includes`.
* Renamed aggregation JSON option `files` to `includes`.
* Renamed option `jsSourceDir` to `sourceDir` and `jsTargetDir` to `targetDir`.
* Renamed option `webappSourceDir` to `baseSourceDir` and `webappTargetDir` to `baseTargetDir`.
* Renamed option `jsSourceExcludes` (alias `jsExcludes`) to `excludes` and `jsSourceIncludes` 
  (alias `jsIncludes`) to `includes`.
* Renamed option `closureSortDependencies` to `closureDependencySorting`.
* Renamed option `closureDefine` to closureDefineReplacements`.
* Replaced option `suffix` with `outputFilename`. This allows for more freedom in defining 
  the output file name.
* Replaced option `verbose` with `logLevel`. This lets you override the default maven 
  log level for this plugin.
* Added an option `force` to skip the check for changed files.
* Added an option `lineSeparator`. Defaults to the system line separator.
* Added an option `sourceMapName` to customize how the source map is named.
* Added an option `outputWrapper`. This is the same as `output_wrapper` from closure compiler,
  but implemented in the plugin as the closure compiler API does not expose this option.
* Added an option `closureSourceMapOuptutType`. Set it to `inline` to include the source map 
  in the minified file. Together with the newly supported `closureIncludeSourcesContent` 
  option this allows for standalone source map that always just work!
* Added new options supported by closure compiler.
* Values for the option `closureDefineReplacements` are now like JavaScript literals. This 
  means that strings support escape sequences and should be quoted; and that binary/octal 
  number literals are supported.
* The `closureMapToOriginalSourceFiles` (see https://github.com/samaxes/minify-maven-plugin/pull/97) 
  is now the default for merge+minify.
* When both `skipMerge` and `skipMinify` are set to `true`, the entire plugin was skipped 
  previously. Now files are copied to their destination without any processing.
* When multple `jsSourceIncludes` are specified, preserve their order. File matched by a single
  `jsSourceIncludes` (when using wildcards) are sorted by name.
* When merging files, a line separator is added between each file. A source file may end 
  with a comment `// ...`, and without a new line the resulting merged file is not 
  syntactically valid.
* Remove YUICompressor. It is dead. Also remove CSS minification and concentrate on 
  one task: closure compiler.
* Adopt [Semantic Versioning](http://semver.org/) scheme.
* Remove @deprecated options.
* Support incremental builds (m2e).
* Added aggregation JSON option to `excludes`.

## 1.7.6

* `<skipMerge>true</skipMerge>` overwrites same file multiple times (#130, #131, #132).

## 1.7.5

* Add support for external bundle configuration (#57).
* Replace `prerequisites` with Maven Enforcer plugin.
* Remove the deprecated oss-parent from `pom.xml` (See [OSSRH Apache Maven Guide](http://central.sonatype.org/pages/apache-maven.html)).
* Change suffix handling to permit alternative naming schemes (#68).
* Add support for Google Closure Compiler's warning levels (#74).
* Use Google Closure Compiler lightweight message formatter for printing compiler errors (#109, #110).
* Clearer error message when target directory creation fails (#42, #87, #116).
* Add configuration for Google Closure Compiler extra annotations (#83, #117).
* Update Google Closure Compiler to v20161024 (#119, #121).
* Minified file should be in the same directory as the merged file (#73).
* Add ES6 support (#128).
* Update YUI Compressor to 2.4.8 (#111).
* Enable colorized error output for Google Closure Compiler.
* Add support for @define replacements (#127).

## 1.7.4

* Update Google Closure Compiler to v20140814 (#71).
* Add support for Closure Library dependencies sorting (#70).
* Add option to use default externs provided with the Google Closure Compiler (#67).

## 1.7.3

* Improve docs: missing information about file order (#45).
* Select the JVM default charset as the value for the `charset` option when none is defined (#48).
* Add a warning message to `nosuffix` option Javadoc (#50).
* Add support for JavaScript Source Maps (#41).
* Update Google Closure Compiler to v20140625.
* Add support for Google Closure Compiler `angular_pass` option (#60).
* Rename `yuiLinebreak` option to `yuiLineBreak`.
* Rename `yuiMunge` option to `yuiNoMunge`.
* Rename `yuiPreserveAllSemiColons` option to `yuiPreserveSemicolons`.
* Fail build when a specified source file is not found (#53).

## 1.7.2

* Update default `charset` value to `${project.build.sourceEncoding}`.
* Deprecate the option `debug`. `verbose` should be used instead.
* Change YUI option's names to clearly indicate that they are specific to YUI Compressor.
* Update Google Closure Compiler to v20130823.
* Add support for Google Closure Compiler `language` option (#24).
* Add support for Google Closure Compiler `compilation_level` option.
* Add support for Google Closure Compiler `externs` option (#22).
* Fail build with Google Closure Compiler on parse errors.

## 1.7.1

* Update Google Closure Compiler to v20130722.
* Preserve sub-directory structure when only minifying (#29).
* Delete transient `.tmp` file on spot in case of `nosuffix = true` (#32).
* Use annotations to generate the plugin descriptor file.

## 1.7

* Add `nosuffix` option to avoid the suffix `.min` on the minified output file name (#16).
* Option to use same subdirectory on target as in source (#17).
* Build should fail if compiler can't parse/compile source files (#19).
* Add `UTF-8` as the default charset.
* Log compression gains.
* Require Java SE 7 for better resource management. See [AutoCloseable](http://docs.oracle.com/javase/7/docs/api/java/lang/AutoCloseable.html) interface and [try-with-resources](http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) statements.

## 1.6.2

* Add `nosuffix` option to avoid the suffix `.min` on the minified output file name (Cherry picked from 31fe5c91bf2d24c29251595206c3c4ebada1c712).

## 1.6.1

* Preserve sub-directory structure when only minifying (Cherry picked from commit 924a23a373e6b9aa841af6b9e4300c670eb602aa).

## 1.6

* Add support for [Google Closure Compiler](https://developers.google.com/closure/compiler/) for JavaScript compression (#14).

## 1.5.2

* New goal parameter to log full source file paths and new FAQ entry pointing to the plugin goal parameters (#5).
* Option to skip the minify step (#11).
* Option to skip the merge step (#13).

## 1.5.1

* Cannot process the same file name of files in different directories (#2).
* CSS minification fails for base64 encoded background images (#3).

## 1.5

* Fix charset issue (#1).
* Update Maven site skin.
* Use `ExecutorService` to wait for all tasks to finish.
* Add support for CLI-based configuration and Maven 2.2.1. From [Configuring Plugin Goals in Maven 3](http://www.sonatype.com/people/2011/03/configuring-plugin-goals-in-maven-3/):

  > For many plugin parameters it is occasionally convenient to specify their values from the command line via system properties. In the past, this was limited to parameters of simple types like `String` or `Boolean`. The latest Maven release finally allows plugin users to configure collections or arrays from the command line via comma-separated strings. Take for example a plugin parameter like this:
  >
  > ```java
  > /** @Parameter(expression="${includes}") */
  > String[] includes;
  > ```
  >
  > This can be configured from the command line as follows:
  >
  > ```sh
  > mvn <goal> -Dincludes=Foo,Bar
  > ```
  >
  > Plugin authors that wish to enable CLI-based configuration of arrays/collections just need to add the `expression` tag to their parameter annotation. Note that if compatibility with older Maven versions is to be kept, the parameter type must not be an interface but a concrete collection class or an array to avoid another shortcoming in the old configurator.

## 1.4

* Move from http://code.google.com/p/maven-samaxes-plugin/ to https://github.com/samaxes/minify-maven-plugin.
* Add Maven Integration for Eclipse (M2E) lifecycle mapping metadata.
* Rename project from Maven Minify Plugin to Minify Maven Plugin:

  > Artifact Ids of the format maven-___-plugin are reserved for  
  > plugins in the Group Id org.apache.maven.plugins  
  > Please change your artifactId to the format ___-maven-plugin  
  > In the future this error will break the build.

## 1.3.5

* Lift restriction that prevented the final file name to be the same as an existing source file name.

## 1.3.4

* Update YUI Compressor to version 2.4.6.

## 1.3.3

* Add debug messages for wrong source file names and source directory paths.

## 1.3.2

* Add `cssTargetDir`, `jsTargetDir`, `suffix`, and `charset` parameters.

## 1.3.1

* Class `java.util.List` cannot be instantiated while running Maven minify goal with versions previous to 3.0.

## 1.3

* Change exclude/include patterns from a comma separated `String` to `List<String>`. Also included a custom file comparator that only compares the file name instead of the full file path.
* Update [YUI Compressor](http://yui.github.com/yuicompressor/) dependency to version 2.4.2.

## 1.2.1

* Don't crash with an `IndexOutOfBoundsException` when a source file does not exist.
* More accurate logging.
* Configure POM to inherit from Sonatype OSS Parent POM.

## 1.2

* Add exclude/include patterns, with the caveat that the developer must name their source files so their lexicographical order is correct for minifying.
* Don't minify a file type if the list of files to process is empty.
* Make JavaScript minify error messages clearer.
* Make file extensions configurable (e.g. it's now possible to save a JavaScript file as `*.jsp` or `*.php`).
* Compile against JDK 1.5 instead of JDK 1.6.
