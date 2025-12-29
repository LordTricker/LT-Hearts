package pl.lordtricker.lth.core.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HeartsDisplayLayout {
    private static final int HEARTS_PER_ROW = 10;
    private static final int MAX_ROWS = 3;
    private static final int OVERFLOW_HEARTS = HEARTS_PER_ROW * MAX_ROWS;
    private static final int OVERFLOW_VISIBLE_HEARTS = OVERFLOW_HEARTS - 2;

    private final List<List<HeartSlot>> rows;

    private HeartsDisplayLayout(List<List<HeartSlot>> rows) {
        this.rows = rows;
    }

    public List<List<HeartSlot>> getRows() {
        return rows;
    }

    public static HeartsDisplayLayout build(float health, float maxHealth) {
        float clampedHealth = Math.max(0f, Math.min(health, maxHealth));
        int maxHearts = Math.max(0, (int) Math.ceil(maxHealth / 2f));
        if (maxHearts <= 0) {
            return new HeartsDisplayLayout(Collections.emptyList());
        }

        if (maxHearts > OVERFLOW_HEARTS) {
            return new HeartsDisplayLayout(
                    Collections.singletonList(buildCollapsedOverflowRow(clampedHealth, maxHearts))
            );
        }

        int rowsCount = Math.min(MAX_ROWS, (int) Math.ceil(maxHearts / (float) HEARTS_PER_ROW));
        List<List<HeartSlot>> rows = new ArrayList<>(rowsCount);

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            rows.add(buildStandardRow(clampedHealth, maxHearts, rowIndex));
        }

        return new HeartsDisplayLayout(rows);
    }

    private static List<HeartSlot> buildStandardRow(float health, int maxHearts, int rowIndex) {
        int startIndex = rowIndex * HEARTS_PER_ROW + 1;
        int heartsInRow = Math.min(HEARTS_PER_ROW, maxHearts - rowIndex * HEARTS_PER_ROW);
        List<HeartSlot> row = new ArrayList<>(heartsInRow);
        for (int i = 0; i < heartsInRow; i++) {
            int heartIndex = startIndex + i;
            row.add(heartSlotForHealth(health, heartIndex));
        }
        return row;
    }

    private static List<HeartSlot> buildOverflowRow(float health, int maxHearts) {
        List<HeartSlot> row = new ArrayList<>(HEARTS_PER_ROW);
        int startIndex = (MAX_ROWS - 1) * HEARTS_PER_ROW + 1;
        int visibleHearts = HEARTS_PER_ROW - 2;
        for (int i = 0; i < visibleHearts; i++) {
            int heartIndex = startIndex + i;
            row.add(heartSlotForHealth(health, heartIndex));
        }
        row.add(HeartSlot.ellipsis());
        int extraHearts = Math.max(1, maxHearts - OVERFLOW_VISIBLE_HEARTS);
        row.add(HeartSlot.bonus(extraHearts));
        return row;
    }

    private static List<HeartSlot> buildCollapsedOverflowRow(float health, int maxHearts) {
        List<HeartSlot> row = new ArrayList<>(HEARTS_PER_ROW);
        int visibleHearts = HEARTS_PER_ROW - 2;
        for (int i = 0; i < visibleHearts; i++) {
            int heartIndex = 1 + i;
            row.add(heartSlotForHealth(health, heartIndex));
        }
        row.add(HeartSlot.ellipsis());
        int extraHearts = Math.max(1, maxHearts - visibleHearts);
        row.add(HeartSlot.bonus(extraHearts));
        return row;
    }

    private static HeartSlot heartSlotForHealth(float health, int heartIndex) {
        float fullThreshold = heartIndex * 2f;
        float halfThreshold = fullThreshold - 1f;
        if (health >= fullThreshold) {
            return HeartSlot.full();
        }
        if (health >= halfThreshold) {
            return HeartSlot.half();
        }
        return HeartSlot.empty();
    }
}
