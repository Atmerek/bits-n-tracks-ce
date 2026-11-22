package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class BntTrackPlane {
    private static final double EPSILON = 1.0E-6;
    private static final int CACHE_LIMIT = 512;
    private static final Map<Level, Map<BlockPos, Plane>> PLANES = new WeakHashMap<>();

    private static int epoch;

    private record Plane(long time, int epoch, double centre, int quarters) {
    }

    public static synchronized void invalidate() {
        epoch++;
    }

    private BntTrackPlane() {
    }

    public static boolean onTrack(Level level, BlockPos pos) {
        BlockPos controller = BntBeltTension.trackController(level, pos);
        return controller != null && BntChains.chainAt(level, controller) != null;
    }

    public static boolean straight(Level level, BlockPos pos) {
        BlockPos controller = BntBeltTension.trackController(level, pos);
        return level.getBlockEntity(controller == null ? pos : controller) instanceof KineticBlockEntityPhysicsAccess access
            && access.bnt$isTrackStraight();
    }

    public static Vec3 alignment(Level level, BlockPos controllerPos, BlockEntity node) {
        if (!(node instanceof KineticBlockEntityPhysicsAccess access)) {
            return Vec3.ZERO;
        }
        Vec3 offset = new Vec3(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        BlockState state = node.getBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)
            || !(level.getBlockEntity(controllerPos) instanceof KineticBlockEntityPhysicsAccess controller)
            || !controller.bnt$isTrackStraight()) {
            return offset;
        }
        Plane plane = plane(level, controllerPos);
        if (plane == null) {
            return offset;
        }
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        return offset.with(axis, plane.centre() - cell(node.getBlockPos(), state, axis));
    }

    public static int quarters(Level level, BlockPos controllerPos) {
        Plane plane = plane(level, controllerPos);
        return plane == null ? 0 : plane.quarters();
    }

    private static double cell(BlockPos pos, BlockState state, Axis axis) {
        return pos.get(axis) + 0.5 + BntCogwheelPairing.seamOffset(state).get(axis);
    }

    private static synchronized Plane plane(Level level, BlockPos controllerPos) {
        Map<BlockPos, Plane> planes = PLANES.computeIfAbsent(level, key -> new HashMap<>());
        long time = level.getGameTime();
        Plane cached = planes.get(controllerPos);
        if (cached != null && cached.time() == time && cached.epoch() == epoch) {
            return cached;
        }
        Plane measured = measure(level, controllerPos, time);
        if (measured == null) {
            planes.remove(controllerPos);
            return null;
        }
        if (planes.size() >= CACHE_LIMIT) {
            planes.clear();
        }
        planes.put(controllerPos.immutable(), measured);
        return measured;
    }

    private static Plane measure(Level level, BlockPos controllerPos, long time) {
        CogwheelChain chain = BntChains.chainAt(level, controllerPos);
        if (chain == null) {
            return null;
        }
        double[] span = span(level, controllerPos, chain, true);
        if (span[0] > span[1]) {
            span = span(level, controllerPos, chain, false);
        }
        double low = span[0];
        double high = span[1];
        if (low > high) {
            return null;
        }
        double pixels = (high - low) * 16.0;
        int quarters = BntCogwheelWidth.PAIR;
        for (int fitted = 1; fitted < BntCogwheelWidth.PAIR; fitted++) {
            if (BntCogwheelWidth.trackPixels(fitted) >= pixels - EPSILON) {
                quarters = fitted;
                break;
            }
        }
        return new Plane(time, epoch, (low + high) * 0.5, quarters);
    }

    private static double[] span(Level level, BlockPos controllerPos, CogwheelChain chain, boolean skipIgnored) {
        double[] span = {Double.MAX_VALUE, -Double.MAX_VALUE};
        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
            BlockPos nodePos = controllerPos.offset(node.localPos());
            BlockState state = level.getBlockState(nodePos);
            if (!state.hasProperty(BlockStateProperties.AXIS) || !BntCogwheelPairing.supports(state)
                || !(level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess access)
                || skipIgnored && access.bnt$isTrackFitIgnored()) {
                continue;
            }
            Axis axis = state.getValue(BlockStateProperties.AXIS);
            Vec3 offset = new Vec3(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
            double centre = cell(nodePos, state, axis) + offset.get(axis);
            double half = BntCogwheelWidth.trackPixels(BntCogwheelWidth.quarters(state)) / 32.0;
            span[0] = Math.min(span[0], centre - half);
            span[1] = Math.max(span[1], centre + half);
        }
        return span;
    }

    public static boolean ignored(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isTrackFitIgnored();
    }

    public static boolean ignore(Level level, BlockPos pos, boolean ignored) {
        if (!onTrack(level, pos) || !straight(level, pos) || ignored(level, pos) == ignored) {
            return false;
        }
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        for (BlockPos cogPos : partnerPos == null ? List.of(pos) : List.of(pos, partnerPos)) {
            if (level.getBlockEntity(cogPos) instanceof KineticBlockEntity kinetic && kinetic instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setTrackFitIgnored(ignored);
                kinetic.setChanged();
                kinetic.sendData();
            }
        }
        return true;
    }

    public static boolean set(Level level, BlockPos pos, boolean straight) {
        Set<BlockPos> positions = BntBeltTension.chainPositions(level, pos);
        if (!onTrack(level, pos) || straight(level, pos) == straight) {
            return false;
        }
        for (BlockPos nodePos : positions) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity kinetic && kinetic instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setTrackStraight(straight);
                kinetic.setChanged();
                kinetic.sendData();
            }
        }
        return true;
    }
}
