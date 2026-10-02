package dev.qwxon.bitsntracks.client.flywheel;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.client.BntClientConfig;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntTrackSink;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class BntTrackVisual implements BntTrackSink {
    private static final long REFRESH_TICKS = 20L;
    private static final double NEAR_SQR = 32.0 * 32.0;
    private static final double MID_SQR = 64.0 * 64.0;
    private static final long SECOND_NANOS = 1_000_000_000L;
    private static final double TICK_NANOS = 50_000_000.0;
    private static final double REACH_INTERVALS = 2.0;
    private static final MultiBufferSource NOWHERE = type -> Discard.INSTANCE;

    private final VisualizationContext context;
    private final KineticBlockEntity be;
    private final Vec3 offset;
    private final Map<Model, BntInstancePool<BntSpanInstance>> pools = new HashMap<>();
    private ResourceLocation texture;
    private Vec3 origin = Vec3.ZERO;
    private volatile boolean dirty = true;
    private boolean showing;
    private boolean signed;
    private int signature;
    private long builtAt = Long.MIN_VALUE;
    private long nextBuild;
    private boolean links = true;
    private double scrollRate;
    private double reach;
    private int stampTicks;
    private float stampPartial;

    BntTrackVisual(VisualizationContext context, KineticBlockEntity be, Vec3i visualPos) {
        this.context = context;
        this.be = be;
        this.offset = Vec3.atLowerCornerOf(visualPos);
    }

    void invalidate() {
        dirty = true;
    }

    void frame(float partialTick) {
        CogwheelChainBehaviour behaviour = be.getBehaviour(CogwheelChainBehaviour.TYPE);
        CogwheelChain chain = behaviour == null ? null : behaviour.getControlledChain();
        if (chain == null || be.getLevel() == null) {
            hide();
            return;
        }

        long now = Util.getNanos();
        if (now < nextBuild) {
            return;
        }

        AABB bounds = chain.getRenderBounds();
        double distanceSqr = BntFlywheel.distanceSqr(be, bounds);
        int rate = BntClientConfig.trackUpdateRate();
        long near = rate <= 0 ? 0L : SECOND_NANOS / rate;
        long interval = distanceSqr < NEAR_SQR ? near : distanceSqr < MID_SQR ? near * 2L : near * 3L;
        nextBuild = !showing
            ? now + (long)(interval * ThreadLocalRandom.current().nextDouble())
            : now - nextBuild < interval ? nextBuild + interval : now + interval;
        double view = BntClientCompat.trackViewDistance();
        if (distanceSqr > view * view) {
            hide();
            return;
        }
        if (!dirty && showing && !BntFlywheel.visible(be, bounds)) {
            return;
        }

        float rotation = behaviour.getChainRotationFactor() * be.getSpeed() / 1200.0F;
        long gameTime = be.getLevel().getGameTime();
        if (rotation == 0.0F) {
            int shape = shapeOf(chain);
            if (!dirty && showing && signed && shape == signature && gameTime - builtAt < REFRESH_TICKS) {
                return;
            }
            signature = shape;
            signed = true;
        } else {
            signed = false;
        }

        dirty = false;
        builtAt = gameTime;
        links = distanceSqr < NEAR_SQR;
        scrollRate = 2.0 * Math.PI * rotation;
        reach = REACH_INTERVALS * interval / TICK_NANOS;
        stampTicks = BntFlywheel.ticks();
        stampPartial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        build(behaviour, partialTick);
    }

    void hide() {
        if (!showing) {
            return;
        }
        for (BntInstancePool<BntSpanInstance> pool : pools.values()) {
            pool.begin();
            pool.settle();
        }
        showing = false;
    }

    void delete() {
        for (BntInstancePool<BntSpanInstance> pool : pools.values()) {
            pool.delete();
        }
        pools.clear();
        showing = false;
    }

    private int shapeOf(CogwheelChain chain) {
        Level level = HiddenCogwheelCompat.getActualLevel(be);
        int hash = 1;
        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
            Vec3 shift = BntClientCompat.getNodeDisplacement(be, node.localPos());
            hash = 31 * hash + Double.hashCode(shift.x);
            hash = 31 * hash + Double.hashCode(shift.y);
            hash = 31 * hash + Double.hashCode(shift.z);
            Direction route = BntChainEngagement.routeSide(level, be.getBlockPos().offset(node.localPos()));
            hash = 31 * hash + (route == null ? -1 : route.ordinal());
        }
        return hash;
    }

    private void build(CogwheelChainBehaviour behaviour, float partialTick) {
        for (BntInstancePool<BntSpanInstance> pool : pools.values()) {
            pool.begin();
        }

        BntTrackSink previous = BntTrackSink.CURRENT.get();
        BntTrackSink.CURRENT.set(this);
        try {
            behaviour.getRenderer().get().get().castRenderSafe(
                behaviour, be, partialTick, new PoseStack(), NOWHERE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } finally {
            BntTrackSink.CURRENT.set(previous);
        }

        for (BntInstancePool<BntSpanInstance> pool : pools.values()) {
            pool.settle();
        }
        showing = true;
    }

    @Override
    public double scrollRate() {
        return scrollRate;
    }

    @Override
    public double reach() {
        return reach;
    }

    @Override
    public boolean links() {
        return links;
    }

    @Override
    public void at(ResourceLocation texture, Vec3 origin) {
        this.texture = texture;
        this.origin = origin;
    }

    @Override
    public void span(Object shape, List<Vec3> source, List<Vec3> destination, float minV, float maxV, float scrollV, int lightFrom, int lightTo) {
        BntSpanInstance instance = pool(BntSpanModels.span(texture, shape)).next();
        end(instance, 0, source);
        end(instance, 1, destination);
        instance.texture(minV, maxV)
            .light(lightFrom, lightTo)
            .stamp(stampTicks, stampPartial, (float)reach)
            .scroll(scrollV)
            .setChanged();
    }

    @Override
    public void box(float[][] uv, Vec3 centre, Vec3 width, Vec3 thick, Vec3 length, int light, Vec3 drift, double along, double alongRate) {
        boolean mirrored = width.dot(thick.cross(length)) < 0.0;
        BntSpanInstance instance = pool(BntSpanModels.box(texture, uv, mirrored)).next();
        Vec3 middle = centre.add(origin).add(offset);
        instance.frame(0, middle.subtract(length), width, thick)
            .frame(1, middle.add(length), width, thick)
            .texture(0.0F, 1.0F)
            .light(light, light)
            .stamp(stampTicks, stampPartial, (float)reach)
            .travel(drift, along, alongRate)
            .setChanged();
    }

    private void end(BntSpanInstance instance, int end, List<Vec3> corners) {
        Vec3 first = corners.get(0);
        Vec3 second = corners.get(1);
        Vec3 third = corners.get(2);
        instance.frame(end, first.add(third).scale(0.5).add(origin).add(offset), first.subtract(second).scale(0.5), second.subtract(third).scale(0.5));
    }

    private BntInstancePool<BntSpanInstance> pool(Model model) {
        return pools.computeIfAbsent(model, key -> new BntInstancePool<>(context.instancerProvider().instancer(BntInstanceTypes.SPAN, key)));
    }

    private static final class Discard implements VertexConsumer {
        private static final Discard INSTANCE = new Discard();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
