package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.block.IExclusiveCogwheelChainBlock;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class BntChainEngagement {
    private static final float DRIVE_TOLERANCE = 1.0E-3F;

    private BntChainEngagement() {
    }

    public static Vec3 alignmentDisplacement(Level level, BlockPos worldPos) {
        if (level == null) {
            return Vec3.ZERO;
        }
        BlockEntity be = level.getBlockEntity(worldPos);
        if (!(be instanceof KineticBlockEntityPhysicsAccess access)) {
            return Vec3.ZERO;
        }

        double y = access.bnt$getAlignmentOffsetY();
        if (HiddenCogwheelCompat.isHiddenCogwheel(be.getBlockState())) {
            y += HiddenCogwheelCompat.getManualVisualVerticalOffset(be);
            y -= quantise(HiddenCogwheelCompat.getVisualDrop(be, 1.0F));
        }
        return new Vec3(access.bnt$getAlignmentOffsetX(), y, access.bnt$getAlignmentOffsetZ());
    }

    private static double quantise(double travel) {
        return Math.round(travel * 16.0) / 16.0;
    }

    public static Direction routeSide(Level level, BlockPos worldPos) {
        if (level == null || !(level.getBlockEntity(worldPos) instanceof KineticBlockEntityPhysicsAccess access)) {
            return null;
        }
        int ordinal = access.bnt$getTrackRouteSide();
        return ordinal < 0 || ordinal >= Direction.values().length ? null : Direction.values()[ordinal];
    }

    public static double[] signature(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        double[] signature = new double[nodes.size() * 5];
        for (int i = 0; i < nodes.size(); i++) {
            BlockPos worldPos = controllerPos.offset(nodes.get(i).localPos());
            BlockEntity be = level == null ? null : level.getBlockEntity(worldPos);
            if (be instanceof KineticBlockEntityPhysicsAccess access) {
                signature[i * 5] = access.bnt$getAlignmentOffsetX();
                signature[i * 5 + 1] = access.bnt$getAlignmentOffsetY();
                signature[i * 5 + 2] = access.bnt$getAlignmentOffsetZ();
                signature[i * 5 + 3] = access.bnt$getTrackRouteSide();
                signature[i * 5 + 4] = HiddenCogwheelCompat.isHiddenCogwheel(be.getBlockState()) ? 1.0 : 0.0;
            } else {
                signature[i * 5 + 3] = -1.0;
            }
        }
        return signature;
    }

    public static boolean stillHolds(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes,
                                     BntChainGeometry.Layout layout) {
        Function<BlockPos, Vec3> previous = BntChainMotion.swapDisplacementSource(
            localPos -> alignmentDisplacement(level, controllerPos.offset(localPos)));
        Function<BlockPos, Direction> previousRoutes = BntChainMotion.swapRouteSource(
            localPos -> routeSide(level, controllerPos.offset(localPos)));

        try {
            return BntChainGeometry.stillHolds(nodes, layout);
        } finally {
            BntChainMotion.swapDisplacementSource(previous);
            BntChainMotion.swapRouteSource(previousRoutes);
        }
    }

    public static BntChainGeometry.Layout layout(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        Function<BlockPos, Vec3> previous = BntChainMotion.swapDisplacementSource(
            localPos -> alignmentDisplacement(level, controllerPos.offset(localPos)));
        Function<BlockPos, Direction> previousRoutes = BntChainMotion.swapRouteSource(
            localPos -> routeSide(level, controllerPos.offset(localPos)));

        try {
            return BntChainGeometry.resolve(nodes);
        } finally {
            BntChainMotion.swapDisplacementSource(previous);
            BntChainMotion.swapRouteSource(previousRoutes);
        }
    }

    public static boolean[] compute(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        return engagement(layout(level, controllerPos, nodes), nodes.size());
    }

    public static boolean[] engagement(BntChainGeometry.Layout layout, int count) {
        boolean[] engaged = new boolean[count];
        if (layout == null) {
            Arrays.fill(engaged, true);
            return engaged;
        }
        for (int index : layout.sequence()) {
            engaged[index] = true;
        }
        return engaged;
    }

    public static Map<BlockPos, Integer> snapshot(Level level, BlockPos pos) {
        BlockPos controllerPos = controllerPos(level, pos);
        CogwheelChain chain = chain(level, controllerPos);
        if (controllerPos == null || chain == null) {
            return Map.of();
        }

        List<PathedCogwheelNode> nodes = chain.getChainPathCogwheelNodes();
        BntChainGeometry.Layout layout = layout(level, controllerPos, nodes);
        boolean[] engaged = engagement(layout, nodes.size());
        int reference = 1;
        for (int i = 0; i < nodes.size(); i++) {
            if (engaged[i]) {
                reference = sideOf(layout, nodes, i);
                break;
            }
        }

        Map<BlockPos, Integer> result = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            result.put(controllerPos.offset(nodes.get(i).localPos()),
                engaged[i] ? sideOf(layout, nodes, i) * reference : 0);
        }
        return result;
    }

    private static int sideOf(BntChainGeometry.Layout layout, List<PathedCogwheelNode> nodes, int index) {
        return layout == null ? nodes.get(index).side() : layout.sides()[index];
    }

    public static Boolean engagementAt(Level level, BlockPos pos) {
        Integer state = snapshot(level, pos).get(pos);
        return state == null ? null : state != 0;
    }

    public static void refresh(Level level, BlockPos pos, Map<BlockPos, Integer> before) {
        Map<BlockPos, Integer> after = snapshot(level, pos);
        if (after.equals(before)) {
            return;
        }

        rebuild(level, after.isEmpty() ? before.keySet() : after.keySet());
    }

    public static void rebuild(Level level, BlockPos pos) {
        Map<BlockPos, Integer> nodes = snapshot(level, pos);
        rebuild(level, nodes.isEmpty() ? Set.of(pos) : nodes.keySet());
    }

    public static void rebuild(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        Set<BlockPos> positions = new HashSet<>();
        for (PathedCogwheelNode node : nodes) {
            positions.add(controllerPos.offset(node.localPos()));
        }
        rebuild(level, positions);
    }

    public static boolean drivesTogether(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes,
                                         boolean[] engaged) {
        boolean seen = false;
        float reference = 0.0F;
        for (int i = 0; i < nodes.size(); i++) {
            if (!engaged[i]) {
                continue;
            }
            PathedCogwheelNode node = nodes.get(i);
            if (!(level.getBlockEntity(controllerPos.offset(node.localPos())) instanceof KineticBlockEntity kinetic)) {
                continue;
            }

            float carried = kinetic.getTheoreticalSpeed() * node.sideFactor();
            if (!seen) {
                seen = true;
                reference = carried;
            } else if (Math.abs(carried - reference) > DRIVE_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static void rebuild(Level level, Set<BlockPos> nodes) {
        for (BlockPos nodePos : nodes) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity kinetic) {
                kinetic.detachKinetics();
            }
        }
        for (BlockPos nodePos : nodes) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity kinetic) {
                kinetic.removeSource();
                kinetic.updateSpeed = true;
                kinetic.setChanged();
                kinetic.sendData();
            }
        }
    }

    public static CogwheelChainBehaviour chainBehaviour(BlockEntity be) {
        return be instanceof SmartBlockEntity smartBe
            ? (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE)
            : null;
    }

    public static boolean sharesChain(BlockEntity from, BlockEntity to) {
        BlockPos fromController = controllerPos(chainBehaviour(from));
        return fromController != null && fromController.equals(controllerPos(chainBehaviour(to)));
    }

    public static boolean meshingWouldFightChain(BlockEntity from, BlockEntity to) {
        if (!isChainCogwheel(from) || !isChainCogwheel(to)) {
            return false;
        }
        return controllerPos(chainBehaviour(from)) != null || controllerPos(chainBehaviour(to)) != null;
    }

    public static boolean sharesShaft(BlockEntity from, BlockEntity to) {
        BlockState stateFrom = from.getBlockState();
        BlockState stateTo = to.getBlockState();
        if (!stateFrom.hasProperty(BlockStateProperties.AXIS) || !stateTo.hasProperty(BlockStateProperties.AXIS)) {
            return false;
        }

        Axis axis = (Axis)stateFrom.getValue(BlockStateProperties.AXIS);
        if (axis != stateTo.getValue(BlockStateProperties.AXIS)) {
            return false;
        }

        BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
        int alongAxis = axis.choose(diff.getX(), diff.getY(), diff.getZ());
        return alongAxis != 0 && diff.distManhattan(BlockPos.ZERO) == Math.abs(alongAxis);
    }

    private static boolean isChainCogwheel(BlockEntity be) {
        return be != null && be.getBlockState().getBlock() instanceof IExclusiveCogwheelChainBlock;
    }

    public static boolean isEngaged(CogwheelChainBehaviour behaviour) {
        if (behaviour == null || behaviour.getBlockEntity() == null) {
            return true;
        }

        Level level = behaviour.getLevel();
        BlockPos controllerPos = controllerPos(behaviour);
        CogwheelChain chain = chainOf(behaviour);
        if (level == null || controllerPos == null || !(chain instanceof BntChainGeometryRefresh engagement)) {
            return true;
        }
        return engagement.bnt$isNodeEngaged(
            level, controllerPos, behaviour.getBlockEntity().getBlockPos().subtract(controllerPos));
    }

    public static BlockPos controllerPos(CogwheelChainBehaviour behaviour) {
        if (behaviour == null || behaviour.getBlockEntity() == null) {
            return null;
        }
        if (behaviour.isController()) {
            return behaviour.getBlockEntity().getBlockPos();
        }
        Vec3i offset = behaviour.getControllerOffset();
        return offset == null ? null : behaviour.getBlockEntity().getBlockPos().offset(offset);
    }

    private static CogwheelChain chainOf(CogwheelChainBehaviour behaviour) {
        CogwheelChain direct = behaviour.getControlledChain();
        if (direct != null) {
            return direct;
        }

        Level level = behaviour.getLevel();
        BlockPos controllerPos = controllerPos(behaviour);
        if (level == null || controllerPos == null) {
            return null;
        }
        CogwheelChainBehaviour controller = behaviour(level, controllerPos);
        return controller == null ? null : controller.getControlledChain();
    }

    private static BlockPos controllerPos(Level level, BlockPos pos) {
        CogwheelChainBehaviour behaviour = behaviour(level, pos);
        if (behaviour == null) {
            return null;
        }
        if (behaviour.getControlledChain() != null) {
            return pos;
        }
        Vec3i offset = behaviour.getControllerOffset();
        return offset == null ? null : pos.offset(offset);
    }

    private static CogwheelChain chain(Level level, BlockPos controllerPos) {
        CogwheelChainBehaviour behaviour = controllerPos == null ? null : behaviour(level, controllerPos);
        return behaviour == null ? null : behaviour.getControlledChain();
    }

    private static CogwheelChainBehaviour behaviour(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SmartBlockEntity smartBe
            ? (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE)
            : null;
    }
}
