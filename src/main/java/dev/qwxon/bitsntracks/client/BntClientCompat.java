package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltLinks;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BntClientCompat {
    private static final String STRUTS_LIGHTER = "com.cake.struts.content.IAntiClippedShadowLighter";
    private static final int UNLIT = Integer.MIN_VALUE;
    private static final int LIGHT_REACH = 1 << 20;
    private static final ThreadLocal<ChainLights> CHAIN_LIGHTS = ThreadLocal.withInitial(ChainLights::new);

    /** The pose the cogwheels are drawn against, which their suspension drop is measured in. */
    public static Pose3dc drawnPose(SubLevel subLevel) {
        return subLevel instanceof ClientSubLevel client
            ? client.renderPose(getPartialTick())
            : subLevel.logicalPose();
    }

    public static double getVisualDrop(BlockEntity be, float partialTick) {
        return HiddenCogwheelCompat.getVisualDrop(be, partialTick);
    }

    public static double getVisualVerticalTranslation(BlockEntity be, float partialTick) {
        return HiddenCogwheelCompat.getVisualVerticalTranslation(be, partialTick);
    }

    /** Tracks are drawn by a block entity renderer, so they need the sublevel's reach rather than the default 64. */
    public static int trackViewDistance() {
        return Math.max(Minecraft.getInstance().options.getEffectiveRenderDistance() * 16, 64);
    }

    /** Lights for a chain are kept for the rest of the tick, so the frames drawn within it share them. */
    public static void beginChainLight(BlockEntity be) {
        ChainLights lights = CHAIN_LIGHTS.get();
        ChainLight cache = lights.byChain.computeIfAbsent(be, ignored -> new ChainLight());
        int tick = be.getLevel() == null ? AnimationTickHolder.getTicks() : AnimationTickHolder.getTicks(be.getLevel());
        if (!cache.started || cache.tick != tick) {
            cache.tick = tick;
            cache.started = false;
            cache.cells.clear();
        }
        lights.current = cache;
    }

    public static void endChainLight() {
        CHAIN_LIGHTS.get().current = null;
    }

    /** Struts lights a point from its block plus any face it sits within 0.3 of, so points sharing both share a light. */
    public static int chainLight(Function<Vector3f, Integer> lighter, Vector3f point) {
        ChainLights lights = CHAIN_LIGHTS.get();
        ChainLight cache = lights.current;
        if (cache == null || !lights.isStruts(lighter)) {
            return lighter.apply(point);
        }

        int x = Mth.floor(point.x);
        int y = Mth.floor(point.y);
        int z = Mth.floor(point.z);
        if (!cache.started) {
            cache.started = true;
            cache.originX = x;
            cache.originY = y;
            cache.originZ = z;
        }

        int dx = x - cache.originX;
        int dy = y - cache.originY;
        int dz = z - cache.originZ;
        if (Math.abs(dx) >= LIGHT_REACH || Math.abs(dy) >= LIGHT_REACH || Math.abs(dz) >= LIGHT_REACH) {
            return lighter.apply(point);
        }

        long key = ((long)(dx + LIGHT_REACH) << 42) | ((long)(dy + LIGHT_REACH) << 21) | (dz + LIGHT_REACH);
        int[] cell = cache.cells.get(key);
        if (cell == null) {
            cell = new int[27];
            Arrays.fill(cell, UNLIT);
            cache.cells.put(key, cell);
        }

        int slot = lightSide(point.x) * 9 + lightSide(point.y) * 3 + lightSide(point.z);
        int light = cell[slot];
        if (light == UNLIT) {
            light = lighter.apply(point);
            cell[slot] = light;
        }
        return light;
    }

    private static int lightSide(float value) {
        float offset = value - (float)Math.round(value);
        return Math.abs(offset) < 0.3F ? (offset > 0.0F ? 1 : 2) : 0;
    }

    public static float getPartialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    public static Vec3 getTransformedPosition(BlockEntity controllerBe, Vec3 localPos, BlockPos relativePos) {
        Level level = HiddenCogwheelCompat.getActualLevel(controllerBe);
        if (level == null) {
            return localPos;
        }
        BlockPos nodePos = controllerBe.getBlockPos().offset(relativePos);
        return localPos
            .add(BntCogwheelPairing.seamOffset(level.getBlockState(nodePos)))
            .add(getNodeDisplacement(controllerBe, relativePos));
    }

    public static Vec3 getNodeDisplacement(BlockEntity controllerBe, BlockPos relativePos) {
        Level level = HiddenCogwheelCompat.getActualLevel(controllerBe);
        if (level == null) {
            return Vec3.ZERO;
        }
        BlockEntity nodeBe = level.getBlockEntity(controllerBe.getBlockPos().offset(relativePos));
        if (nodeBe == null) {
            return Vec3.ZERO;
        }

        Vec3 displacement = Vec3.ZERO;
        if (nodeBe instanceof KineticBlockEntityPhysicsAccess access) {
            displacement = new Vec3(
                access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ()
            );
        }

        if (level.isClientSide && HiddenCogwheelCompat.isPhysicsEnabled(nodeBe)) {
            double manualOffset = HiddenCogwheelCompat.getManualVisualVerticalOffset(nodeBe);
            if (manualOffset != 0.0) {
                displacement = displacement.add(0.0, manualOffset, 0.0);
            }

            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            displacement = displacement.add(BntSuspension.displacement(nodeBe, HiddenCogwheelCompat.getHeldVisualDrop(nodeBe, partialTick)));
        }

        return displacement;
    }

    public static List<ChainSegment> transformChainSegments(List<ChainSegment> segments, CogwheelChain chain, KineticBlockEntity be) {
        if (chain != null && segments != null && !segments.isEmpty()) {
            List<RenderedChainPathNode> pathNodes = chain.getChainPathNodes();
            int size = pathNodes.size();
            if (size != segments.size()) {
                return segments;
            } else {
                Vec3[] shifts = nodeShifts(be, pathNodes);
                Vec3[][] points = new Vec3[size][];
                double[] lengths = new double[size];
                double drawn = 0.0;
                for (int i = 0; i < size; i++) {
                    ChainSegment segment = segments.get(i);
                    Vec3 tPreFrom = segment.preFrom().add(shifts[(i + 2) % size]);
                    Vec3 tFrom = segment.from().add(shifts[(i + 1) % size]);
                    Vec3 tTo = segment.to().add(shifts[i]);
                    Vec3 tPostTo = segment.postTo().add(shifts[(i - 1 + size) % size]);
                    points[i] = new Vec3[]{tPreFrom, tFrom, tTo, tPostTo};
                    lengths[i] = tFrom.distanceTo(tTo);
                    drawn += lengths[i];
                }

                List<ChainSegment> transformed = new ArrayList<>(size);
                BntBeltLinks.beginSpans();
                double start = 0.0;
                for (int i = 0; i < size; i++) {
                    ChainSegment segment = segments.get(i);
                    Vec3[] point = points[i];
                    transformed.add(new ChainSegment(
                        point[0], point[1], point[2], point[3], segment.fromCogwheelAxis(), segment.toCogwheelAxis(), start, lengths[i]
                    ));
                    BntBeltLinks.addSpan(point[2], start, lengths[i]);
                    start += lengths[i];
                }
                BntBeltLinks.setDrawnLength(drawn);
                return transformed;
            }
        } else {
            return segments;
        }
    }

    /** What getTransformedPosition adds for each path node, worked out once per cogwheel rather than per point. */
    private static Vec3[] nodeShifts(KineticBlockEntity be, List<RenderedChainPathNode> pathNodes) {
        int size = pathNodes.size();
        Vec3[] shifts = new Vec3[size];
        Level level = HiddenCogwheelCompat.getActualLevel(be);
        if (level == null) {
            Arrays.fill(shifts, Vec3.ZERO);
            return shifts;
        }

        Map<BlockPos, Vec3> byCogwheel = new HashMap<>();
        for (int i = 0; i < size; i++) {
            BlockPos relativePos = pathNodes.get(i).relativePos();
            Vec3 shift = byCogwheel.get(relativePos);
            if (shift == null) {
                BlockPos nodePos = be.getBlockPos().offset(relativePos);
                shift = BntCogwheelPairing.seamOffset(level.getBlockState(nodePos)).add(getNodeDisplacement(be, relativePos));
                byCogwheel.put(relativePos, shift);
            }
            shifts[i] = shift;
        }
        return shifts;
    }

    private static final class ChainLights {
        private final Map<BlockEntity, ChainLight> byChain = new WeakHashMap<>();
        private ChainLight current;
        private Class<?> struts;

        private boolean isStruts(Object lighter) {
            Class<?> type = lighter.getClass();
            if (type == this.struts) {
                return true;
            }
            if (!type.getName().startsWith(STRUTS_LIGHTER)) {
                return false;
            }
            this.struts = type;
            return true;
        }
    }

    private static final class ChainLight {
        private final Long2ObjectOpenHashMap<int[]> cells = new Long2ObjectOpenHashMap<>();
        private boolean started;
        private int tick;
        private int originX;
        private int originY;
        private int originZ;
    }
}
