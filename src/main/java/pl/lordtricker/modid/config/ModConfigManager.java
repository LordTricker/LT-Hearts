package pl.lordtricker.modid.config;

public class ModConfigManager {
    public static boolean adsEnabled = false;
    public static boolean attackDelayTutorEnabled = false;
    public static boolean attackDelayTutorSoundEnabled = true;
    public static boolean attackDelayTutorTextEnabled = true;

    public static ModConfig toConfig() {
        ModConfig config = new ModConfig();
        config.adsEnabled = adsEnabled;
        config.attackDelayTutorEnabled = attackDelayTutorEnabled;
        config.attackDelayTutorSoundEnabled = attackDelayTutorSoundEnabled;
        config.attackDelayTutorTextEnabled = attackDelayTutorTextEnabled;
        return config;
    }

    public static void applyFrom(ModConfig config) {
        adsEnabled = config.adsEnabled;
        attackDelayTutorEnabled = config.attackDelayTutorEnabled;
        attackDelayTutorSoundEnabled = config.attackDelayTutorSoundEnabled;
        attackDelayTutorTextEnabled = config.attackDelayTutorTextEnabled;
    }
}
