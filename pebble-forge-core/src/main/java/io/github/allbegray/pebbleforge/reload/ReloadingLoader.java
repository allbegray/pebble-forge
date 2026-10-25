package io.github.allbegray.pebbleforge.reload;

import io.pebbletemplates.pebble.loader.Loader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Adds a content hash to the loader cache key so template edits are picked up
 * without rebuilding the engine.
 *
 * @param <T> the cache key type of the wrapped loader
 */
public final class ReloadingLoader<T> implements Loader<ReloadingLoader.CacheKey> {

    private final Loader<T> delegate;

    private ReloadingLoader(Loader<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Wraps a loader so its cache keys include a content hash.
     *
     * @param delegate the loader to wrap
     * @param <T> the cache key type of the wrapped loader
     * @return the wrapped loader
     */
    public static <T> ReloadingLoader<T> wrap(Loader<T> delegate) {
        return new ReloadingLoader<>(delegate);
    }

    /**
     * Returns the wrapped loader.
     *
     * @return the wrapped loader
     */
    public Loader<T> delegate() {
        return delegate;
    }

    @Override
    public Reader getReader(CacheKey cacheKey) {
        return delegate.getReader(delegate.createCacheKey(cacheKey.templateName()));
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
    public CacheKey createCacheKey(String templateName) {
        return new CacheKey(templateName, contentHash(templateName));
    }

    @Override
    public boolean resourceExists(String templateName) {
        return delegate.resourceExists(templateName);
    }

    private String contentHash(String templateName) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }

        try (Reader reader = delegate.getReader(delegate.createCacheKey(templateName))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                digest.update(new String(buffer, 0, read).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (RuntimeException | IOException e) {
            return "missing";
        }
    }

    /**
     * A loader cache key that changes when the template content changes.
     *
     * @param templateName the template name
     * @param version the content hash
     */
    public record CacheKey(String templateName, String version) {
    }
}
