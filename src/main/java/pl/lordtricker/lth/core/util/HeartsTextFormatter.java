package pl.lordtricker.lth.core.util;

import java.util.Locale;

public final class HeartsTextFormatter {
    private HeartsTextFormatter() {}

    public static String formatHearts(float hearts) {
        float rounded = Math.round(hearts * 2f) / 2f;
        int intValue = Math.round(rounded);
        if (Math.abs(rounded - intValue) < 0.001f) {
            return Integer.toString(intValue);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }
}
