package dev.qwxon.bitsntracks.physics;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class BntPonderPhysics {
    private static final Map<Level, Stage> STAGES = new WeakHashMap<>();

    private BntPonderPhysics() {
    }

    public interface Stage {
        double wheelDrop(BlockPos pos);

        Vec3 toWorld(Vec3 levelPos);

        double groundAt(double x, double z);

        double epoch();
    }

    public static void setStage(Level level, Stage stage) {
        STAGES.put(level, stage);
    }

    public static Stage stage(Level level) {
        return level == null || STAGES.isEmpty() ? null : STAGES.get(level);
    }

    public static double wheelDrop(BlockEntity be) {
        Stage stage = stage(be.getLevel());
        return stage == null ? Double.NaN : stage.wheelDrop(be.getBlockPos());
    }
}
