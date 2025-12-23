package pl.lordtricker.lth.core.render;

import pl.lordtricker.lth.core.config.HeartsConfigLimits;
import pl.lordtricker.lth.core.util.HeartsTextFormatter;

public final class HeartsRenderLogic {
    private HeartsRenderLogic() {}

    public static int clampDistance(int value) {
        return HeartsConfigLimits.clampDistance(value);
    }

    public static float computeExtraYOffset(float scale, int fontHeight, int padding, boolean hasLabel, int extraYOffsetPixels) {
        float offset = 0f;
        if (hasLabel) {
            offset += (fontHeight + padding + 2) * scale;
        }
        if (extraYOffsetPixels != 0) {
            offset += extraYOffsetPixels * scale;
        }
        return offset;
    }

    public static String buildDamageLabel(float hearts) {
        return "-" + HeartsTextFormatter.formatHearts(hearts);
    }

    public static int computeFadeAlpha(int ageTicks, int maxAgeTicks) {
        if (maxAgeTicks <= 0) {
            return 0;
        }
        float progress = ageTicks / (float) maxAgeTicks;
        int alpha = Math.round(255f * (1.0f - progress));
        if (alpha < 0) {
            return 0;
        }
        if (alpha > 255) {
            return 255;
        }
        return alpha;
    }

    public static float computeDamageLineOffset(int index, int fontHeight, int padding, int ageTicks, float pixelsPerTick) {
        float baseOffset = 0f;
        float floatUp = -ageTicks * pixelsPerTick;
        return baseOffset - (index * (fontHeight + 2)) + floatUp;
    }
}
