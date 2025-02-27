# Closure Compiler Maven Plugin

Forked from [Minify Maven Plugin](http://samaxes.github.io/minify-maven-plugin/). That project seems to be inactive. In line with the principle of single
responsibility, this fork is meant only for processing JavaScript files. The [YUI Compressor](http://yui.github.com/yuicompressor/) is dead, so what remains
is a maven plugin for [Google Closure Compiler](https://developers.google.com/closure/compiler/). I found some plugins for the closure compiler, but found
them all to be lacking - by not having a recent version of closure compile, not exposing many of its options or not handling files well. So I decided to fork the excellent Minify Maven Plugin as a base for a closure compiler maven plugin.

This plugin combines and minimizes JavaScript files. It produces a merged and a minified version.

Requires at least Java 11.

# Notes regarding changes and updates

* Closure compiler is pretty stable right now and rarely adds completely new features. It seems to concentrate on stability and bug fixes. I'll update closure compiler every few months. If you need an update immediately for a particular bug fix, feel free to open an issue.
* There are many low-level options in closure compiler, most of which are not exposed by this plugin, as I do not have any use case. If you are missing an option, also feel free to open an issue.

# Usage

Configure your project's `pom.xml` to run the plugin during the project's build cycle.

```xml
<build>
  
  <!-- Exclude the sources in "src/main/resources/includes" -->
  <!-- Include the transpiled files in "target/generated-resources/includes" -->
  <resources>
    <resource>
      <directory>${project.basedir}/src/main/resources</directory>
      <excludes>
        <exclude>includes/**/*.js</exclude>
      </excludes>
      </resource>
      <resource>
        <directory>${project.basedir}/target/generated-resources</directory>
      </resource>
  </resources>
  
  <!-- Transpiled all sources from               -->
  <!--     "src/main/resources/includes"         -->
  <!--  to                                       -->
  <!--     "target/generated-resources/includes" -->
  <plugins>
    <plugin>
      <groupId>com.github.blutorange</groupId>
      <artifactId>closure-compiler-maven-plugin</artifactId>
      <version>${closure-compiler-maven-plugin.version}</version>
      <configuration>
        <!-- Base configuration for all executions (bundles) -->
        <baseSourceDir>${project.basedir}/src/main/resources</baseSourceDir>
        <baseTargetDir>${project.build.directory}/generated-resources</baseTargetDir>
      </configuration>
      <executions>
        <!-- Process all files in the "includes" directory individually-->
        <execution>
          <id>default-minify</id>
          <configuration>
            <encoding>UTF-8</encoding>
            <sourceDir>includes</sourceDir>
            <targetDir>includes</targetDir>
            <includes>
              <include>**/*.js</include>
            </includes>
            <skipMerge>true</skipMerge>
            <closureLanguageOut>ECMASCRIPT5</closureLanguageOut>
          </configuration>
          <goals>
            <goal>minify</goal>
          </goals>
          <phase>generate-resources</phase>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

# Documentation

For more information, check the [documentation](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html)
or the [test projects](https://github.com/blutorange/closure-compiler-maven-plugin/tree/master/src/test/resources/projects).

# Paths, directories and files

To process our files, we need to know where they are located and where we want the output to go to. This sound simple, but it gets more complicated as you also want files to be ordered the right way, use wild cards, have relative paths, create source maps etc. This is short explanation of how file handling works with this plugin.

Any transpilation process consists of two main ingredients: The source (or input) files; and the target (or output) files. First we need to establish the base directory, which needs to be an absolute path on the file system:

* [baseSourceDir](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#baseSourceDir): The **absolute path** of the direcotry with the input files. Usually the source file of our project, eg. `${project.basedir}/src/webapp`
* [baseTargetDir](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#baseTargetDir): The **absolute path** of the directory with the output files. Usually the target directory of our maven project, eg. `${project.basedir}/target/generated-resources`

For larger projects, we may want to run closure compiler multiple times on different sets of files. We can do this with multiple `execution`s of this plugin. But as we are still working within the same project, we don't want to set the absolute path to our project each and every time. For each execution, we only want to specify a relative path:

* [sourceDir](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#sourceDir): The path to the directory with the source files, relative to the `baseSourceDir`.
* [targetDir](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#targetDir): The path to the directory with the source files, **relative** to the `baseTargetDir`.

Next, we want to specify some actual files to process:

* [includes](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#includes): List of files to include. This is **relative** to the `sourceDir`. Wildcards are allowed. Eg. `**/*.js`
* [excludes](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#excludes): List of files to exclude. This is **relative** to the `sourceDir`. Wildcards are allowed. Eg. `do_not_process.js`

So how are these source files ordered? We could take all includes and excludes, figure out all matching files, and sort them alphabetically. But this means we could not specify the order of files manually if we ever needed it. On the other hand, when we use wild cards to specify a set of files, we probably want these to be sorted. To get the best of both world, this plugin orders files like this:

* For each `<include>`, find all files matching the wildcard pattern (observing the `<excludes>`).
* For each `<include>`, sort the matching files alphabetically.
* Then add the matching files for each include in the order the `<include>`s were specified.

Another thing to mention here is how the files are passed to closure compiler. Closure compiler never reads files from the files system itself. When ES modules are used, it does not look in the file system for the imported files. It expects that we give it all the inpt files it needs. To pass a source file to closure compiler, we need to give it the content of the file, as well as a (possibly relative) path to the file. If we pass the wrong path, closure compiler may not be able to resolve reference between files. This plugin always uses the path of a source file on the file system, **relative to the sourceDir**.
So if we set the `baseSourceDir` to `/home/john/git/project/src/webapp` and `sourceDir` to `js`; and have a file at `/home/john/git/project/src/webapp/js/logic/model.js`: the file is passed to closure compiler with the file name `logic/model.js` By doing it this way, we also get closure compiler to "just work" with node.js projects: We just need to set the source directory to the main directory of the node project (with the `node_modules` folder) and set [closureModuleResolution](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#closureModuleResolution) to `NODE`.

Finally, we want to tell the plugin where to place the output file(s). There are two cases here, depending on whether we want to merge all input files into once large file; or just process each file separately and place these files into the target directory: 

* [skipMerge](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#skipMerge): If `true`, process each file individually. Otherwise, merge all source files.
* [outputFilename](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#outputFilename): The location of the output file, **relative** to the `targetDir`. When we merge all files, we usually want to specify an fixed name, eg. `bundle.min.js`. When we skip the merge process, we can use variables to customize the directory and file name. The default here is `#{path}/#{basename}.min.#{extension}`, which uses the original filename with a `.min` before the extension, and preserves the directory structure of the input files. See the linked documentation for more details.

As a bonus, we may sometimes want to create a source map as well. The easiest and quickest way to get working source maps is to:

* set [closureSourceMapOutputType](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#closureSourceMapOutputType) to `inline` and
* set [closureIncludeSourcesContent](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#closureIncludeSourcesContent) to `true`

This includes the entire source map as well as the original source file content in the minified file itself. As soon as the browser loads the minfied file, it's got everything it needs and the source map feature just works. Now in case we do not like our source maps being that large, we need to keep the source map as a separate file. That involves several paths:

* The path from the minified file to the source map (so the browser can find it)
* The path from the source map to the minified file (which is part of the source map)
* The path from the source map to the original source files (so the browser can find them)

Before we can worry about that, we need to specify where to put the generated source map:

* [closureSourceMapName](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#closureSourceMapName): Path and file name of the source map, **relative**  to the directory of the `outputFilename`. 

Now we can worry about the paths mentioned above. The first two are easy: we know the location of the minified file and the source map files, so we just use the corresponding relative paths. And normally, both the minified file and the source map are put inside the same directory.

The last one - the path from the source map to the original source files - is not quite as easy. The source map is generated by closure compiler and by default, closure compiler just uses the name of the source file as it was passed to it: that is, relative to the `sourceDir` This usually won't work, because the source map is placed in the `targetDir`, so the relative path won't be correct. Fortunately, closure compiler offer an option to remap the location of (path to the) source files when it creates the source map. By default, this plugin sets this option so that the paths are correct with respect to the underlying file system. So for example, if we set

* the source directory to `/home/john/git/project/src/webapp`
* the target directory to `/home/john/git/project/target/generated-sources`
* the includes to `js/index.js`
* the output file name to `bundle.min.js`
* the source map file name to `bundle.min.map.js`

Then we get the two output files 

* `/home/john/git/project/target/generated-sources/bundle.min.js` and
* `/home/john/git/project/target/generated-sources/bundle.min.map.js`

The source map `bundle.min.map.js` now references the source files as `../../src/webapp/js/index.js`. When your project directory structure resembles your directory structure on the server, then by default, every will just work. If the directory structure is diffrent, [closure compiler offers the option source_map_location_mapping](https://github.com/google/closure-compiler/wiki/Flags-and-Options). For this plugin, this is set with the option:

* [closureSourceMapLocationMappings](https://blutorange.github.io/closure-compiler-maven-plugin/minify-mojo.html#closureSourceMapLocationMappings): When the file name of a source file contains the given prefix, it is replaced with the specified replacement. Here the file name is as it was passed to closure compiler, i.e. **relative** to the `sourceDir`

For the example above, this means that the source file name would be `js/index.js`. We could now set this option to replace `js/` with `https://example.com/sources/`. Now the source map contains a reference to the source file as `https://example.com/sources/index.js`.

# Build site

* Edit files in `/src/site`
* `./mvnw clean site`
    * You can check out the locally rendered site in `target/site/index.html`.
* To upload to github, add the profile `site`
    * `./mvnw clean report:report site -P site`

# Release

* `./mvnw versions:display-dependency-updates`
* Update version in `pom.xml` and `src/test/resources/projects/parent/pom.xml`.
* Update CHANGELOG.md
* Generate site, check links
* Upload site to github (see above)
* Upload source to github
* `./mvnw clean install`
* `./mvnw -P release deploy`

# Test

The test projects need a built version of the plugin, so make a full local build first:

```sh
./mvnw clean install -DskipTests
```

You may need to run `install` on a test project first to download the required dependencies:

```sh
cd src/test/resources/projects/minimal/
./mvnw install
cd ../../../../../
```

Now test away

```sh
./mvnw clean package test
```

To run only a single test for debugging, use 

```sh
# nameOfTestMethod is one of the methods annotated with @Test in MinifyMojoTest
# For example: testOutputFilename
./mvnw test -Dtest=MinifyMojoTest#nameOfTestMethod
```

To add a new test, go to `src/test/resources/projects/`, copy one of the test projects as a base (except `parent`). Open the pom.xml
and change the `artifactId` to a new name. Edit the closure compiler configuration as necessary. Add input JavaScript files to
the directory `test` and the expected output files to the directory `expected`. Finally open `MinifyMojoTest` and add a new test method:

```java
  @Test
  public void testMyproject() throws Exception {
    runMinify("myproject");
  }
```

# License

This distribution is licensed under the terms of the Apache License, Version 2.0 (see LICENSE.txt).
