package com.chhavi.busbuddy_backend.util;

import java.util.Locale;

public final class SearchKeyUtils {

    private SearchKeyUtils() {
    }

    /**
     * Normalizes a free-text name/code into a searchable key.
     *
     * Example:
     * - "Hajipur - Patna" -> "hajipur patna"
     * - "BUS-101" -> "bus 101"
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(" to ", " ")
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
