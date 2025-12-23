package pl.lordtricker.lth.core.damage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DamageIndicatorTracker {
    public static final int MAX_AGE_TICKS = 20;
    private static final int ATTACK_WINDOW_TICKS = 8;

    private final Map<Integer, Float> lastHealth = new HashMap<>();
    private final Map<Integer, List<DamageIndicator>> indicators = new HashMap<>();
    private int lastAttackedEntityId = -1;
    private long lastAttackTime = -1;
    private boolean lastAttackCritical = false;

    public void recordAttack(int entityId, long worldTime, boolean critical) {
        lastAttackedEntityId = entityId;
        lastAttackTime = worldTime;
        lastAttackCritical = critical;
    }

    public void updateHealth(Map<Integer, Float> healthById, int localPlayerId, long worldTime) {
        if (healthById == null) {
            clear();
            return;
        }
        if (lastAttackTime != -1 && worldTime - lastAttackTime > ATTACK_WINDOW_TICKS) {
            lastAttackTime = -1;
            lastAttackedEntityId = -1;
        }

        Set<Integer> activeIds = new HashSet<>(healthById.keySet());
        for (Map.Entry<Integer, Float> entry : healthById.entrySet()) {
            int entityId = entry.getKey();
            if (entityId == localPlayerId) {
                continue;
            }
            float health = entry.getValue();
            Float last = lastHealth.put(entityId, health);
            if (last != null && health < last) {
                if (entityId == lastAttackedEntityId
                        && worldTime - lastAttackTime <= ATTACK_WINDOW_TICKS) {
                    float deltaHearts = (last - health) / 2f;
                    if (deltaHearts > 0f) {
                        addIndicator(entityId, deltaHearts, lastAttackCritical);
                    }
                    lastAttackTime = -1;
                    lastAttackedEntityId = -1;
                    lastAttackCritical = false;
                }
            }
        }

        lastHealth.keySet().retainAll(activeIds);
        indicators.keySet().retainAll(activeIds);

        for (Iterator<Map.Entry<Integer, List<DamageIndicator>>> it = indicators.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, List<DamageIndicator>> entry = it.next();
            List<DamageIndicator> list = entry.getValue();
            list.removeIf(indicator -> {
                indicator.tick();
                return indicator.getAgeTicks() > MAX_AGE_TICKS;
            });
            if (list.isEmpty()) {
                it.remove();
            }
        }
    }

    public List<DamageIndicator> getIndicators(int entityId) {
        List<DamageIndicator> list = indicators.get(entityId);
        if (list == null) {
            return List.of();
        }
        return list;
    }

    public void clear() {
        lastHealth.clear();
        indicators.clear();
        lastAttackedEntityId = -1;
        lastAttackTime = -1;
        lastAttackCritical = false;
    }

    private void addIndicator(int entityId, float amountHearts, boolean critical) {
        List<DamageIndicator> list = indicators.computeIfAbsent(entityId, id -> new ArrayList<>());
        list.add(new DamageIndicator(amountHearts, critical));
    }
}
