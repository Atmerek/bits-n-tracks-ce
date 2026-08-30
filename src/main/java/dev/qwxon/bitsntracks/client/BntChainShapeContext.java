package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class BntChainShapeContext {
    private static final ThreadLocal<Level> LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> CONTROLLER = new ThreadLocal<>();

    private BntChainShapeContext() {
    }

    public static void set(Level level, BlockPos controllerPos) {
        LEVEL.set(level);
        CONTROLLER.set(controllerPos);
    }

    public static void clear() {
        LEVEL.remove();
        CONTROLLER.remove();
    }

    public static Vec3 transform(RenderedChainPathNode node) {
        Vec3 position = node.getPosition();
        Level level = LEVEL.get();
        BlockPos controllerPos = CONTROLLER.get();
        if (level == null || controllerPos == null || !level.isClientSide) {
            return position;
        }

        BlockEntity controllerBe = level.getBlockEntity(controllerPos);
        return controllerBe == null ? position : BntClientCompat.getTransformedPosition(controllerBe, position, node.relativePos());
    }
}
