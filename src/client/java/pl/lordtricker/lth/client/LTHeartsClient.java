package pl.lordtricker.lth.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import pl.lordtricker.lth.client.command.ClientCommandRegistration;
import pl.lordtricker.lth.client.config.ConfigLoader;
import pl.lordtricker.lth.client.render.ClientDamageTracker;
import pl.lordtricker.lth.client.render.ClientHeartsVisibility;
import pl.lordtricker.lth.client.util.ColorUtils;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.util.Messages;
import pl.lordtricker.lth.util.RemoteAdConfig;

public class LTHeartsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HeartsState.init(ConfigLoader.loadConfig());
        RemoteAdConfig.preloadAsync();
        Messages.setMissingMessageHandler(pl.lordtricker.lth.client.LTHeartsClient::sendMissingMessage);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                String welcomeMsg = Messages.get("player.join");
                client.player.sendMessage(ColorUtils.translateColorCodes(welcomeMsg), false);
            }
        });

        ClientCommandRegistration.registerCommands();
        ClientTickEvents.END_CLIENT_TICK.register(ClientDamageTracker::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientHeartsVisibility::onClientTick);
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
