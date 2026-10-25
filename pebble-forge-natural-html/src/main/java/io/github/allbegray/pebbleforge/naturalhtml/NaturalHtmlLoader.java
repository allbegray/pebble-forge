package io.github.allbegray.pebbleforge.naturalhtml;

import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * A delegating loader that rewrites Natural HTML expressions before Pebble parses them.
 */
public final class NaturalHtmlLoader implements Loader<String> {

    private final Loader<String> delegate;

    NaturalHtmlLoader(Loader<String> delegate) {
        this.delegate = delegate;
    }

    public Loader<String> delegate() {
        return delegate;
    }

    /**
     * Reads a template from the delegate without rewriting it.
     */
    public String readOriginal(String templateName) {
        Objects.requireNonNull(templateName, "templateName");
        return read(delegate.getReader(delegate.createCacheKey(templateName)), templateName);
    }

    @Override
    public Reader getReader(String cacheKey) {
        String original = read(delegate.getReader(cacheKey), cacheKey);
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(cacheKey, original);
        return new StringReader(analysis.generatedSource());
    }

    @Override
    public void setCharset(String charset) {
        delegate.setCharset(charset);
    }

    @Override
    public void setPrefix(String prefix) {
        delegate.setPrefix(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        delegate.setSuffix(suffix);
    }

    @Override
    public String resolveRelativePath(String relativePath, String anchorPath) {
        return delegate.resolveRelativePath(relativePath, anchorPath);
    }

    @Override
    public String createCacheKey(String templateName) {
        return delegate.createCacheKey(templateName);
    }

    @Override
    public boolean resourceExists(String templateName) {
        return delegate.resourceExists(templateName);
    }

    private static String read(Reader reader, String templateName) {
        if (reader == null) {
            throw new LoaderException(null, "No template named [" + templateName + "]");
        }
        try (Reader closeable = reader) {
            StringBuilder source = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = closeable.read(buffer)) != -1) {
                source.append(buffer, 0, read);
            }
            return source.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not read template [" + templateName + "]", e);
        }
    }
}
