package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.ArrayList;
import java.util.List;

final class SourceCoordinates {

    private SourceCoordinates() {
    }

    static int[] lineStarts(String source) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '\r') {
                if (index + 1 < source.length() && source.charAt(index + 1) == '\n') {
                    index++;
                }
                starts.add(index + 1);
            } else if (character == '\n') {
                starts.add(index + 1);
            }
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    static SourcePosition position(int[] lineStarts, int offset) {
        int low = 0;
        int high = lineStarts.length - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (lineStarts[middle] <= offset) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return new SourcePosition(offset, low + 1, offset - lineStarts[low] + 1);
    }
}
