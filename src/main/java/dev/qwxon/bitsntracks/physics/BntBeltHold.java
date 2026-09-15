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
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltPath;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The belt is a loop of fixed length, and it pulls back when the wheels ask for more path than it has.
 * The length it is fitted to is measured where the wheels are drawn, so a machine standing still asks for
 * exactly what it was fitted with, and only terrain that lengthens the path makes the belt carry anything.
 */
public final class BntBeltHold {
    private static final Logger LOG = LoggerFactory.getLogger("bits_n_tracks");
    private static final double FLAT = 1.0E-6;
    private static final double GRIP_SLOPE = 0.05;
    private static final double RELAX = 0.6;
    private static final double STEP = 1.0 / 8.0;
    private static final int MAX_CLIMB_PROBES = 16;

    private BntBeltHold() {
    }

    /** Lift the belt puts on one wheel, solved once per tick for the whole loop. */
    public static double at(Level level, KineticBlockEntity wheel) {
        if (!BntPhysicsTuning.isBeltHoldEnabled()
            || !(wheel instanceof KineticBlockEntityPhysicsAccess access)
            || level == null) {
            return 0.0;
        }

        long now = level.getGameTime();
        if (access.bnt$getBeltHoldTick() == now) {
            return access.bnt$getBeltHold();
        }

        access.bnt$setBeltHold(now, access.bnt$getBeltHold());
        BlockPos controllerPos = controllerPos(wheel);
        if (controllerPos != null) {
            solve(level, controllerPos, now);
        }
        return access.bnt$getBeltHold();
    }

