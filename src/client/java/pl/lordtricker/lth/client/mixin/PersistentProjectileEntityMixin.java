package pl.lordtricker.lth.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.lth.client.render.ClientCombatTracker;
import pl.lordtricker.lth.client.render.ClientDamageTracker;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void lth_onEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        Entity owner = projectile.getOwner();
        if (owner == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        if (owner.getId() != client.player.getId()) {
            return;
        }
        Entity target = hitResult.getEntity();
        if (target instanceof AbstractClientPlayerEntity targetPlayer) {
            if (client.world == null) {
                return;
            }
            long worldTime = client.world.getTime();
            boolean critical = projectile.isCritical();
            ClientDamageTracker.recordAttack(targetPlayer.getId(), worldTime, critical);
            ClientCombatTracker.recordCombatWith(targetPlayer.getId(), worldTime);
        }
    }
}
