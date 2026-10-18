package dev.qwxon.bitsntracks.physics;

import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import net.minecraft.world.level.block.Block;

public final class CogwheelSizeHelper {
    public static final double TRACK_HALF_THICKNESS = 1.0 / 16.0;

    private CogwheelSizeHelper() {
    }

    public static double getRadius(Block block) {
        return BntPhysicsTuning.collisionRadius(CogwheelSize.of(block));
    }

    public static double getTrackRadius(Block block) {
        return BntPhysicsTuning.trackRadius(CogwheelSize.of(block));
    }

    public static double getTrackRestRadius(Block block) {
        return getTrackRadius(block) + TRACK_HALF_THICKNESS;
    }

    public static double getChainRadius(Block block) {
        return CogwheelSize.of(block).chainRadius();
    }

    public static double getChainRadius(Block block, boolean isLarge) {
        return sizedByBlock(block) ? getChainRadius(block) : (isLarge ? CogwheelSize.LARGE : CogwheelSize.SMALL).chainRadius();
    }

    public static boolean sizedByBlock(Block block) {
        return BntCogwheels.isOwnOrLegacyCogwheel(block);
    }

    public static double getToolHighlightRadius(Block block) {
        return BntPhysicsTuning.toolHighlightRadius(CogwheelSize.of(block));
    }

    public static double getSuspensionRest(Block block, Object blockEntity) {
        return CogwheelSize.of(block).suspensionRest() * BntTuning.TRAVEL.scale(blockEntity);
    }

    public static double getVerticalOffset(Block block) {
        return BntPhysicsTuning.verticalOffset(CogwheelSize.of(block));
    }

    public static double getDrawnRestRadius(Block block) {
        CogwheelSize size = CogwheelSize.of(block);
        return BntPhysicsTuning.collisionRadius(size) + BntPhysicsTuning.visualVerticalOffset(size) - BntPhysicsTuning.verticalOffset(size);
    }
}
