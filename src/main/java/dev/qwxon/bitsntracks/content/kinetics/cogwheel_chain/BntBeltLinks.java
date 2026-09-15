package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The loop as a whole number of links of fixed pitch. */
public final class BntBeltLinks {
    public static final int UNSET = 0;
    private static final int MAX_LINKS = 8192;

    private static final ThreadLocal<Double> LOOP_SCROLL = new ThreadLocal<>();
    private static final ThreadLocal<Double> LOOP_BELT = new ThreadLocal<>();

    private BntBeltLinks() {
    }

    public static double pitch() {
        return BntPhysicsTuning.getBeltLinkPitch();
    }

    /** Path length one pass of the texture covers. */
    public static double repeatLength() {
        return pitch() * BntPhysicsTuning.getBeltLinksPerTexture();
    }

    public static double length(int links) {
        return links * pitch();
    }

    /** Taut path round the wheels where they rest. */
    public static double tautLength(List<PathedCogwheelNode> nodes) {
        return pathLength(nodes, false);
    }

    /** Taut path round the wheels where they are now. */
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

    /** Whole links a loop is built from, before any slack the tensioner lets out. */
    public static int linksFor(double tautLength) {
        double pitch = pitch();
        if (tautLength <= 0.0 || pitch <= 0.0) {
            return UNSET;
        }
        return Mth.clamp((int)Math.floor(tautLength / pitch + 1.0E-9), 2, MAX_LINKS);
    }

    /** Slack from the dial, squared so hang moves evenly with the lever, less the pull a taut loop keeps. */
    public static double slackLength(float tension) {
        double taut = BntBeltTension.clamp(tension);
        double slack = 1.0 - taut;
        return BntPhysicsTuning.getBeltMaxSlackLinks() * pitch() * slack * slack
            - BntPhysicsTuning.getBeltPreTension() * taut;
    }

    /** Length the loop carries over the path it has to clear, never negative. */
    public static double surplus(int links, float tension, double clearing) {
        if (links <= UNSET) {
            return 0.0;
        }
        return Math.max(0.0, length(links) + slackLength(tension) - clearing);
    }

    /** Belt length and scroll for the chain being rendered. */
    public static void setLoop(double belt, double scroll) {
        LOOP_BELT.set(belt);
        LOOP_SCROLL.set(scroll);
    }

    public static void clearLoop() {
        LOOP_BELT.remove();
        LOOP_SCROLL.remove();
    }

    /** Length of belt in the loop, which only moves when the lever does. */
    public static double beltLength() {
        Double belt = LOOP_BELT.get();
        return belt == null ? 0.0 : belt;
    }

    /** Folds the render time scroll into a whole number of texture passes. */
    public static double wrapScroll(double offset) {
        return wrapScroll(offset, beltRun());
    }

    /** Folds against a period the caller repeats over. */
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

    /** Whole texture passes the loop is built from, rounded down off the belt length. */
    private static long loopRepeats() {
        double belt = beltLength();
        double repeat = repeatLength();
        return repeat <= 0.0 || belt <= repeat ? 1L : Math.max(1L, (long)Math.floor(belt / repeat));
    }

    /** Length those passes take up. */
    public static double beltRun() {
        return loopRepeats() * repeatLength();
    }

    /** Texture scale, one sheet pass per repeatLength of belt. */
    public static float squish() {
        double repeat = repeatLength();
        return repeat <= 0.0 ? 1.0F : (float)(1.0 / repeat);
    }

    /** Rebuilds the loop for a tension and returns its link count. */
    /** Taut path the loop is fitted to, measured where the wheels are drawn. */
    public static double fitLength(Level level, BlockPos controllerPos) {
        List<PathedCogwheelNode> nodes = beltOrder(level, controllerPos);
        if (nodes.size() < 2) {
            return 0.0;
        }
        Level heldLevel = BntRadiusProvider.level();
        BlockPos heldOrigin = BntRadiusProvider.origin();
        try {
            BntRadiusProvider.setLevel(level);
            BntRadiusProvider.setOrigin(controllerPos);
            return drawnTautLength(level, controllerPos, nodes);
        } finally {
            BntRadiusProvider.setLevel(heldLevel);
            BntRadiusProvider.setOrigin(heldOrigin);
        }
    }

    public static int relatch(Level level, BlockPos controllerPos, float tension) {
        List<PathedCogwheelNode> nodes = beltOrder(level, controllerPos);
        if (nodes.size() < 2) {
            return UNSET;
        }
        Level heldLevel = BntRadiusProvider.level();
        BlockPos heldOrigin = BntRadiusProvider.origin();
        try {
            BntRadiusProvider.setLevel(level);
            BntRadiusProvider.setOrigin(controllerPos);
            return linksFor(drawnTautLength(level, controllerPos, nodes));
        } finally {
            BntRadiusProvider.setLevel(heldLevel);
            BntRadiusProvider.setOrigin(heldOrigin);
        }
    }

    /** Where a wheel is drawn, its visual seat less the drop terrain pulls it down to. */
    public static Vec3 drawnCentre(Level level, BlockPos controllerPos, PathedCogwheelNode node) {
        BlockPos nodePos = controllerPos.offset(node.localPos());
        BlockState state = level.getBlockState(nodePos);
        Vec3 centre = nodePos.getCenter()
            .add(0.0, CogwheelSizeHelper.getVisualVerticalOffset(state.getBlock()), 0.0)
            .add(BntCogwheelPairing.seamOffset(state));
        BlockEntity be = level.getBlockEntity(nodePos);
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            centre = centre.add(
                access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        }
        if (be instanceof KineticBlockEntity kinetic) {
            centre = centre.subtract(0.0, Math.max(0.0, BntPhysicsEvents.getRawRenderExtension(kinetic, 1.0F)), 0.0);
        }
        return centre;
    }

    /** Taut path round the wheels where they are drawn, which is what the belt is fitted to. */
    public static double drawnTautLength(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
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
            Vec3 centre = drawnCentre(level, controllerPos, nodes.get(i));
            xs[i] = BntChainGeometry.planarX(centre, axis);
            ys[i] = BntChainGeometry.planarY(centre, axis);
            radii[i] = BntChainGeometry.trackRadius(nodes.get(i));
            sides[i] = nodes.get(i).side();
        }

        double length = BntBeltSolver.tautLength(xs, ys, radii, sides);
        return Double.isFinite(length) && length < Double.MAX_VALUE ? length : 0.0;
    }

    /** Links the loop settles at, worked out fresh when none is latched. */
    public static int resolve(Level level, BlockPos controllerPos, float tension) {
        int latched = at(level, controllerPos);
        return latched > UNSET ? latched : relatch(level, controllerPos, tension);
    }

    /** Writes a link count onto a chain that has none, from the lazy tick only. */
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

    public static int at(Level level, BlockPos controllerPos) {
        return level != null
            && level.getBlockEntity(controllerPos) instanceof KineticBlockEntityPhysicsAccess access
            ? Math.max(UNSET, access.bnt$getBeltLinks())
            : UNSET;
    }

    /** Link count of the chain at the level and origin set by the caller. */
    public static int context() {
        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        return level == null || origin == null ? UNSET : at(level, origin);
    }

    /** Link count for the loop being drawn, worked out fresh when none has been latched. */
    public static int contextOrEstimate(List<PathedCogwheelNode> nodes) {
        int latched = context();
        return latched > UNSET ? latched : linksFor(tautLength(nodes));
    }
}
