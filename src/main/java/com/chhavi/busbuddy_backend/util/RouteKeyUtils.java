package com.chhavi.busbuddy_backend.util;

public final class RouteKeyUtils {

    private RouteKeyUtils() {
    }

    /**
     * Parses a route code like "Hajipur - Patna" and returns normalized keys.
     * Returns null if the code cannot be parsed.
     */
    public static FromToKeys parseFromTo(String routeCode) {
        if (routeCode == null) {
            return null;
        }
        String trimmed = routeCode.trim();

        // Prefer explicit " - " separator.
        String[] parts;
        if (trimmed.contains(" - ")) {
            parts = trimmed.split("\\s-\\s", 2);
        } else if (trimmed.contains("-")) {
            parts = trimmed.split("-", 2);
        } else if (trimmed.toLowerCase().contains(" to ")) {
            parts = trimmed.split("(?i)\\s+to\\s+", 2);
        } else {
            return null;
        }

        if (parts.length != 2) {
            return null;
        }
        String from = SearchKeyUtils.normalize(parts[0]);
        String to = SearchKeyUtils.normalize(parts[1]);
        if (from.isBlank() || to.isBlank()) {
            return null;
        }
        return new FromToKeys(from, to);
    }

    public record FromToKeys(String fromKey, String toKey) {
    }
}
