package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;

import java.io.Reader;

/**
 * Rejects template names that escape the loader root.
 */
public final class SandboxedLoader<T> implements Loader<T> {

    private final Loader<T> delegate;
    private final SandboxPolicy policy;

    public SandboxedLoader(Loader<T> delegate, SandboxPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    @Override
    public Reader getReader(T cacheKey) {
        if (cacheKey instanceof String name) {
            validate(name);
        }
        return delegate.getReader(cacheKey);
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
        validate(relativePath);
        if (anchorPath != null) {
            validate(anchorPath);
        }
        String resolved = delegate.resolveRelativePath(relativePath, anchorPath);
        if (resolved != null) {
            validate(resolved);
        }
        return resolved;
    }

    @Override
    public T createCacheKey(String templateName) {
        validate(templateName);
        return delegate.createCacheKey(templateName);
    }

    @Override
    public boolean resourceExists(String templateName) {
        validate(templateName);
        return delegate.resourceExists(templateName);
    }

    private void validate(String name) {
        if (name == null || name.isBlank()) {
            throw new LoaderException(null, "Template name must not be empty");
        }
        if (name.indexOf('\\') >= 0) {
            throw new LoaderException(null, "Template name must use '/' separators: " + name);
        }
        if (name.indexOf('\0') >= 0) {
            throw new LoaderException(null, "Template name contains a null byte");
        }
        if (name.startsWith("/") || name.matches("^[A-Za-z]:.*") || name.contains(":")) {
            throw new LoaderException(null, "Absolute template paths are not allowed: " + name);
        }
        String[] segments = name.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new LoaderException(null, "Template path traversal is not allowed: " + name);
            }
        }
        if (!policy.templateNameValidator().test(name)) {
            throw new LoaderException(null, "Template name rejected by policy: " + name);
        }
    }
}
