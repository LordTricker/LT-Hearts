package pl.lordtricker.lth.client.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ClientCombatTracker {
    private static final Map<Integer, Long> LAST_COMBAT_TICK = new HashMap<>();

    private ClientCombatTracker() {}

    public static void recordCombatWith(int entityId, long worldTime) {
        if (entityId < 0) {
            return;
        }
        LAST_COMBAT_TICK.put(entityId, worldTime);
    }

    public static Set<Integer> getActiveIds(long worldTime, int memoryTicks) {
        if (LAST_COMBAT_TICK.isEmpty()) {
            return Set.of();
        }
        Set<Integer> active = new HashSet<>();
        for (Iterator<Map.Entry<Integer, Long>> it = LAST_COMBAT_TICK.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> entry = it.next();
            if (worldTime - entry.getValue() > memoryTicks) {
                it.remove();
                continue;
            }
            active.add(entry.getKey());
        }
        return active;
    }

    public static boolean isActive(int entityId, long worldTime, int memoryTicks) {
        if (entityId < 0) {
            return false;
        }
        Long lastTick = LAST_COMBAT_TICK.get(entityId);
        if (lastTick == null) {
            return false;
        }
        if (worldTime - lastTick > memoryTicks) {
            LAST_COMBAT_TICK.remove(entityId);
            return false;
        }
        return true;
    }

    public static void clear() {
        LAST_COMBAT_TICK.clear();
    }
}
