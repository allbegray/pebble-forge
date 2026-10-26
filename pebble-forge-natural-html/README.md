# pebble-forge-natural-html

Browser-openable `.html` templates with context-aware Pebble escaping.

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-natural-html:0.4.0")
```

```java
ClasspathLoader base = new ClasspathLoader();
base.setPrefix("templates/");
base.setSuffix(".html");

PebbleEngine engine = PebbleForge.engine()
        .loader(NaturalHtml.loader(base))
        .build();
```

A template is an ordinary HTML file with `{{ ... }}` expressions. The loader chooses `html`, `html_attr`, or `url` from the surrounding HTML context, rewrites the expression, and lets Pebble parse the result. Insertions preserve line numbers and the original file stays openable in a browser.

The first slice supports expressions only. `{% ... %}` tags, includes, extends, components, macros, JavaScript, and CSS interpolation are rejected. `NaturalHtmlException` implements `SourceMappedError`, so `TemplateDiagnostics` can show the original line and column when given the delegate loader. See [docs/NATURAL-HTML.md](../docs/NATURAL-HTML.md) for supported and rejected contexts and the source map.

Run:

```shell
./gradlew :pebble-forge-natural-html:test
```
