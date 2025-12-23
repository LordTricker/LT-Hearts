package pl.lordtricker.modid.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Messages {
    private static final Map<String, List<String>> MESSAGES = new HashMap<>();
    private static MissingMessageHandler missingMessageHandler = MissingMessageHandler.stderr();

    static {
        loadMessages();
    }

    public static void init() {
        // Intentionally empty; forces class loading for static init.
    }

    public static void setMissingMessageHandler(MissingMessageHandler handler) {
        missingMessageHandler = (handler != null) ? handler : MissingMessageHandler.stderr();
    }

    public static String get(String key) {
        if (!MESSAGES.containsKey(key)) {
            String missing = "Missing message for key: " + key;
            missingMessageHandler.handle(missing);
            return missing;
        }
        List<String> lines = MESSAGES.get(key);
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        return String.join("\n", lines);
    }

    public static String format(String key, Map<String, String> placeholders) {
        String message = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }

    private static void loadMessages() {
        try (InputStream in = Messages.class.getResourceAsStream("/assets/modid/messages/messages.json")) {
            if (in == null) {
                System.err.println("messages.json not found in resources.");
            } else {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                Map<String, List<String>> loaded = new Gson()
                        .fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
                if (loaded != null) {
                    for (Map.Entry<String, List<String>> entry : loaded.entrySet()) {
                        if (entry.getValue() == null) {
                            entry.setValue(Collections.singletonList(
                                    "Missing message for key: " + entry.getKey()));
                        }
                    }
                    MESSAGES.putAll(loaded);
                    System.out.println("Loaded message keys: " + MESSAGES.keySet());
                } else {
                    System.err.println("messages.json parsed to null.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface MissingMessageHandler {
        void handle(String message);

        static MissingMessageHandler stderr() {
            return System.err::println;
        }
    }
}
