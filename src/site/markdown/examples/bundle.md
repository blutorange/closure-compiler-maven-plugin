# Bundle Configuration

**Deprecated** 

You can simply use multiple `<execution>` in the plugin configuration. If you have a use-case for bundles, consider
[opening an issue and discuss it](../issue-management.html).

---

You can optionally specify the source files to process via an external configuration file. When a `bundleConfiguration` 
is defined, it overrides `includes` of the [Basic Configuration](../examples/basic.html).

```xml
<project>
  <!-- ... -->
  <build>
    <plugins>
      <!-- ... -->
      <plugin>
        <groupId>com.github.blutorange</groupId>
        <artifactId>closure-compiler-maven-plugin</artifactId>
        <version>${closure-compiler-maven-plugin.version}</version>
        <executions>
          <execution>
            <id>bundle-minify</id>
            <configuration>
            <bundleConfiguration>src/minify/static-bundles.json</bundleConfiguration>
            </configuration>
            <goals>
              <goal>minify</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <!-- ... -->
    </plugins>
  </build>
  <!-- ... -->
</project>
```

# Configuration Format

Bundles are defined in JSON format.

```json
{
    "bundles": [
        {
            "name": "static-combined.js",
            "includes": [
                "blutorange.js",
                "subdir/pearce-kelly.js"
            ],
            "excludes": []
        }
    ]
}
```