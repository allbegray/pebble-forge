package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.Comparator;
import java.util.List;

/**
 * Maps offsets in the generated source back to the original source.
 *
 * <p>Rewriting only inserts characters, so every original character keeps its identity. Offsets
 * inside inserted text map to the original offset where the insertion was made.
 */
public final class SourceMap {

    private final List<Insertion> insertions;
    private final int generatedLength;
    private final int[] lineStarts;

    SourceMap(String originalSource, List<Insertion> insertions, int generatedLength) {
        this.insertions = insertions.stream()
                .sorted(Comparator.comparingInt(Insertion::generatedOffset))
                .toList();
        this.generatedLength = generatedLength;
        this.lineStarts = SourceCoordinates.lineStarts(originalSource);
    }

    /**
     * Returns the original offset for a generated offset.
     *
     * @param generatedOffset the offset in the generated source
     * @return the matching offset in the original source
     */
    public int originalOffset(int generatedOffset) {
        if (generatedOffset < 0 || generatedOffset > generatedLength) {
            throw new IllegalArgumentException(
                    "generatedOffset must be between 0 and " + generatedLength);
        }
        int inserted = 0;
        for (Insertion insertion : insertions) {
            if (generatedOffset < insertion.generatedOffset()) {
                break;
            }
            if (generatedOffset < insertion.generatedOffset() + insertion.length()) {
                return insertion.originalOffset();
            }
            inserted += insertion.length();
        }
        return generatedOffset - inserted;
    }

    /**
     * Returns the one-based line and column in the original source for a generated offset.
     *
     * @param generatedOffset the offset in the generated source
     * @return the matching position in the original source
     */
    public SourcePosition originalPosition(int generatedOffset) {
        return SourceCoordinates.position(lineStarts, originalOffset(generatedOffset));
    }

    record Insertion(int generatedOffset, int originalOffset, int length) {
    }
}
