package pl.lordtricker.modid.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.modid.client.util.ServerListPatcher;
import pl.lordtricker.modid.config.ModConfigManager;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {
    @Unique
    private boolean modid$refreshedOnce = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void modid$afterInit(CallbackInfo ci) {
        if (!ModConfigManager.adsEnabled) return;
        ServerList sl = modid$findServerList(this);
        if (sl != null) {
            ServerListPatcher.injectOrMove(sl);
        }
        if (!modid$refreshedOnce) {
            modid$refreshedOnce = true;
            try {
                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.execute(() -> {
                            ServerList s2 = modid$findServerList(this);
                            if (s2 != null) {
                                ServerListPatcher.injectOrMove(s2);
                            }
                        });
                    }
                }, "modid-servers-refresh");
                t.setDaemon(true);
                t.start();
            } catch (Throwable ignored) {}
        }
    }

    @Unique
    private static ServerList modid$findServerList(Object screen) {
        try {
            for (java.lang.reflect.Field f : screen.getClass().getDeclaredFields()) {
                if (ServerList.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object v = f.get(screen);
                    if (v instanceof ServerList sl) {
                        return sl;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
