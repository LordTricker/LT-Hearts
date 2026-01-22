package pl.lordtricker.lth.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.lordtricker.lth.client.render.ClientCombatTracker;

@Mixin(Entity.class)
public class EntityDamageMixin {
    @Inject(method = "damage", at = @At("TAIL"))
    private void lth_onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity)) {
            return;
        }
        if (source == null) {
            return;
        }
        if (source.getAttacker() instanceof AbstractClientPlayerEntity attacker) {
            ClientCombatTracker.recordCombatWith(attacker.getId(), attacker.getWorld().getTime());
        }
    }
}