    private static void solve(Level level, BlockPos controllerPos, long now) {
        List<PathedCogwheelNode> nodes = beltOrder(level, controllerPos);
        int count = nodes.size();
        Axis axis = BntChainGeometry.sharedAxis(nodes);
        boolean solvable = count >= 3 && axis != null && axis != Axis.Y;
        for (int i = 0; i < count; i++) {
            if (level.getBlockEntity(controllerPos.offset(nodes.get(i).localPos()))
                instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setBeltHold(now,
                    solvable ? access.bnt$getBeltHold() : Math.max(0.0, access.bnt$getBeltHold() - STEP));
            }
        }
        if (!solvable) {
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
        Vec3[] centres = new Vec3[count];
        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        double[] drops = new double[count];
        int[] sides = new int[count];
        double averageY = 0.0;

        double[] holds = new double[count];
        boolean[] powered = new boolean[count];
        for (int i = 0; i < count; i++) {
            PathedCogwheelNode node = nodes.get(i);
            BlockEntity be = level.getBlockEntity(controllerPos.offset(node.localPos()));
            if (be instanceof KineticBlockEntity kinetic) {
                drops[i] = Math.max(0.0, BntPhysicsEvents.getRawRenderExtension(kinetic, 1.0F));
            }
            if (be instanceof KineticBlockEntityPhysicsAccess access) {
                holds[i] = access.bnt$getBeltHold();
                powered[i] = access.bnt$isPhysicsEnabled();
            }
            centres[i] = BntBeltLinks.drawnCentre(level, controllerPos, node).add(0.0, holds[i], 0.0);
            xs[i] = BntChainGeometry.planarX(centres[i], axis);
            ys[i] = BntChainGeometry.planarY(centres[i], axis);
            radii[i] = BntChainGeometry.trackRadius(node);
            sides[i] = node.side();
            averageY += centres[i].y;
        }
        averageY /= count;

        float tension = BntBeltTension.at(level, controllerPos);
        double give = Math.max(0.0, BntBeltLinks.slackLength(tension));
        double cap = BntPhysicsTuning.getBeltMaxHold();
        int links = BntBeltLinks.at(level, controllerPos);
        double path = BntBeltSolver.tautLength(xs, ys, radii, sides)
            + climb(controllerPos, nodes, axis, centres, xs, ys, radii, sides);
        double fit = level.getBlockEntity(controllerPos) instanceof KineticBlockEntityPhysicsAccess seat
            ? seat.bnt$getBeltFit()
            : 0.0;
        double allowance = fit > FLAT ? fit + give : BntBeltLinks.length(links) + BntBeltLinks.pitch() + give;
        double excess = links <= BntBeltLinks.UNSET || !Double.isFinite(path) || path >= Double.MAX_VALUE
            ? 0.0
            : path - allowance;

        double[] gradient = new double[count];
        double weight = 0.0;
        for (int i = 0; i < count; i++) {
            if (centres[i].y > averageY || !powered[i]) {
                continue;
            }
            Vec3 toPrevious = centres[(i - 1 + count) % count].subtract(centres[i]);
            Vec3 toNext = centres[(i + 1) % count].subtract(centres[i]);
            if (toPrevious.lengthSqr() < FLAT || toNext.lengthSqr() < FLAT) {
                continue;
            }
            double slope = -(toPrevious.normalize().y + toNext.normalize().y);
            if (slope > -GRIP_SLOPE) {
                continue;
            }
            gradient[i] = slope;
            weight += slope * slope;
        }

        StringBuilder report = BntPhysicsTuning.isBeltDebugLogging() && now % 20L == 0L ? new StringBuilder() : null;
        for (int i = 0; i < count; i++) {
            double step = weight < FLAT || gradient[i] >= 0.0
                ? -STEP
                : Mth.clamp(RELAX * excess * -gradient[i] / weight, -STEP, STEP);
            double lift = Mth.clamp(holds[i] + step, 0.0, Math.min(cap, drops[i]));
            if (report != null) {
                report.append(String.format(" [%d r%.2f drop%.3f grad%.2f lift%.3f%s]",
                    i, radii[i], drops[i], gradient[i], lift, centres[i].y > averageY ? " top" : ""));
            }
            BlockEntity be = level.getBlockEntity(controllerPos.offset(nodes.get(i).localPos()));
            if (be instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setBeltHold(now, lift);
            }
        }

        if (report != null) {
            LOG.info("belt {} {} tension={} give={} links={} path={} allow={} excess={}{}",
                level.isClientSide ? "client" : "server", controllerPos,
                String.format("%.2f", tension), String.format("%.3f", give), links,
                String.format("%.3f", path), String.format("%.3f", allowance),
                String.format("%.3f", excess), report);
        }
    }

    /** Path the runs gain climbing the ground under them. */
    private static double climb(
        BlockPos controllerPos, List<PathedCogwheelNode> nodes, Axis axis,
        Vec3[] centres, double[] xs, double[] ys, double[] radii, int[] sides
    ) {
        if (!BntPhysicsTuning.isBeltDrapeEnabled()) {
            return 0.0;
        }

        int count = nodes.size();
        double averageY = 0.0;
        for (int i = 0; i < count; i++) {
            averageY += centres[i].y;
        }
        averageY /= count;

        Vec3 base = Vec3.atLowerCornerOf(controllerPos);
        double total = 0.0;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            if (next == i) {
                continue;
            }
            double[] run = BntBeltPath.tangent(
                xs[i], ys[i], sides[i] * radii[i], xs[next], ys[next], sides[next] * radii[next]);
            if (run == null || run[0] < 1.0E-4) {
                continue;
            }

            Vec3 start = BntBeltPath.fromPlanar(xs[i] + run[1], ys[i] + run[2],
                BntBeltPath.axisCoord(centres[i], axis), axis);
            Vec3 end = BntBeltPath.fromPlanar(xs[next] + run[3], ys[next] + run[4],
                BntBeltPath.axisCoord(centres[next], axis), axis);
            if ((start.y + end.y) * 0.5 > averageY) {
                continue;
            }

            Vec3 along = end.subtract(start);
            int probes = Math.min(BntBeltDrape.probeCount(run[0]), MAX_CLIMB_PROBES);
            double rest = (BntBeltDrape.restOffset(nodes.get(i)) + BntBeltDrape.restOffset(nodes.get(next))) * 0.5;
            double[] shape = BntBeltDrape.profile(start.subtract(base), along, probes, 0.0, rest, true);

            double stride = run[0] / probes;
            for (int probe = 0; probe < probes; probe++) {
                double rise = shape[probe + 1] - shape[probe];
                total += Math.sqrt(stride * stride + rise * rise) - stride;
            }
        }
        return total;
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
