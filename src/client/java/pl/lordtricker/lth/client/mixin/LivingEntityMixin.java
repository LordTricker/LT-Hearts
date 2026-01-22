package pl.lordtricker.lth.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.lordtricker.lth.client.render.ClientCombatTracker;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "damage", at = @At("TAIL"))
    private void lth_onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity)) {
            return;
        }
        if (source == null) {
            return;
        }
        if (source.getAttacker() instanceof AbstractClientPlayerEntity attacker) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }
            ClientCombatTracker.recordCombatWith(attacker.getId(), client.world.getTime());
        }
    }
}
