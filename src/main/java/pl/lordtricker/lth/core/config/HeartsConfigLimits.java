package pl.lordtricker.lth.core.config;

public final class HeartsConfigLimits {
    public static final int OFFSET_MIN = -40;
    public static final int OFFSET_MAX = 40;
    public static final int DISTANCE_MIN = 0;
    public static final int DISTANCE_MAX = 16;
    public static final int COMBAT_MEMORY_SECONDS_MIN = 5;
    public static final int COMBAT_MEMORY_SECONDS_MAX = 120;
    public static final int MAX_VISIBLE_PLAYERS_MIN = 5;
    public static final int MAX_VISIBLE_PLAYERS_MAX = 60;

    private HeartsConfigLimits() {}

    public static int clampOffset(int value) {
        if (value < OFFSET_MIN) {
            return OFFSET_MIN;
        }
        if (value > OFFSET_MAX) {
            return OFFSET_MAX;
        }
        return value;
    }

    public static int clampDistance(int value) {
        if (value < DISTANCE_MIN) {
            return DISTANCE_MIN;
        }
        if (value > DISTANCE_MAX) {
            return DISTANCE_MAX;
        }
        return value;
    }

    public static int clampCombatMemorySeconds(int value) {
        if (value < COMBAT_MEMORY_SECONDS_MIN) {
            return COMBAT_MEMORY_SECONDS_MIN;
        }
        if (value > COMBAT_MEMORY_SECONDS_MAX) {
            return COMBAT_MEMORY_SECONDS_MAX;
        }
        return value;
    }

    public static int clampMaxVisiblePlayers(int value) {
        if (value < MAX_VISIBLE_PLAYERS_MIN) {
            return MAX_VISIBLE_PLAYERS_MIN;
        }
        if (value > MAX_VISIBLE_PLAYERS_MAX) {
            return MAX_VISIBLE_PLAYERS_MAX;
        }
        return value;
    }

    public static double normalizeOffset(int value) {
        int clamped = clampOffset(value);
        return (clamped - OFFSET_MIN) / (double) (OFFSET_MAX - OFFSET_MIN);
    }

    public static double normalizeDistance(int value) {
        int clamped = clampDistance(value);
        return (clamped - DISTANCE_MIN) / (double) (DISTANCE_MAX - DISTANCE_MIN);
    }

    public static double normalizeCombatMemorySeconds(int value) {
        int clamped = clampCombatMemorySeconds(value);
        return (clamped - COMBAT_MEMORY_SECONDS_MIN)
                / (double) (COMBAT_MEMORY_SECONDS_MAX - COMBAT_MEMORY_SECONDS_MIN);
    }

    public static int denormalizeOffset(double value) {
        int raw = (int) Math.round(OFFSET_MIN + value * (OFFSET_MAX - OFFSET_MIN));
        return clampOffset(raw);
    }

    public static int denormalizeDistance(double value) {
        int raw = (int) Math.round(DISTANCE_MIN + value * (DISTANCE_MAX - DISTANCE_MIN));
        return clampDistance(raw);
    }

    public static int denormalizeCombatMemorySeconds(double value) {
        int raw = (int) Math.round(COMBAT_MEMORY_SECONDS_MIN
                + value * (COMBAT_MEMORY_SECONDS_MAX - COMBAT_MEMORY_SECONDS_MIN));
        return clampCombatMemorySeconds(raw);
    }
}
