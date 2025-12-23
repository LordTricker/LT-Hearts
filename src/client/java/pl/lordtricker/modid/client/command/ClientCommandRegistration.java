package pl.lordtricker.modid.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import pl.lordtricker.modid.client.config.ConfigLoader;
import pl.lordtricker.modid.config.ModConfigManager;
import pl.lordtricker.modid.util.Messages;

public class ClientCommandRegistration {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistration::registerModCommand);
    }

    private static void registerModCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess
    ) {
        dispatcher.register(
                ClientCommandManager.literal("ltb")
                        .executes(ctx -> {
                            String message = Messages.get("mod.info");
                            ctx.getSource().sendFeedback(CommandUi.colored(message));
                            return 1;
                        })
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
                                            ConfigLoader.saveConfig(ModConfigManager.toConfig());
                                            String msg = Messages.get("command.config.save.success");
                                            ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("reload")
                                        .executes(ctx -> {
                                            ModConfigManager.applyFrom(ConfigLoader.loadConfig());
                                            String msg = Messages.get("command.config.reload.success");
                                            ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                            return 1;
                                        })
                                )
                        )
        );
    }
}
