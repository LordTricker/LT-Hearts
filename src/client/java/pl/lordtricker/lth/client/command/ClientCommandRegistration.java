package pl.lordtricker.lth.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import pl.lordtricker.lth.client.gui.MainSettingsScreen;
import pl.lordtricker.lth.client.config.ConfigLoader;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.util.Messages;

public class ClientCommandRegistration {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistration::registerModCommand);
    }

    private static void registerModCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess
    ) {
        dispatcher.register(
                ClientCommandManager.literal("lth")
                        .executes(ctx -> {
                            String message = Messages.get("mod.info");
                            ctx.getSource().sendFeedback(CommandUi.colored(message));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("settings")
                                .executes(ctx -> {
                                    MinecraftClient client = MinecraftClient.getInstance();
                                    client.setScreen(null);
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(100);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        client.execute(() -> client.setScreen(new MainSettingsScreen()));
                                    }).start();
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("pomoc")
                                .executes(ctx -> {
                                    String msg = Messages.get("command.help");
                                    ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("config")
                                .then(ClientCommandManager.literal("save")
                                        .executes(ctx -> {
                                            ConfigLoader.saveConfig(HeartsState.getConfig());
                                            String msg = Messages.get("command.config.save.success");
                                            ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("reload")
                                        .executes(ctx -> {
                                            HeartsState.init(ConfigLoader.loadConfig());
                                            String msg = Messages.get("command.config.reload.success");
                                            ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                            return 1;
                                        })
                                )
                        )
        );
    }
}
