package pl.lordtricker.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import pl.lordtricker.modid.client.command.ClientCommandRegistration;
import pl.lordtricker.modid.client.config.ConfigLoader;
import pl.lordtricker.modid.client.util.ColorUtils;
import pl.lordtricker.modid.config.ModConfigManager;
import pl.lordtricker.modid.util.Messages;
import pl.lordtricker.modid.util.RemoteAdConfig;

public class ModTemplateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfigManager.applyFrom(ConfigLoader.loadConfig());
        RemoteAdConfig.preloadAsync();
        Messages.setMissingMessageHandler(ModTemplateClient::sendMissingMessage);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                String welcomeMsg = Messages.get("player.join");
                client.player.sendMessage(ColorUtils.translateColorCodes(welcomeMsg), false);
            }
        });

        ClientCommandRegistration.registerCommands();
    }

    private static void sendMissingMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        } else {
            System.err.println(message);
        }
    }
}
