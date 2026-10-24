package io.pebbleforge.spring.testing;

import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.utils.PathUtils;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

public final class MapLoader implements Loader<String> {

    private final Map<String, String> templates;

    public MapLoader(Map<String, String> templates) {
        this.templates = Map.copyOf(templates);
    }

    @Override
    public Reader getReader(String cacheKey) {
        String template = templates.get(cacheKey);
        if (template == null) {
            throw new LoaderException(null, "No template named [" + cacheKey + "]");
        }
        return new StringReader(template);
    }

    @Override
    public void setCharset(String charset) {
    }

    @Override
    public void setPrefix(String prefix) {
    }

    @Override
    public void setSuffix(String suffix) {
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
        return templates.containsKey(templateName);
    }
}
