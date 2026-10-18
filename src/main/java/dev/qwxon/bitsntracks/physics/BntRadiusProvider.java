package dev.qwxon.bitsntracks.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class BntRadiusProvider {
    private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> CURRENT_ORIGIN = new ThreadLocal<>();

    private BntRadiusProvider() {
    }

    public record Scope(Level level, BlockPos origin) implements AutoCloseable {
        @Override
        public void close() {
            set(this.level, this.origin);
        }
    }

    public static Scope at(Level level, BlockPos origin) {
        Scope previous = keep();
        set(level, origin);
        return previous;
    }

    public static Scope keep() {
        return new Scope(CURRENT_LEVEL.get(), CURRENT_ORIGIN.get());
    }

    private static void set(Level level, BlockPos origin) {
        if (level == null) {
            CURRENT_LEVEL.remove();
        } else {
            CURRENT_LEVEL.set(level);
        }
        if (origin == null) {
            CURRENT_ORIGIN.remove();
        } else {
            CURRENT_ORIGIN.set(origin);
        }
    }

    public static Level level() {
        return CURRENT_LEVEL.get();
    }

    public static BlockPos origin() {
        return CURRENT_ORIGIN.get();
    }

    public static double getTrackRadius(BlockPos localPos, double fallbackRadius) {
        Level level = CURRENT_LEVEL.get();
        if (level == null) {
            return fallbackRadius;
        }
        BlockPos origin = CURRENT_ORIGIN.get();
        Block block = level.getBlockState(origin != null ? origin.offset(localPos) : localPos).getBlock();
        return CogwheelSizeHelper.sizedByBlock(block) ? CogwheelSizeHelper.getTrackRadius(block) : fallbackRadius;
    }
}
