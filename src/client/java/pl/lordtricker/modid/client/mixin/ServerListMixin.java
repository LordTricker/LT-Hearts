package pl.lordtricker.modid.client.mixin;

import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.modid.client.util.ServerListPatcher;
import pl.lordtricker.modid.config.ModConfigManager;

@Mixin(ServerList.class)
public class ServerListMixin {
    @Inject(method = "loadFile", at = @At("TAIL"), require = 0)
    private void modid$afterLoadFile(CallbackInfo ci) {
        modid$injectOrMove();
    }

    @Inject(method = "load()V", at = @At("TAIL"), cancellable = false, require = 0)
    private void modid$afterLoad(CallbackInfo ci) {
        modid$injectOrMove();
    }

    @Unique
    private void modid$injectOrMove() {
        if (!ModConfigManager.adsEnabled) return;
        ServerListPatcher.injectOrMove((ServerList) (Object) this);
    }
}
