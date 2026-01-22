package pl.lordtricker.lth.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.config.HeartsConfigLimits;
import pl.lordtricker.lth.core.damage.DamageIndicator;
import pl.lordtricker.lth.core.damage.DamageIndicatorTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientDamageTracker {
    private static final DamageIndicatorTracker TRACKER = new DamageIndicatorTracker();
    private static final int NEAR_DISTANCE_SQ = 25;
    private static final int MID_DISTANCE_SQ = 100;
    private static final int FAR_DISTANCE_SQ = 256;
    private static final int NEAR_INTERVAL_TICKS = 2;
    private static final int MID_INTERVAL_TICKS = 3;
    private static final int FAR_INTERVAL_TICKS = 4;
    private static final int VERY_FAR_INTERVAL_TICKS = 5;
    private static final int FULL_SCAN_INTERVAL_TICKS = 40;
    private static final Map<Integer, Long> NEXT_UPDATE_TICK = new HashMap<>();
    private static long lastFullScanTick = -1L;

    private ClientDamageTracker() {}

    public static void recordAttack(int entityId, long worldTime, boolean critical) {
        TRACKER.recordAttack(entityId, worldTime, critical);
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.world == null) {
            TRACKER.clear();
            NEXT_UPDATE_TICK.clear();
            lastFullScanTick = -1L;
            return;
        }
        if (!HeartsState.getSettings().showDamageAnimation) {
            TRACKER.clear();
            NEXT_UPDATE_TICK.clear();
            lastFullScanTick = -1L;
            return;
        }

        TRACKER.tickIndicators();

        long worldTime = client.world.getTime();
        boolean fullScan = lastFullScanTick == -1L
                || worldTime - lastFullScanTick >= FULL_SCAN_INTERVAL_TICKS;
        if (fullScan) {
            lastFullScanTick = worldTime;
        }

        Map<Integer, Float> healthById = new HashMap<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            int id = player.getId();
            if (fullScan || shouldUpdatePlayer(client, player, worldTime)) {
                healthById.put(id, player.getHealth());
            }
        }
        int localPlayerId = client.player != null ? client.player.getId() : -1;
        TRACKER.updateHealth(healthById, localPlayerId, worldTime, fullScan);
    }

    public static List<DamageIndicator> getIndicators(int entityId) {
        return TRACKER.getIndicators(entityId);
    }

    private static boolean shouldUpdatePlayer(MinecraftClient client, AbstractClientPlayerEntity player, long worldTime) {
        long nextTick = NEXT_UPDATE_TICK.getOrDefault(player.getId(), -1L);
        if (nextTick != -1L && worldTime < nextTick) {
            return false;
        }
        int interval = computeUpdateInterval(client, player, worldTime);
        NEXT_UPDATE_TICK.put(player.getId(), worldTime + interval);
        return true;
    }

    private static int computeUpdateInterval(MinecraftClient client, AbstractClientPlayerEntity player, long worldTime) {
        if (client.player == null) {
            return MID_INTERVAL_TICKS;
        }
        int combatSeconds = HeartsConfigLimits.clampCombatMemorySeconds(
                HeartsState.getSettings().combatMemorySeconds
        );
        int combatTicks = combatSeconds * 20;
        if (ClientCombatTracker.isActive(player.getId(), worldTime, combatTicks)) {
            return 1;
        }
        double distSq = player.squaredDistanceTo(client.player);
        if (distSq <= NEAR_DISTANCE_SQ) {
            return NEAR_INTERVAL_TICKS;
        }
        if (distSq <= MID_DISTANCE_SQ) {
            return MID_INTERVAL_TICKS;
        }
        if (distSq <= FAR_DISTANCE_SQ) {
            return FAR_INTERVAL_TICKS;
        }
        return VERY_FAR_INTERVAL_TICKS;
    }
}
