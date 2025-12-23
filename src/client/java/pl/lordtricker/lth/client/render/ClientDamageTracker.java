package pl.lordtricker.lth.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.damage.DamageIndicator;
import pl.lordtricker.lth.core.damage.DamageIndicatorTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientDamageTracker {
    private static final DamageIndicatorTracker TRACKER = new DamageIndicatorTracker();

    private ClientDamageTracker() {}

    public static void recordAttack(int entityId, long worldTime, boolean critical) {
        TRACKER.recordAttack(entityId, worldTime, critical);
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.world == null) {
            TRACKER.clear();
            return;
        }
        if (!HeartsState.getSettings().showDamageAnimation) {
            TRACKER.clear();
            return;
        }

        Map<Integer, Float> healthById = new HashMap<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            healthById.put(player.getId(), player.getHealth());
        }
        int localPlayerId = client.player != null ? client.player.getId() : -1;
        TRACKER.updateHealth(healthById, localPlayerId, client.world.getTime());
    }

    public static List<DamageIndicator> getIndicators(int entityId) {
        return TRACKER.getIndicators(entityId);
    }
}
