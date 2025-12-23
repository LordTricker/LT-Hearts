package pl.lordtricker.lth.core;

import pl.lordtricker.lth.core.config.HeartsSettings;
import pl.lordtricker.lth.core.config.ServersConfig;

public final class HeartsState {
    private static ServersConfig config = new ServersConfig();

    private HeartsState() {}

    public static void init(ServersConfig cfg) {
        if (cfg == null) {
            config = new ServersConfig();
            return;
        }
        if (cfg.heartsSettings == null) {
            cfg.heartsSettings = new HeartsSettings();
        }
        config = cfg;
    }

    public static ServersConfig getConfig() {
        return config;
    }

    public static HeartsSettings getSettings() {
        return config.heartsSettings;
    }
}
