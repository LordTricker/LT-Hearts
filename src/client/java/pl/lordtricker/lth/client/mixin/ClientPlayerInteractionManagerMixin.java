package pl.lordtricker.lth.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.lth.client.render.ClientCombatTracker;
import pl.lordtricker.lth.client.render.ClientDamageTracker;
import pl.lordtricker.lth.core.combat.CriticalHitRules;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void lth_onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof AbstractClientPlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }
            boolean isCrit = CriticalHitRules.isCritical(
                    (float) player.fallDistance,
                    player.isOnGround(),
                    player.isClimbing(),
                    player.isTouchingWater(),
                    player.hasStatusEffect(StatusEffects.BLINDNESS),
                    player.hasVehicle(),
                    player.isSprinting()
            );
            long worldTime = client.world.getTime();
            ClientDamageTracker.recordAttack(target.getId(), worldTime, isCrit);
            ClientCombatTracker.recordCombatWith(target.getId(), worldTime);
        }
    }
}
