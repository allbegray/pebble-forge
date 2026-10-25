# Context-aware escaping

Pebble's default autoescaping knows one context: HTML text. Pebble Forge adds two strategies and pins the three built-ins so a template can choose the right escaping for each sink.

This is the PF-201 layer. It does not infer context from the template. The template author, a component, or the Natural HTML loader chooses the strategy.

## Strategies

| Context | Strategy | What it does |
| --- | --- | --- |
| HTML text | `html` | Pebble built-in. Escapes `& < > " '`. |
| HTML attribute value | `html_attr` | Escapes markup characters, whitespace, `=`, backtick, NUL, and C0 controls so quoted and unquoted values cannot break out. |
| Whole URL in an HTML attribute | `url` | Allows `http`, `https`, `mailto`, and `tel`; rejects every other scheme with `#`; then applies `html_attr`. |
| Quoted JavaScript string | `js` | Pebble built-in. Escapes quotes, controls, U+2028/U+2029, and `</`. |
| CSS in `<style>` | `css` | Pebble built-in CSS identifier escaping. Escapes `<`, `/`, `>`, braces, semicolons, quotes, backslashes, and parentheses. |

`html`, `js`, and `css` are Pebble's own strategies. Pebble Forge does not replace them. `html_attr` and `url` are registered by `ContextEscaping`.

## Template usage

```twig
<p>{{ title }}</p>

<a href="{{ link | escape("url") }}"
   title="{{ title | escape("html_attr") }}">{{ body }}</a>

<script>const name = "{{ name | escape("js") }}";</script>

<style>
  .card::after { content: "{{ label | escape("css") }}"; }
</style>
```

Rules:

- Put the context escape last: `{{ value | upper | escape("html_attr") }}`.
- Do not chain two context escapes. The first returns `SafeString`; the second does nothing.
- `js` fills a quoted JavaScript string, not a JavaScript expression. Write the quotes in the template.
- `css` is for `<style>` content, not a CSS sanitizer or a `style` attribute.
- `url` is for a whole `href` or `src` value. Use `url_param` for an individual query component.
- One expression gets one context. Event handler and `style` attributes need composite escaping and are not covered.

## Java setup

`PebbleForge.engine()` registers `html_attr` and `url` automatically:

```java
PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .build();
```

A plain Pebble builder needs one call:

```java
PebbleEngine.Builder builder = new PebbleEngine.Builder()
        .loader(loader);
ContextEscaping.register(builder);
PebbleEngine engine = builder.build();
```

Registration is safe to repeat. It must happen before `build()`.

## URL policy

`url` validates the scheme on a copy that ignores leading and trailing ASCII whitespace and removes tab, LF, and CR anywhere. It allows:

- `http:`
- `https:`
- `mailto:`
- `tel:`
- relative URLs, including `/path`, `./path`, `../path`, `?query`, `#fragment`, and protocol-relative `//host/path`

Every other scheme becomes `#`, including `javascript:`, `data:`, `vbscript:`, `file:`, `blob:`, and `about:`.

The strategy does not percent-encode the URL and does not validate hosts or paths. `//evil.example` is an open-redirect risk, not an XSS risk. The template or application must decide whether that URL is allowed.

## SafeString

Pebble treats `SafeString` as trusted. A value from `raw`, `slot()`, `escape(...)`, or a custom safe function bypasses every strategy. Context escaping protects ordinary values. It does not make an HTML fragment safe for a URL, JavaScript, CSS, or an unquoted attribute.

Natural HTML rejects known `SafeString` producers in expressions. A custom function that returns `SafeString` remains the application's responsibility.

## Tests

The core module has 62 escaping tests:

- `ContextEscapingTest` for the two new algorithms.
- `ContextEscapingTemplateTest` for the real engine and exact output.
- `ContextEscapingXssTest` for 18 payload fixtures across HTML text, attribute, URL, JavaScript, and CSS contexts.
- One strict-sandbox test and one language-block test.

Run:

```shell
./gradlew :pebble-forge-core:test --tests '*ContextEscaping*'
```

## Limits

- No automatic context inference. That is the Natural HTML loader's job.
- `html_attr` leaves normal Unicode and C1 controls alone.
- `url` is scheme validation plus HTML attribute escaping, not URL canonicalization.
- `js` does not make a JavaScript expression, template literal, or regex safe.
- `css` does not validate CSS grammar or `url()` schemes.
- `SafeString` bypasses context escaping.
- `srcset`, `srcdoc`, `ping`, event handlers, and `style` attributes need dedicated contexts or rejection.
