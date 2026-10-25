package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.List;
import java.util.Optional;

record HtmlScan(List<HtmlRegion> regions) {

    Optional<HtmlRegion.Allowed> allowedAt(int offset) {
        HtmlRegion.Allowed best = null;
        for (HtmlRegion region : regions) {
            if (region instanceof HtmlRegion.Allowed allowed && contains(allowed, offset)) {
                if (best == null || span(allowed) < span(best)) {
                    best = allowed;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    Optional<HtmlRegion.Forbidden> forbiddenAt(int offset) {
        HtmlRegion.Forbidden best = null;
        for (HtmlRegion region : regions) {
            if (region instanceof HtmlRegion.Forbidden forbidden && contains(forbidden, offset)) {
                if (best == null || span(forbidden) < span(best)) {
                    best = forbidden;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    Optional<String> hintAt(int offset) {
        HtmlRegion.Hint best = null;
        for (HtmlRegion region : regions) {
            if (region instanceof HtmlRegion.Hint hint && contains(hint, offset)) {
                if (best == null || span(hint) < span(best)) {
                    best = hint;
                }
            }
        }
        return Optional.ofNullable(best).map(HtmlRegion.Hint::reason);
    }

    private static boolean contains(HtmlRegion region, int offset) {
        return offset >= region.start() && offset < region.end();
    }

    private static int span(HtmlRegion region) {
        return region.end() - region.start();
    }
}
