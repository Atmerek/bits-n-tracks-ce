package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BntBeltLinks {
    public static final int UNSET = 0;
    private static final int MAX_LINKS = 8192;

    private static final double MIN_STRETCH = 0.8;
    private static final double MAX_STRETCH = 1.25;

    private static final ThreadLocal<Double> LOOP_SCROLL = new ThreadLocal<>();
    private static final ThreadLocal<Double> LOOP_BELT = new ThreadLocal<>();
    private static final ThreadLocal<Double> LOOP_DRAWN = new ThreadLocal<>();
    private static final ThreadLocal<Map<Vec3, double[]>> LOOP_SPANS = ThreadLocal.withInitial(IdentityHashMap::new);

    private BntBeltLinks() {
    }

    public static double pitch() {
        return BntPhysicsTuning.getBeltLinkPitch();
    }

    public static double repeatLength() {
        return pitch() * BntPhysicsTuning.getBeltLinksPerTexture();
    }

    public static double length(int links) {
        return links * pitch();
    }

    public static double tautLength(List<PathedCogwheelNode> nodes) {
        return pathLength(nodes, false);
    }

    public static double liveTautLength(List<PathedCogwheelNode> nodes) {
        return pathLength(nodes, true);
    }

    private static double pathLength(List<PathedCogwheelNode> nodes, boolean live) {
        int count = nodes == null ? 0 : nodes.size();
        if (count < 2) {
            return 0.0;
        }
        Axis axis = BntChainGeometry.sharedAxis(nodes);
        if (axis == null) {
            return 0.0;
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        int[] sides = new int[count];
        if (live) {
            BntChainGeometry.fillLive(nodes, axis, xs, ys, radii);
        } else {
            BntChainGeometry.fill(nodes, axis, xs, ys, radii);
        }
        for (int i = 0; i < count; i++) {
            sides[i] = nodes.get(i).side();
        }

        double length = BntBeltSolver.tautLength(xs, ys, radii, sides);
        return Double.isFinite(length) && length < Double.MAX_VALUE ? length : 0.0;
    }

    public static int linksFor(double tautLength) {
        double pitch = pitch();
        if (tautLength <= 0.0 || pitch <= 0.0) {
            return UNSET;
        }
        return Mth.clamp((int)Math.ceil(tautLength / pitch - 1.0E-9), 2, MAX_LINKS);
    }

    public static double slackLength(float tension) {
        double taut = BntBeltTension.clamp(tension);
        double slack = 1.0 - taut;
        return BntPhysicsTuning.getBeltMaxSlackLinks() * pitch() * slack * slack
            - BntPhysicsTuning.getBeltPreTension() * taut;
    }

    public static double surplus(int links, float tension, double clearing) {
        if (links <= UNSET || clearing <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, length(links) + slackLength(tension) - clearing);
    }

    public static void setLoop(double belt, double scroll) {
        LOOP_BELT.set(belt);
        LOOP_SCROLL.set(scroll);
    }

    public static void clearLoop() {
        LOOP_BELT.remove();
        LOOP_SCROLL.remove();
        LOOP_DRAWN.remove();
        LOOP_SPANS.get().clear();
    }

    public static void beginSpans() {
        LOOP_DRAWN.remove();
        LOOP_SPANS.get().clear();
    }

    public static void addSpan(Vec3 to, double start, double length) {
        LOOP_SPANS.get().put(to, new double[]{start, length});
    }

    public static void setDrawnLength(double drawn) {
        LOOP_DRAWN.set(drawn);
    }

    public static double[] onLoop(Vec3 to, double offset, double length, double period) {
        double[] span = LOOP_SPANS.get().get(to);
        Double drawn = LOOP_DRAWN.get();
        Double scroll = LOOP_SCROLL.get();
        if (span == null || drawn == null || scroll == null || drawn <= 1.0E-6 || period <= 1.0E-6) {
            return new double[]{wrapScroll(offset, latchedRun(period)), length};
        }

        double run = closingRun(drawn, period);
        double scale = run / drawn;
        double folded = scroll % run;
        if (folded < 0.0) {
            folded += run;
        }
        return new double[]{folded + span[0] * scale, span[1] * scale};
    }

    private static double latchedRun(double period) {
        return Math.max(1L, (long)Math.floor(beltLength() / period)) * period;
    }

    private static double closingRun(double drawn, double period) {
        double latched = latchedRun(period);
        double stretch = latched / drawn;
        return stretch >= MIN_STRETCH && stretch <= MAX_STRETCH
            ? latched
            : Math.max(1L, Math.round(drawn / period)) * period;
    }

    public static double beltLength() {
        Double belt = LOOP_BELT.get();
        return belt == null ? 0.0 : belt;
    }

    public static double wrapScroll(double offset) {
        return wrapScroll(offset, beltRun());
    }

    public static double wrapScroll(double offset, double period) {
        Double scroll = LOOP_SCROLL.get();
        if (scroll == null || period <= 1.0E-6) {
            return offset;
        }
        double folded = scroll % period;
        if (folded < 0.0) {
            folded += period;
        }
        return offset - scroll + folded;
    }

    private static long loopRepeats() {
        double belt = beltLength();
        double repeat = repeatLength();
        return repeat <= 0.0 || belt <= repeat ? 1L : Math.max(1L, (long)Math.floor(belt / repeat));
    }

    public static double beltRun() {
        return loopRepeats() * repeatLength();
    }

    public static float squish() {
        double repeat = repeatLength();
        return repeat <= 0.0 ? 1.0F : (float)(1.0 / repeat);
    }

    public static double fitLength(Level level, BlockPos controllerPos) {
        List<PathedCogwheelNode> nodes = BntChains.beltOrder(level, controllerPos);
        if (nodes.size() < 2) {
            return 0.0;
        }
        try (BntRadiusProvider.Scope ignored = BntRadiusProvider.at(level, controllerPos)) {
            return tautLength(level, controllerPos, nodes, true);
        }
    }

    public static int relatch(Level level, BlockPos controllerPos, float tension) {
        List<PathedCogwheelNode> nodes = BntChains.beltOrder(level, controllerPos);
        if (nodes.size() < 2) {
            return UNSET;
        }
        try (BntRadiusProvider.Scope ignored = BntRadiusProvider.at(level, controllerPos)) {
            return linksFor(tautLength(level, controllerPos, nodes, true));
        }
    }

    public static Vec3 drawnCentre(Level level, BlockPos controllerPos, PathedCogwheelNode node) {
        return centre(level, controllerPos, node, false);
    }

    public static Vec3 parkedCentre(Level level, BlockPos controllerPos, PathedCogwheelNode node) {
        return centre(level, controllerPos, node, true);
    }

    private static Vec3 centre(Level level, BlockPos controllerPos, PathedCogwheelNode node, boolean parked) {
        BlockPos nodePos = controllerPos.offset(node.localPos());
        BlockState state = level.getBlockState(nodePos);
        Vec3 centre = nodePos.getCenter().add(BntCogwheelPairing.seamOffset(state));
        BlockEntity be = level.getBlockEntity(nodePos);
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            centre = centre.add(
                access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        }
        if (be instanceof KineticBlockEntity kinetic && parked) {
            centre = centre.add(BntSuspension.restDisplacement(kinetic));
        } else if (be instanceof KineticBlockEntity kinetic) {
            double drop = BntPhysicsEvents.getRawRenderExtension(kinetic, 1.0F);
            centre = centre.add(BntSuspension.displacement(kinetic, BntSuspension.hasPiece(kinetic) ? drop : Math.max(0.0, drop)));
        }
        return centre;
    }

    public static double drawnTautLength(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        return tautLength(level, controllerPos, nodes, false);
    }

    private static double tautLength(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes, boolean parked) {
        int count = nodes == null ? 0 : nodes.size();
        Axis axis = count < 2 || level == null ? null : BntChainGeometry.sharedAxis(nodes);
        if (axis == null) {
            return 0.0;
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        int[] sides = new int[count];
        for (int i = 0; i < count; i++) {
            Vec3 centre = centre(level, controllerPos, nodes.get(i), parked);
            xs[i] = BntChainGeometry.planarX(centre, axis);
            ys[i] = BntChainGeometry.planarY(centre, axis);
            radii[i] = BntChainGeometry.trackRadius(nodes.get(i));
            sides[i] = nodes.get(i).side();
        }

        double length = BntBeltSolver.tautLength(xs, ys, radii, sides);
        return Double.isFinite(length) && length < Double.MAX_VALUE ? length : 0.0;
    }

    public static int resolve(Level level, BlockPos controllerPos, float tension) {
        int latched = at(level, controllerPos);
        return latched > UNSET ? latched : relatch(level, controllerPos, tension);
    }

    public static void latchIfUnset(Level level, BlockPos controllerPos) {
        if (at(level, controllerPos) > UNSET) {
            return;
        }
        float tension = BntBeltTension.at(level, controllerPos);
        double fit = fitLength(level, controllerPos);
        int links = linksFor(fit);
        if (links > UNSET) {
            BntBeltTension.write(level, controllerPos, tension, links, fit);
        }
    }

    public static int at(Level level, BlockPos controllerPos) {
        return level != null
            && level.getBlockEntity(controllerPos) instanceof KineticBlockEntityPhysicsAccess access
            ? Math.max(UNSET, access.bnt$getBeltLinks())
            : UNSET;
    }

    public static double fitAt(Level level, BlockPos controllerPos) {
        return level != null
            && level.getBlockEntity(controllerPos) instanceof KineticBlockEntityPhysicsAccess access
            ? Math.max(0.0, access.bnt$getBeltFit())
            : 0.0;
    }

    public static int context() {
        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        return level == null || origin == null ? UNSET : at(level, origin);
    }

    public static int contextOrEstimate(List<PathedCogwheelNode> nodes) {
        int latched = context();
        return latched > UNSET ? latched : linksFor(tautLength(nodes));
    }
}
