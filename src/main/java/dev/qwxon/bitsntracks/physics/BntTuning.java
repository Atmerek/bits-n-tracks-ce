package dev.qwxon.bitsntracks.physics;

import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import java.util.Arrays;
import net.minecraft.util.Mth;

public enum BntTuning {
    STIFFNESS("stiffness", 0.03, 8.0),
    DAMPING("damping", 0.08, 8.0),
    TRAVEL("travel", 0.35, 2.5),
    SUPPORT("support", 0.1, 5.0);

    public static final int MIN = 1;
    public static final int MAX = 11;
    public static final int DEFAULT = 4;
    private static final BntTuning[] VALUES = values();

    private final String key;
    private final double weakest;
    private final double strongest;

    BntTuning(String key, double weakest, double strongest) {
        this.key = key;
        this.weakest = weakest;
        this.strongest = strongest;
    }

    public String key() {
        return this.key;
    }

    public String translationKey() {
        return "bits_n_tracks.tuning." + this.key;
    }

    public static int clamp(int level) {
        return Mth.clamp(level, MIN, MAX);
    }

    public double scale(int level) {
        int clamped = clamp(level);
        return clamped <= DEFAULT
            ? Math.pow(this.weakest, (DEFAULT - clamped) / (double)(DEFAULT - MIN))
            : Math.pow(this.strongest, (clamped - DEFAULT) / (double)(MAX - DEFAULT));
    }

    public double scale(Object blockEntity) {
        return blockEntity instanceof KineticBlockEntityPhysicsAccess access ? this.scale(access.bnt$getTuning(this)) : 1.0;
    }

    public int levelOf(Object blockEntity) {
        return blockEntity instanceof KineticBlockEntityPhysicsAccess access ? access.bnt$getTuning(this) : DEFAULT;
    }

    public BntTuning cycle(int direction) {
        return byIndex(this.ordinal() + direction);
    }

    public static BntTuning byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public static int[] defaults() {
        int[] levels = new int[VALUES.length];
        Arrays.fill(levels, DEFAULT);
        return levels;
    }
}
