package pl.lordtricker.lth.core.damage;

public final class DamageIndicator {
    private final float amountHearts;
    private final boolean critical;
    private int ageTicks;

    public DamageIndicator(float amountHearts, boolean critical) {
        this.amountHearts = amountHearts;
        this.critical = critical;
    }

    public void tick() {
        ageTicks++;
    }

    public int getAgeTicks() {
        return ageTicks;
    }

    public float getAmountHearts() {
        return amountHearts;
    }

    public boolean isCritical() {
        return critical;
    }
}
