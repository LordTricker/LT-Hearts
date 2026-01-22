package pl.lordtricker.lth.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.lth.client.render.ClientCombatTracker;
import pl.lordtricker.lth.client.render.ClientDamageTracker;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityDamage", at = @At("TAIL"))
    private void lth_onEntityDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return;
        }
        int attackerId = packet.sourceCauseId();
        if (attackerId != client.player.getId()) {
            return;
        }
        int targetId = packet.entityId();
        if (targetId == client.player.getId()) {
            return;
        }
        boolean critical = false;
        int directId = packet.sourceDirectId();
        Entity direct = client.world.getEntityById(directId);
        if (direct instanceof PersistentProjectileEntity persistent) {
            critical = persistent.isCritical();
        }
        long worldTime = client.world.getTime();
        ClientDamageTracker.recordAttack(targetId, worldTime, critical);
        ClientCombatTracker.recordCombatWith(targetId, worldTime);
    }
}
