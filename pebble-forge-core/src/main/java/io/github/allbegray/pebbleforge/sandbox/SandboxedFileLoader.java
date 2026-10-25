package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.utils.PathUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File loader that rejects symlinks escaping the configured root.
 */
public final class SandboxedFileLoader implements Loader<String> {

    private Path root;
    private Path realRoot;
    private String suffix;
    private String charset = "UTF-8";

    public SandboxedFileLoader(Path root) {
        setPrefix(root.toAbsolutePath().normalize().toString());
    }

    @Override
    public Reader getReader(String templateName) {
        Path path = resolve(templateName);
        try {
            return Files.newBufferedReader(path, Charset.forName(charset));
        } catch (IOException | RuntimeException e) {
            throw new LoaderException(e, "Could not read template [" + templateName + "]");
        }
    }

    @Override
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public void setPrefix(String prefix) {
        if (prefix == null) {
            throw new LoaderException(null, "Prefix cannot be null");
        }
        Path candidate = Paths.get(prefix);
        if (!candidate.isAbsolute()) {
            throw new LoaderException(null, "Prefix must be an absolute path");
        }
        candidate = candidate.normalize();
        if (!Files.isDirectory(candidate)) {
            throw new LoaderException(null, "Prefix must be an existing directory: " + candidate);
        }
        try {
            this.root = candidate;
            this.realRoot = candidate.toRealPath();
        } catch (IOException e) {
            throw new LoaderException(e, "Could not resolve the loader root: " + candidate);
        }
    }

    @Override
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String resolveRelativePath(String relativePath, String anchorPath) {
        return PathUtils.resolveRelativePath(relativePath, anchorPath, '/');
    }

    @Override
    public String createCacheKey(String templateName) {
        return templateName;
    }

    @Override
    public boolean resourceExists(String templateName) {
        try {
            resolve(templateName);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String getPrefix() {
        return root.toString();
    }

    private Path resolve(String templateName) {
        validateName(templateName);
        String name = templateName + (suffix == null ? "" : suffix);
        Path candidate = root.resolve(name).normalize();
        if (!candidate.startsWith(root)) {
            throw new LoaderException(null, "Template path escapes the loader root: " + templateName);
        }

        Path real;
        try {
            real = candidate.toRealPath();
        } catch (IOException e) {
            throw new LoaderException(e, "Could not resolve template [" + templateName + "]");
        }
        if (!real.startsWith(realRoot)) {
            throw new LoaderException(
                    null,
                    "Template resolves outside the loader root: " + templateName);
        }
        return real;
    }

    private static void validateName(String name) {
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
        for (String segment : name.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new LoaderException(null, "Template path traversal is not allowed: " + name);
            }
        }
    }
}
