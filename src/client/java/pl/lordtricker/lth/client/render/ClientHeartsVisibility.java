package pl.lordtricker.lth.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.config.HeartsConfigLimits;
import pl.lordtricker.lth.core.config.HeartsSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientHeartsVisibility {
    private static final int REFRESH_INTERVAL_TICKS = 2;
    private static final Set<Integer> VISIBLE_IDS = new HashSet<>();
    private static long lastUpdateTime = -1L;
    private static boolean initialized = false;

    private ClientHeartsVisibility() {}

    public static void onClientTick(MinecraftClient client) {
        if (client.world == null) {
            clear();
            return;
        }
        HeartsSettings settings = HeartsState.getSettings();
        if (!settings.showHearts && !settings.showDamageAnimation) {
            VISIBLE_IDS.clear();
            initialized = false;
            return;
        }
        if (client.player == null) {
            VISIBLE_IDS.clear();
            initialized = false;
            return;
        }

        long worldTime = client.world.getTime();
        if (lastUpdateTime != -1 && worldTime - lastUpdateTime < REFRESH_INTERVAL_TICKS) {
            return;
        }
        lastUpdateTime = worldTime;

        int maxDistance = HeartsConfigLimits.clampDistance(settings.maxRenderDistanceBlocks);
        double maxDistSq = (double) maxDistance * (double) maxDistance;
        int maxPlayers = HeartsConfigLimits.clampMaxVisiblePlayers(settings.maxVisiblePlayers);
        int combatSeconds = HeartsConfigLimits.clampCombatMemorySeconds(settings.combatMemorySeconds);
        int combatTicks = combatSeconds * 20;

        Set<Integer> combatIds = ClientCombatTracker.getActiveIds(worldTime, combatTicks);
        List<DistanceEntry> candidates = new ArrayList<>();

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getId() == client.player.getId()) {
                continue;
            }
            int id = player.getId();
            if (combatIds.contains(id)) {
                continue;
            }
            double distSq = player.squaredDistanceTo(cameraPos);
            if (distSq <= maxDistSq) {
                candidates.add(new DistanceEntry(id, distSq));
            }
        }

        candidates.sort(Comparator.comparingDouble(entry -> entry.distanceSq));
        VISIBLE_IDS.clear();
        VISIBLE_IDS.addAll(combatIds);

        int remainingSlots = maxPlayers - combatIds.size();
        if (remainingSlots < 0) {
            remainingSlots = 0;
        }
        for (int i = 0; i < candidates.size() && i < remainingSlots; i++) {
            VISIBLE_IDS.add(candidates.get(i).id);
        }

        initialized = true;
    }

    public static boolean shouldRender(int entityId, double distSq, double maxDistSq) {
        if (VISIBLE_IDS.contains(entityId)) {
            return true;
        }
        if (!initialized || VISIBLE_IDS.isEmpty()) {
            return distSq <= maxDistSq;
        }
        int maxPlayers = HeartsConfigLimits.clampMaxVisiblePlayers(
                HeartsState.getSettings().maxVisiblePlayers
        );
        if (distSq <= maxDistSq && VISIBLE_IDS.size() < maxPlayers) {
            return true;
        }
        return false;
    }

    public static void clear() {
        VISIBLE_IDS.clear();
        lastUpdateTime = -1L;
        initialized = false;
        ClientCombatTracker.clear();
    }

    private record DistanceEntry(int id, double distanceSq) {}
}
