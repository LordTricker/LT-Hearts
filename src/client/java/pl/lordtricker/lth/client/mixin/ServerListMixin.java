package pl.lordtricker.lth.client.mixin;

import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.lth.client.util.ServerListPatcher;
import pl.lordtricker.lth.core.HeartsState;

@Mixin(ServerList.class)
public class ServerListMixin {
    @Inject(method = "loadFile", at = @At("TAIL"), require = 0)
    private void lth$afterLoadFile(CallbackInfo ci) {
        lth$injectOrMove();
    }

    @Inject(method = "load()V", at = @At("TAIL"), cancellable = false, require = 0)
    private void lth$afterLoad(CallbackInfo ci) {
        lth$injectOrMove();
    }

    @Unique
    private void lth$injectOrMove() {
        if (!HeartsState.getConfig().adsEnabled) return;
        ServerListPatcher.injectOrMove((ServerList) (Object) this);
    }
}
