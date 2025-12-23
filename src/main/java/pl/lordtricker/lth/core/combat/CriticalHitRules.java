package pl.lordtricker.lth.core.combat;

public final class CriticalHitRules {
    private CriticalHitRules() {}

    public static boolean isCritical(float fallDistance,
                                     boolean onGround,
                                     boolean climbing,
                                     boolean touchingWater,
                                     boolean hasBlindness,
                                     boolean hasVehicle,
                                     boolean sprinting) {
        return fallDistance > 0.0f
                && !onGround
                && !climbing
                && !touchingWater
                && !hasBlindness
                && !hasVehicle
                && !sprinting;
    }
}
