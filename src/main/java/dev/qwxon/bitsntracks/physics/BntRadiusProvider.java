package dev.qwxon.bitsntracks.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BntRadiusProvider {
    private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> CURRENT_ORIGIN = new ThreadLocal<>();

    public static void setLevel(Level level) {
        CURRENT_LEVEL.set(level);
    }

    public static void setOrigin(BlockPos origin) {
        CURRENT_ORIGIN.set(origin);
    }

    public static void clearLevel() {
        CURRENT_LEVEL.remove();
        CURRENT_ORIGIN.remove();
    }

    public static double getTrackRadius(BlockPos bPos, boolean isLargeDefault, double fallbackRadius) {
        Level level = CURRENT_LEVEL.get();
        BlockPos lookupPos = CURRENT_ORIGIN.get() != null ? CURRENT_ORIGIN.get().offset(bPos) : bPos;
        if (level == null) {
            return fallbackRadius;
        } else {
            BlockState state = level.getBlockState(lookupPos);
            if (CogwheelSizeHelper.isLarge(state.getBlock())) {
                return BntPhysicsTuning.getLargeTrackRadius();
            } else if (CogwheelSizeHelper.isMedium(state.getBlock())) {
                return BntPhysicsTuning.getMediumTrackRadius();
            } else if (CogwheelSizeHelper.isTiny(state.getBlock())) {
                return BntPhysicsTuning.getTinyTrackRadius();
            } else {
                return fallbackRadius;
            }
        }
    }
}
