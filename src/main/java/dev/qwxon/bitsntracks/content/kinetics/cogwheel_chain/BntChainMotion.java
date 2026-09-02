package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.ICogwheelNode;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class BntChainMotion {
    private static final ThreadLocal<Function<BlockPos, Vec3>> DISPLACEMENT = new ThreadLocal<>();
    private static final ThreadLocal<Function<BlockPos, Direction>> ROUTE = new ThreadLocal<>();
    private static final ThreadLocal<BntChainGeometry.Layout> LAYOUT = new ThreadLocal<>();

    public static BntChainGeometry.Layout swapLayout(BntChainGeometry.Layout layout) {
        BntChainGeometry.Layout previous = LAYOUT.get();
        if (layout == null) {
            LAYOUT.remove();
        } else {
            LAYOUT.set(layout);
        }
        return previous;
    }

    public static BntChainGeometry.Layout layout() {
        return LAYOUT.get();
    }

    private BntChainMotion() {
    }

    public static void setDisplacementSource(Function<BlockPos, Vec3> source) {
        DISPLACEMENT.set(source);
    }

    public static Function<BlockPos, Vec3> swapDisplacementSource(Function<BlockPos, Vec3> source) {
        Function<BlockPos, Vec3> previous = DISPLACEMENT.get();
        if (source == null) {
            DISPLACEMENT.remove();
        } else {
            DISPLACEMENT.set(source);
        }
        return previous;
    }

    public static void clearDisplacementSource() {
        DISPLACEMENT.remove();
        ROUTE.remove();
    }

    public static Vec3 displacement(BlockPos localPos) {
        Function<BlockPos, Vec3> source = DISPLACEMENT.get();
        if (source == null) {
            return Vec3.ZERO;
        }
        Vec3 displacement = source.apply(localPos);
        return displacement == null ? Vec3.ZERO : displacement;
    }

    public static Function<BlockPos, Direction> swapRouteSource(Function<BlockPos, Direction> source) {
        Function<BlockPos, Direction> previous = ROUTE.get();
        if (source == null) {
            ROUTE.remove();
        } else {
            ROUTE.set(source);
        }
        return previous;
    }

    public static Direction routeSide(BlockPos localPos) {
        Function<BlockPos, Direction> source = ROUTE.get();
        return source == null ? null : source.apply(localPos);
    }

    public static Vec3 liveCenter(ICogwheelNode node) {
        return node.center().add(displacement(node.pos()));
    }

    public static double[] displacementSignature(List<? extends ICogwheelNode> nodes) {
        double[] signature = new double[nodes.size() * 4];
        for (int i = 0; i < nodes.size(); i++) {
            Vec3 displacement = displacement(nodes.get(i).pos());
            Direction route = routeSide(nodes.get(i).pos());
            signature[i * 4] = displacement.x;
            signature[i * 4 + 1] = displacement.y;
            signature[i * 4 + 2] = displacement.z;
            signature[i * 4 + 3] = route == null ? -1.0 : route.ordinal();
        }
        return signature;
    }
}
