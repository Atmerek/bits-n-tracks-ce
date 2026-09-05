package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BntBeltDrape {
    private static final double PROBE_ABOVE = 1.25;
    private static final double PROBE_BELOW = 0.5;
    private static final double NO_SURFACE = -1.0E9;
    private static final int MAX_PROBES = 64;

    private BntBeltDrape() {
    }

    /** Point count for a run, from beltNodeSpacing, clamped to 2 up to 64. */
    public static int probeCount(double span) {
        double spacing = BntPhysicsTuning.getBeltNodeSpacing();
        return Mth.clamp((int)Math.round(span / spacing), 2, MAX_PROBES);
    }

    /** Seam shift of a wide cogwheel half, which the renderer adds back. */
    public static Vec3 seamOffset(PathedCogwheelNode node) {
        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        if (level == null || origin == null) {
            return Vec3.ZERO;
        }
        return BntCogwheelPairing.seamOffset(level.getBlockState(origin.offset(node.localPos())));
    }

    /** Track radius minus collision radius, the depth a flat run rests at. */
    public static double restOffset(PathedCogwheelNode node) {
        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        if (level == null || origin == null) {
            return 0.0;
        }
        double collision = CogwheelSizeHelper.getRadius(level.getBlockState(origin.offset(node.localPos())).getBlock());
        return BntChainGeometry.trackRadius(node) - collision;
    }

    /** Run height against the straight line between its wheels, per sample. */
    public static double[] profile(
        Vec3 runStart, Vec3 along, int probes, double sag, float tension, double restOffset, boolean underside
    ) {
        double[] ground = new double[probes + 1];
        Arrays.fill(ground, NO_SURFACE);

        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        boolean probing = underside && level != null && origin != null && BntPhysicsTuning.isBeltDrapeEnabled();

        SubLevel subLevel = null;
        Pose3dc pose = null;
        if (probing) {
            Vec3 base = Vec3.atLowerCornerOf(origin);
            subLevel = Sable.HELPER.getContaining(level, base.add(runStart));
            pose = subLevel == null ? null : subLevel.logicalPose();
            double clearance = BntPhysicsTuning.getBeltSurfaceClearance();
            for (int probe = 1; probe < probes; probe++) {
                Vec3 chord = runStart.add(along.scale((double)probe / probes));
                double raw = surfaceOffset(level, base, pose, subLevel, chord, sag, restOffset);
                ground[probe] = raw <= NO_SURFACE ? NO_SURFACE : raw - restOffset + clearance;
            }
        }

        double[] anchored = ground.clone();
        anchored[0] = 0.0;
        anchored[probes] = 0.0;
        double[] taut = upperHull(anchored);

        double blend = BntBeltTension.clamp(tension);
        double[] offsets = new double[probes + 1];
        for (int probe = 1; probe < probes; probe++) {
            double hanging = taut[probe] - BntBeltTension.droopAt((double)probe / probes, sag);
            double slack = Math.max(hanging, ground[probe]);
            offsets[probe] = Mth.lerp(blend, slack, taut[probe]);
        }
        return offsets;
    }

    /** Ground height against the straight line, positive when above it. */
    private static double surfaceOffset(
        Level level, Vec3 base, Pose3dc pose, SubLevel subLevel, Vec3 chord, double sag, double restOffset
    ) {
        double top = PROBE_ABOVE;
        double bottom = Math.min(-(sag + PROBE_BELOW), restOffset - PROBE_BELOW);
        Vec3 blockTop = base.add(chord).add(0.0, top, 0.0);
        Vec3 blockBottom = base.add(chord).add(0.0, bottom, 0.0);
        Vec3 worldTop = pose == null ? blockTop : pose.transformPosition(blockTop);
        Vec3 worldBottom = pose == null ? blockBottom : pose.transformPosition(blockBottom);

        double reach = worldTop.distanceTo(worldBottom);
        if (reach < 1.0E-9) {
            return NO_SURFACE;
        }

        ClipContext clipContext = new ClipContext(
            worldTop, worldBottom, Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
        ((ClipContextExtension)clipContext).sable$setSubLevelIgnoring(other -> other == subLevel);

        BlockHitResult hit = level.clip(clipContext);
        if (hit.getType() == Type.MISS) {
            return NO_SURFACE;
        }
        return top + (bottom - top) * (hit.getLocation().distanceTo(worldTop) / reach);
    }

    /** Upper convex hull of the samples, never below them and never concave. */
    private static double[] upperHull(double[] heights) {
        int count = heights.length;
        int[] stack = new int[count];
        int size = 0;

        for (int i = 0; i < count; i++) {
            if (heights[i] <= NO_SURFACE) {
                continue;
            }
            while (size >= 2 && turnsUpward(stack[size - 2], stack[size - 1], i, heights)) {
                size--;
            }
            stack[size++] = i;
        }

        double[] hull = new double[count];
        for (int segment = 0; segment + 1 < size; segment++) {
            int from = stack[segment];
            int to = stack[segment + 1];
            for (int i = from; i <= to; i++) {
                hull[i] = heights[from] + (heights[to] - heights[from]) * ((double)(i - from) / (to - from));
            }
        }
        return hull;
    }

    private static boolean turnsUpward(int a, int b, int c, double[] heights) {
        return (b - a) * (heights[c] - heights[a]) - (heights[b] - heights[a]) * (c - a) >= 0.0;
    }
}
