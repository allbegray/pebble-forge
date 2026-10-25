# Natural HTML

Natural HTML is a browser-openable `.html` template dialect. Dynamic values use normal Pebble print expressions. The loader analyzes the HTML, rewrites each expression with the correct context escape, and lets Pebble parse and render the result. Pebble's parser is not replaced.

The first slice is **PF-202.1**: expressions only. Control flow, includes, extends, components, macros, JavaScript, and CSS interpolation are rejected. This keeps the context analysis per file and fail-closed.

## Dependency

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-natural-html:0.1.0-SNAPSHOT")
```

## Usage

```java
ClasspathLoader base = new ClasspathLoader();
base.setPrefix("templates/");
base.setSuffix(".html");

PebbleEngine engine = PebbleForge.engine()
        .loader(NaturalHtml.loader(base))
        .build();
```

The source file opens directly in a browser:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>{{ page.title }}</title>
</head>
<body>
  <h1>{{ page.heading }}</h1>
  <a href="{{ page.ctaUrl }}">Continue</a>
  <img src="/avatars/{{ user.id }}" alt="{{ user.name }}">
</body>
</html>
```

The loader turns it into:

```html
<title>{{ (page.title) | escape('html') }}</title>
<h1>{{ (page.heading) | escape('html') }}</h1>
<a href="{{ (page.ctaUrl) | escape('url') }}">Continue</a>
<img src="/avatars/{{ (user.id) | escape('url') }}" alt="{{ (user.name) | escape('html_attr') }}">
```

The whole expression is parenthesized, so a ternary or filter chain cannot escape only one branch. Insertions never add a newline, so Pebble errors keep the original line number.

## Supported contexts

| Context | Strategy |
| --- | --- |
| HTML text | `html` |
| `<title>` and `<textarea>` RCDATA | `html` |
| Quoted ordinary attribute value | `html_attr` |
| Quoted URL attribute (`href`, `src`, `action`, `formaction`, `poster`, `cite`, `data`, `background`, `longdesc`, `usemap`, `manifest`) | `url` |

A URL attribute can hold a whole-value expression, or a literal relative prefix starting with `/`, `./`, `../`, `#`, or `?`:

```html
<a href="/users/{{ user.id }}">Profile</a>
```

Absolute prefixes such as `https://host/{{ path }}` are rejected. Build the whole URL in the model.

Pebble trim markers are preserved:

```html
{{- name -}}  ->  {{- (name) | escape('html') -}}
```

## Rejected contexts

The loader rejects an expression in:

- `<script>`, `<style>`, HTML comments, doctypes, and CDATA.
- Tag names, attribute names, and unquoted attribute values.
- `on*`, `style`, `srcdoc`, `srcset`, `imagesrcset`, `ping`, and `archive`.
- Subtrees of `meta`, `base`, `iframe`, `object`, `embed`, `svg`, `math`, `template`, `noscript`, `xmp`, `plaintext`, `noembed`, `noframes`, `frame`, `frameset`, and `applet`.
- Expressions immediately after `<` or `</`.

Static HTML in those elements is allowed. Only interpolation is rejected.

The loader also rejects:

- Every `{% ... %}` tag and every `{# ... #}` comment.
- `raw`, explicit `escape`, `date`, `safe`, `slot(`, `block(`, and `parent(` inside an expression.
- Empty, unclosed, or unterminated-string expressions.
- A `<` followed by a letter, `/`, `!`, or `?` inside a text expression, because the browser-openable source would contain markup.

Every rejection is a `NaturalHtmlException` with `template:line:column`.

## HTML parsing and mapping

The analyzer uses jsoup 1.20.1 with position tracking to validate the source and find text, RCDATA, attribute, and forbidden regions. A separate expression scanner finds `{{ ... }}` boundaries while tracking strings, escapes, and nested braces. The scanner is not a Pebble parser; Pebble still parses the rewritten source.

`NaturalHtml.analyze(name, source)` returns:

- the original source
- the generated source
- the expression sites with context, element, attribute, and strategy
- a `SourceMap` for generated offset to original offset, line, and column

`NaturalHtmlLoader.readOriginal(name)` reads the delegate without rewriting, for diagnostics or preview.

## Tests

The module has 129 tests:

- loader and exact rewrite output
- source-map and line preservation
- rejection matrix
- XSS fixtures
- jsoup parsing of the original and rendered HTML
- temporary preview file round trip
- strict sandbox rendering

Run:

```shell
./gradlew :pebble-forge-natural-html:test
```

## Limits

- Expressions only. No `if`, `for`, `set`, `extends`, `include`, `component`, `macro`, or `import`.
- No JavaScript or CSS interpolation.
- No cross-template context propagation.
- `url` is scheme validation, not host or path validation.
- `SafeString` from a custom function or model remains trusted.
- `NaturalHtmlException` implements `SourceMappedError`. Pass the delegate loader to `TemplateDiagnostics` to show the original line and caret; passing `NaturalHtmlLoader` shows rewritten text. The message also carries line and column.
- Columns count UTF-16 code units.
- A raw Pebble engine needs `ContextEscaping.register`; `PebbleForge.engine()` already does it.
- The preview test parses the source and rendered output with jsoup. It is not a real-browser automation test.
