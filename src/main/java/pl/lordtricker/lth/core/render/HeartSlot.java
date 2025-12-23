package pl.lordtricker.lth.core.render;

public class HeartSlot {
    private final HeartSlotType type;
    private final int bonusHearts;

    private HeartSlot(HeartSlotType type, int bonusHearts) {
        this.type = type;
        this.bonusHearts = bonusHearts;
    }

    public static HeartSlot full() {
        return new HeartSlot(HeartSlotType.FULL, 0);
    }

    public static HeartSlot half() {
        return new HeartSlot(HeartSlotType.HALF, 0);
    }

    public static HeartSlot empty() {
        return new HeartSlot(HeartSlotType.EMPTY, 0);
    }

    public static HeartSlot ellipsis() {
        return new HeartSlot(HeartSlotType.ELLIPSIS, 0);
    }

    public static HeartSlot bonus(int hearts) {
        return new HeartSlot(HeartSlotType.BONUS, hearts);
    }

    public HeartSlotType getType() {
        return type;
    }

    public int getBonusHearts() {
        return bonusHearts;
    }
}
