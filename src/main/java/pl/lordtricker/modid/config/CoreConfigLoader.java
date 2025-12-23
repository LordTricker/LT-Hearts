package pl.lordtricker.modid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CoreConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MAIN_CONFIG_FILE_NAME = "modid-config.json";

    private CoreConfigLoader() {}

    public static ModConfig loadConfig(Path modConfigDir) {
        Path configDir = ensureConfigDir(modConfigDir);
        Path configFile = configDir.resolve(MAIN_CONFIG_FILE_NAME);
        if (!Files.exists(configFile)) {
            ModConfig defaultConfig = createDefaultConfig();
            saveConfig(defaultConfig, configDir);
            return defaultConfig;
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            ModConfig loadedConfig = GSON.fromJson(reader, ModConfig.class);
            return loadedConfig == null ? new ModConfig() : loadedConfig;
        } catch (IOException e) {
            e.printStackTrace();
            return new ModConfig();
        }
    }

    public static void saveConfig(ModConfig config, Path modConfigDir) {
        Path configDir = ensureConfigDir(modConfigDir);
        Path configFile = configDir.resolve(MAIN_CONFIG_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path ensureConfigDir(Path modConfigDir) {
        if (modConfigDir == null) {
            throw new IllegalArgumentException("modConfigDir is null");
        }
        try {
            if (!Files.exists(modConfigDir)) {
                Files.createDirectories(modConfigDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return modConfigDir;
    }

    private static ModConfig createDefaultConfig() {
        return new ModConfig();
    }
}
