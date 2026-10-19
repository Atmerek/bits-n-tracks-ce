package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.world.level.block.Block;

public enum CogwheelSize {
    TINY(0.25, 0.45, 4.0, 4.0 * Math.sqrt(2.0) / 16.0),
    INTERMEDIATE(0.375, 0.55, 5.0, Math.hypot(6.0, 3.0) / 16.0),
    SMALL(0.5, 0.65, 6.0, 9.0 / 16.0),
    MEDIUM(0.75, 1.0, 7.0, 9.0 * Math.sqrt(2.0) / 16.0),
    MEDIUM_LARGE(0.875, 1.15, 7.5, 14.0 / Math.cos(Math.PI / 8.0) / 16.0),
    LARGE(1.0, 1.3, 8.0, Math.hypot(16.0, 6.6) / 16.0);

    private final double chainRadius;
    private final double suspensionRest;
    private final double shapeRadius;
    private final double drawnRadius;

    CogwheelSize(double chainRadius, double suspensionRest, double shapeRadius, double drawnRadius) {
        this.chainRadius = chainRadius;
        this.suspensionRest = suspensionRest;
        this.shapeRadius = shapeRadius;
        this.drawnRadius = drawnRadius;
    }

    public double chainRadius() {
        return this.chainRadius;
    }

    public double suspensionRest() {
        return this.suspensionRest;
    }

    public double shapeRadius() {
        return this.shapeRadius;
    }

    public double drawnRadius() {
        return this.drawnRadius;
    }

    public static CogwheelSize of(Block block) {
        if (block instanceof BntFlangedCogwheelBlock cogwheel) {
            return cogwheel.size();
        }
        if (block instanceof HiddenCogwheelBlock hidden) {
            return hidden.size();
        }
        return block instanceof ICogWheel cog && cog.isLargeCog() ? LARGE : SMALL;
    }

    public static CogwheelSize ofNode(boolean isLarge, boolean hasSmallCogwheelOffset) {
        return isLarge ? hasSmallCogwheelOffset ? MEDIUM : LARGE : hasSmallCogwheelOffset ? SMALL : TINY;
    }
}
