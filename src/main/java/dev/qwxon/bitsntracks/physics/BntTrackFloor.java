package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltDrape;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltLinks;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltSolver;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** The track surface a wheel stands on, bridging what a taut run crosses. */
public final class BntTrackFloor {
    private static final double FLAT = 1.0E-6;
    private static final int MAX_PROBES = 24;

    private BntTrackFloor() {
    }

    /** Height the track holds this wheel above the ground under it. */
    public static double at(Level level, KineticBlockEntity wheel) {
        if (!BntPhysicsTuning.isBeltDrapeEnabled()
            || !(wheel instanceof KineticBlockEntityPhysicsAccess access)
            || level == null) {
            return 0.0;
        }

        long now = level.getGameTime();
        if (access.bnt$getTrackLiftTick() == now) {
            return access.bnt$getTrackLift();
        }

        access.bnt$setTrackLift(now, 0.0);
        BlockPos controllerPos = controllerPos(wheel);
        if (controllerPos != null) {
            solve(level, controllerPos, now);
        }
        return access.bnt$getTrackLift();
    }

    private static void solve(Level level, BlockPos controllerPos, long now) {
        List<PathedCogwheelNode> nodes = beltOrder(level, controllerPos);
        int count = nodes.size();
        for (int i = 0; i < count; i++) {
            if (level.getBlockEntity(controllerPos.offset(nodes.get(i).localPos()))
                instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setTrackLift(now, 0.0);
            }
        }
        if (count < 3) {
            return;
        }

        Axis axis = BntChainGeometry.sharedAxis(nodes);
        if (axis == null || axis == Axis.Y) {
            return;
        }

        Level heldLevel = BntRadiusProvider.level();
        BlockPos heldOrigin = BntRadiusProvider.origin();
        try {
            BntRadiusProvider.setLevel(level);
            BntRadiusProvider.setOrigin(controllerPos);
            apply(level, controllerPos, nodes, axis, now);
        } finally {
            BntRadiusProvider.setLevel(heldLevel);
            BntRadiusProvider.setOrigin(heldOrigin);
        }
    }

    private static void apply(
        Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes, Axis axis, long now
    ) {
        int count = nodes.size();
        Vec3[] seats = new Vec3[count];
        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        double[] heights = new double[count];
        int[] sides = new int[count];
        boolean[] powered = new boolean[count];
        double averageY = 0.0;

        for (int i = 0; i < count; i++) {
            PathedCogwheelNode node = nodes.get(i);
            Vec3 centre = BntBeltLinks.drawnCentre(level, controllerPos, node);
            radii[i] = BntChainGeometry.trackRadius(node);
            seats[i] = centre.subtract(0.0, radii[i], 0.0);
            xs[i] = BntChainGeometry.planarX(centre, axis);
            ys[i] = BntChainGeometry.planarY(centre, axis);
            sides[i] = node.side();
            powered[i] = level.getBlockEntity(controllerPos.offset(node.localPos()))
                instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled();
            heights[i] = centre.y;
            averageY += centre.y;
        }
        averageY /= count;

        double path = BntBeltSolver.tautLength(xs, ys, radii, sides);
        if (!Double.isFinite(path) || path >= Double.MAX_VALUE || path < FLAT) {
            return;
        }
        double surplus = BntBeltLinks.surplus(
            BntBeltLinks.at(level, controllerPos), BntBeltTension.at(level, controllerPos), path);

        Vec3 base = Vec3.atLowerCornerOf(controllerPos);
        for (int i = 0; i < count; i++) {
            int previous = (i - 1 + count) % count;
            int next = (i + 1) % count;
            if (!powered[i] || previous == next || previous == i || next == i
                || heights[i] > averageY + FLAT || heights[previous] > averageY + FLAT || heights[next] > averageY + FLAT) {
                continue;
            }

            Vec3 from = seats[previous];
            Vec3 along = seats[next].subtract(from);
            double span = along.length();
            if (span < FLAT) {
                continue;
            }

            double reach = seats[i].subtract(from).dot(along) / (span * span);
            if (reach <= 0.0 || reach >= 1.0) {
                continue;
            }

            int probes = Mth.clamp((int)Math.round(span / BntPhysicsTuning.getBeltNodeSpacing()), 2, MAX_PROBES);
            double rest = (BntBeltDrape.restOffset(nodes.get(previous)) + BntBeltDrape.restOffset(nodes.get(next))) * 0.5;
            double sag = BntBeltTension.sagFromSurplus(span, surplus * span / path);
            double[] shape = BntBeltDrape.profile(from.subtract(base), along, probes, sag, rest, true);

            double sample = reach * probes;
            int lower = Mth.clamp((int)Math.floor(sample), 0, probes);
            int upper = Math.min(lower + 1, probes);
            double carried = Mth.lerp(sample - lower, shape[lower], shape[upper]);
            double lift = Mth.lerp(reach, from.y, seats[next].y) + carried - seats[i].y;
            if (lift <= FLAT) {
                continue;
            }

            if (level.getBlockEntity(controllerPos.offset(nodes.get(i).localPos()))
                instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setTrackLift(now, Math.min(lift, BntPhysicsTuning.getBeltMaxHold()));
            }
        }
    }

    private static List<PathedCogwheelNode> beltOrder(Level level, BlockPos controllerPos) {
        if (!(level.getBlockEntity(controllerPos) instanceof SmartBlockEntity smart)
            || !(smart.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour)) {
            return List.of();
        }
        CogwheelChain chain = behaviour.getControlledChain();
        if (chain == null) {
            return List.of();
        }
        List<PathedCogwheelNode> latched = chain instanceof BntChainGeometryRefresh refreshable
            ? refreshable.bnt$latchedBeltOrder()
            : List.of();
        return latched.size() >= 2 ? latched : chain.getChainPathCogwheelNodes();
    }

    private static BlockPos controllerPos(KineticBlockEntity wheel) {
        CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)wheel.getBehaviour(CogwheelChainBehaviour.TYPE);
        if (behaviour == null || !behaviour.isPartOfChain()) {
            return null;
        }
        if (behaviour.getControlledChain() != null) {
            return wheel.getBlockPos();
        }
        return behaviour.getControllerOffset() == null ? null : wheel.getBlockPos().offset(behaviour.getControllerOffset());
    }
}
