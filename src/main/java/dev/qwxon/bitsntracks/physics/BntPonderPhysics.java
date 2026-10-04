package dev.qwxon.bitsntracks.physics;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class BntPonderPhysics {
    private static final Map<Level, WeakReference<Stage>> STAGES = new WeakHashMap<>();

    private BntPonderPhysics() {
    }

    public interface Stage {
        double wheelDrop(BlockPos pos);

        Vec3 toWorld(Vec3 levelPos);

        double groundAt(double x, double z);

        double epoch();

        long tick();
    }

    public static void setStage(Level level, Stage stage) {
        STAGES.put(level, new WeakReference<>(stage));
    }

    public static Stage stage(Level level) {
        if (level == null || STAGES.isEmpty()) {
            return null;
        }
        WeakReference<Stage> stage = STAGES.get(level);
        return stage == null ? null : stage.get();
    }

    public static long clock(Level level) {
        Stage stage = stage(level);
        return stage == null ? level.getGameTime() : stage.tick();
    }

    public static double wheelDrop(BlockEntity be) {
        Stage stage = stage(be.getLevel());
        return stage == null ? Double.NaN : stage.wheelDrop(be.getBlockPos());
    }
}
