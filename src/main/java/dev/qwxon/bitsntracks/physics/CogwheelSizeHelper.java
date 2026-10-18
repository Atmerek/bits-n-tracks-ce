package dev.qwxon.bitsntracks.physics;

import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.minecraft.world.level.block.Block;

public final class CogwheelSizeHelper {
    private static final double TINY_CHAIN_RADIUS = 0.25;
    private static final double INTERMEDIATE_CHAIN_RADIUS = 0.375;
    private static final double SMALL_CHAIN_RADIUS = 0.5;
    private static final double MEDIUM_CHAIN_RADIUS = 0.75;
    private static final double LARGE_CHAIN_RADIUS = 1.0;

    private static final double TINY_SUSPENSION_REST = 0.45;
    private static final double INTERMEDIATE_SUSPENSION_REST = 0.55;
    private static final double SMALL_SUSPENSION_REST = 0.65;
    private static final double MEDIUM_SUSPENSION_REST = 1.0;
    private static final double LARGE_SUSPENSION_REST = 1.3;

    public static final double TRACK_HALF_THICKNESS = 1.0 / 16.0;

    private CogwheelSizeHelper() {
    }

    private static CogwheelSize sizeOf(Block block) {
        return CogwheelSize.of(block);
    }

    public static boolean isLarge(Block block) {
        return sizeOf(block) == CogwheelSize.LARGE;
    }

    public static boolean isMedium(Block block) {
        return sizeOf(block) == CogwheelSize.MEDIUM;
    }

    public static boolean isIntermediate(Block block) {
        return sizeOf(block) == CogwheelSize.INTERMEDIATE;
    }

    public static boolean isTiny(Block block) {
        return sizeOf(block) == CogwheelSize.TINY;
    }

    public static double getRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeCollisionRadius();
            case MEDIUM -> BntPhysicsTuning.getMediumCollisionRadius();
            case INTERMEDIATE -> BntPhysicsTuning.getIntermediateCollisionRadius();
            case TINY -> BntPhysicsTuning.getTinyCollisionRadius();
            case SMALL -> BntPhysicsTuning.getSmallCollisionRadius();
        };
    }

    public static double getTrackRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeTrackRadius();
            case MEDIUM -> BntPhysicsTuning.getMediumTrackRadius();
            case INTERMEDIATE -> BntPhysicsTuning.getIntermediateTrackRadius();
            case TINY -> BntPhysicsTuning.getTinyTrackRadius();
            case SMALL -> BntPhysicsTuning.getSmallTrackRadius();
        };
    }

    public static double getTrackRestRadius(Block block) {
        return getTrackRadius(block) + TRACK_HALF_THICKNESS;
    }

    public static double getChainRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> LARGE_CHAIN_RADIUS;
            case MEDIUM -> MEDIUM_CHAIN_RADIUS;
            case INTERMEDIATE -> INTERMEDIATE_CHAIN_RADIUS;
            case TINY -> TINY_CHAIN_RADIUS;
            case SMALL -> SMALL_CHAIN_RADIUS;
        };
    }

    public static double getChainRadius(Block block, boolean isLarge) {
        return sizedByBlock(block) ? getChainRadius(block) : isLarge ? LARGE_CHAIN_RADIUS : SMALL_CHAIN_RADIUS;
    }

    public static boolean sizedByBlock(Block block) {
        return HiddenCogwheelCompat.isOwnOrLegacyCogwheel(block);
    }

    public static double getToolHighlightRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeToolHighlightRadius();
            case MEDIUM -> BntPhysicsTuning.getMediumToolHighlightRadius();
            case INTERMEDIATE -> BntPhysicsTuning.getIntermediateToolHighlightRadius();
            case TINY -> BntPhysicsTuning.getTinyToolHighlightRadius();
            case SMALL -> BntPhysicsTuning.getSmallToolHighlightRadius();
        };
    }

    public static double getSuspensionRest(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> LARGE_SUSPENSION_REST;
            case MEDIUM -> MEDIUM_SUSPENSION_REST;
            case INTERMEDIATE -> INTERMEDIATE_SUSPENSION_REST;
            case TINY -> TINY_SUSPENSION_REST;
            case SMALL -> SMALL_SUSPENSION_REST;
        };
    }

    public static double getSuspensionRest(Block block, Object blockEntity) {
        return getSuspensionRest(block) * BntTuning.TRAVEL.scale(blockEntity);
    }

    public static double getVerticalOffset(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeVerticalOffset();
            case MEDIUM -> BntPhysicsTuning.getMediumVerticalOffset();
            case INTERMEDIATE -> BntPhysicsTuning.getIntermediateVerticalOffset();
            case TINY -> BntPhysicsTuning.getTinyVerticalOffset();
            case SMALL -> BntPhysicsTuning.getSmallVerticalOffset();
        };
    }

    public static double getVisualVerticalOffset(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeVisualVerticalOffset();
            case MEDIUM -> BntPhysicsTuning.getMediumVisualVerticalOffset();
            case INTERMEDIATE -> BntPhysicsTuning.getIntermediateVisualVerticalOffset();
            case TINY -> BntPhysicsTuning.getTinyVisualVerticalOffset();
            case SMALL -> BntPhysicsTuning.getSmallVisualVerticalOffset();
        };
    }

    public static double getDrawnRestRadius(Block block) {
        return getRadius(block) + getVisualVerticalOffset(block) - getVerticalOffset(block);
    }

}
