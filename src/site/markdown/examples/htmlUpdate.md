# Updating HTML pages

Available since version 2.32.0.

You can update an HTML file with the path of the generated output script file. Supports HTML as well as XML and XHTML
files. To update an HTML file, use the `<htmlUpdates></htmlUpdates>` option. Below is a description of the various
options.

* For a discussion  regarding this feature, see also [issue 4](https://github.com/blutorange/closure-compiler-maven-plugin/issues/4).
* For a longer description of each option, see the JavaDocs of
[HtmlUpdate](../apidocs/com/github/blutorange/maven/plugin/closurecompiler/plugin/HtmlUpdate.html)
* For more examples, see also the 
[tests for this feature](https://github.com/blutorange/closure-compiler-maven-plugin/tree/master/src/test/resources/projects/htmlUpdate)

In order for this plugin to be able to update a script tag in an HTML file, 3 main questions need to be answered:

1. Which HTML files should be updated?
1. How to locate the script element to update?
1. How to construct the relative path from the HTML file to the script file?

## Configuring the HTML files to update

First, we need to find the HTML files that should be processed.

To find the HTML file or files to update, this plugin uses Maven's standard includes/excludes approach. By default,
all HTML files are included.

You also need to specify a base directory to scan for HTML files. Similarly to the configuration for minifying files,
this plugin offers a `baseHtmlDir` and an `htmlDir` option. This way, you can configure a default base directory for all
executions. In addition, you can also set a separate `dir` for each `htmlUpdate`.

Note that not all of these options are required. For example, you could only set `baseHtmlDir`, or only a `dir`
for each `htmlUpdate`. If multiple options are given, they are resolved relative to each other.

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.blutorange</groupId>
        <artifactId>closure-compiler-maven-plugin</artifactId>
        <version>${closure-compiler-maven-plugin.version}</version>
        <configuration>
           <!-- Defaults to src/main/resources -->
          <baseHtmlDir>${project.basedir}/web</baseHtmlDir>
        </configuration>
        <executions>
          <execution>
            <id>default-minify</id>
            <goals><goal>minify</goal></goals>
            <configuration>
              <!-- Resolves to ${project.basedir}/web/templates -->
              <htmlDir>templates</htmlDir>
              <htmlUpdates>
                <htmlUpdate>
                  <!-- Resolves to ${project.basedir}/web/templates/page1 -->
                  <dir>page1</dir>      
                </htmlUpdate>
                <htmlUpdate>
                  <!-- Resolves to ${project.basedir}/web/templates/page2 -->
                  <dir>page2</dir>
                </htmlUpdate>
              </htmlUpdates>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

The default encoding is `UTF-8`. If the HTML file uses a different encoding, use the `encoding` option:

```xml
<htmlUpdates>
  <htmlUpdate>
    <encoding>UTF16-BE</encoding>
  </htmlUpdate>
</htmlUpdates>
```

## Locating the script elements to update

Next, we need to locate the script element.

To find the script element to update, you need to specify a selector that can be used to locate the element. For
flexibility, this plugin offers 3 different types of selectors: by ID, by CSS selector, or by XPath. By default, the
first `<script>` element in the HTML file is used. If a selector matches multiple elements, all matching elements are
updated. 

For example:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.blutorange</groupId>
        <artifactId>closure-compiler-maven-plugin</artifactId>
        <version>${closure-compiler-maven-plugin.version}</version>
        <executions>
          <execution>
            <id>default-minify</id>
            <goals><goal>minify</goal></goals>
            <configuration>
              <htmlDir>templates</htmlDir>
              <htmlUpdates>
                <htmlUpdate>
                  <scripts>id:target</scripts>
                </htmlUpdate>
                <htmlUpdate>
                  <scripts>css:.my-script[type='module']</scripts>
                </htmlUpdate>
                <htmlUpdate>
                  <scripts>xpath://script[contains(text(), "code")]</scripts>
                </htmlUpdate>
              </htmlUpdates>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

By default, the `src` attribute of the matching elements are updated to the new value. For advanced use cases, you
can use the `attributes` option to update different or additional attributes:

```xml
<htmlUpdates>
  <htmlUpdate>
    <attributes>
        <attribute>src</attribute>
        <attribute>data-src</attribute>
    </attributes>
  </htmlUpdate>
</htmlUpdates>
```

## Constructing the relative path from the HTML to the script file

Finally, we need to create a relative path from the HTML file to the script file.

The easiest way to do so would be relativizing the path of the script file on the file system against the path of
the HTML file. However, in many cases, the physical location will not correspond to the final location when the files
are served by a server.

To offer enough flexibility for such use cases, this plugin lets you define the root path for HTML and script file. The
physical location of the HTML and script file is then resoled against the respective root path. Lastly, the relative
path of the script file gets relativized against the relative path of the HTML file.

For example, assume the following configuration:

* HTML file - `/home/user/project/src/main/resources/webapp/public/pages/profile/index.html`
* Script file - `/home/user/project/target/generated-resources/frontend/js/public/resources/main/script.min.js`
* HTML root - `/home/user/project/src/main/resources/webapp`
* Script root - `/home/user/project/target/generated-resources/frontend/js`

Then, the relative path of the HTML file relative to the HTML root is `public/pages/profile/index.html`
 and the relative of the JavaScript file is `public/resources/main/script.min.js`. Relativizing the script file against
the HTML file yields `../../resources/main/script.min.js`, which is used to update the `<script>` tag.

Similarly to the configuration for minifying files, this plugin offers the `baseHtmlRoot` / `baseHtmlScriptRoot` options
in addition to the `htmlRoot` and `htmlScriptRoot` options. This way, you can configure a default base directory for all
executions. In addition, you can also set a separate `root` and `scriptRoot` for each `htmlUpdate`.

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.blutorange</groupId>
        <artifactId>closure-compiler-maven-plugin</artifactId>
        <version>${closure-compiler-maven-plugin.version}</version>
        <configuration>
          <!-- Defaults to src/main/resources -->
          <baseHtmlRoot>${project.basedir}/web/html</baseHtmlRoot>
          <!-- Defaults to target/generated-resources -->
          <baseHtmlScriptRoot>${project.basedir}/web/js</baseHtmlScriptRoot>
        </configuration>
        <executions>
          <execution>
            <id>default-minify</id>
            <goals><goal>minify</goal></goals>
            <configuration>
              <!-- Resolves to ${project.basedir}/web/html/area/blog -->
              <htmlRoot>area/blog</htmlRoot>
              <!-- Resolves to ${project.basedir}/web/js/area/blog -->
              <htmlScriptRoot>area/blog</htmlScriptRoot>
              <htmlUpdates>
                <htmlUpdate>
                  <!-- Resolves to ${project.basedir}/web/html/area/blog/articles -->
                  <root>articles</root>
                  <!-- Resolves to ${project.basedir}/web/js/area/blog/articles -->
                  <scriptRoot>articles</scriptRoot>
                </htmlUpdate>
                <htmlUpdate>
                  <!-- Resolves to ${project.basedir}/web/html/area/blog/extras -->
                  <root>extras</root>
                  <!-- Resolves to ${project.basedir}/web/js/area/blog/extras -->
                  <scriptRoot>extras</scriptRoot>
                </htmlUpdate>
              </htmlUpdates>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

In case you want to relativize the files with respect to their physical location on the file system, set the
`<usePhysicalRoot>` option to true, either in the top-level plugin configuration or inside an `<htmlUpdate>` tag:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.blutorange</groupId>
        <artifactId>closure-compiler-maven-plugin</artifactId>
        <version>${closure-compiler-maven-plugin.version}</version>
        <configuration>
        </configuration>
        <executions>
          <execution>
            <id>default-minify</id>
            <goals><goal>minify</goal></goals>
            <htmlUsePhysicalRoot>true</htmlUsePhysicalRoot>
            <configuration>
              <htmlUpdates>
                <htmlUpdate>
                 <!-- Overrides the htmlUsePhysicalRoot -->
                  <usePhysicalRoot>true</usePhysicalRoot>
                </htmlUpdate>
              </htmlUpdates>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

In case the above does not suit your needs, you can also specify the source path directly via the `sourcePath` option,
either in the top-level plugin configuration or inside an `<htmlUpdate>` tag:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.blutorange</groupId>
        <artifactId>closure-compiler-maven-plugin</artifactId>
        <version>${closure-compiler-maven-plugin.version}</version>
        <configuration>
        </configuration>
        <executions>
          <execution>
            <id>default-minify</id>
            <goals><goal>minify</goal></goals>
            <htmlSourcePath>../../#{basename}.#{extension}</htmlSourcePath>
            <configuration>
              <htmlUpdates>
                <htmlUpdate>
                  <!-- Overrides the htmlSourcePath -->
                  <sourcePath>../../#{basename}.#{extension}</sourcePath>
                </htmlUpdate>
              </htmlUpdates>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```