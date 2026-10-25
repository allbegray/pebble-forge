package io.github.allbegray.pebbleforge.fuzz.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class FailureMinimizer {

    private final Predicate<String> reproduces;

    public FailureMinimizer(Predicate<String> reproduces) {
        this.reproduces = Objects.requireNonNull(reproduces, "reproduces");
    }

    public Optional<String> minimize(String input) {
        Objects.requireNonNull(input, "input");
        if (!reproduces.test(input)) {
            return Optional.empty();
        }

        String current = input;
        int chunkSize = Math.max(1, current.length() / 2);
        while (!current.isEmpty() && chunkSize > 0) {
            boolean removed = false;
            for (int start = 0; start < current.length(); start += chunkSize) {
                int end = Math.min(current.length(), start + chunkSize);
                String candidate = current.substring(0, start) + current.substring(end);
                if (reproduces.test(candidate)) {
                    current = candidate;
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                chunkSize = chunkSize == 1 ? 0 : chunkSize / 2;
            }
        }
        return Optional.of(current);
    }

    public static Path writeReproducer(Path directory, String name, String input) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(name + ".txt");
        Files.writeString(file, input, StandardCharsets.UTF_8);
        return file;
    }
}
