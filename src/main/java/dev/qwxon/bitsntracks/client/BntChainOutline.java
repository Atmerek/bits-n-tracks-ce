package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.ChainCoordinateSpace;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainWholeShape;
import com.kipti.bnb.content.kinetics.cogwheel_chain.world.CogwheelChainWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.qwxon.bitsntracks.mixin.accessor.CogwheelChainShapeAccessor;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

@OnlyIn(Dist.CLIENT)
public final class BntChainOutline extends Outline {
    public static final String SELECTED_SLOT = "bnt_chain_selected";
    public static final String HOVER_SLOT = "bnt_chain_hover";
    private static final Map<String, BntChainOutline> WHOLE = new HashMap<>();

    private final List<Vector3d> ends = new ArrayList<>();
    private final Collector collector = new Collector();
    @Nullable
    private BlockPos controller;

    private BntChainOutline() {
    }

    public static void showWhole(String slot, BlockPos controller, int colour, float width) {
        BntChainOutline outline = WHOLE.computeIfAbsent(slot, key -> new BntChainOutline());
        outline.controller = controller;
        Outliner.getInstance().showOutline(slot, outline).colored(colour).lineWidth(width);
    }

    private void collectWhole() {
        this.ends.clear();
        Level level = Minecraft.getInstance().level;
        BlockPos pos = this.controller;
        if (level == null || pos == null) {
            return;
        }
        CogwheelChain chain = CogwheelChainWorld.get(level).getChain(pos);
        if (chain == null) {
            return;
        }
        CogwheelChainWholeShape shape;
        BntChainShapeContext.set(level, pos);
        try (BntRadiusProvider.Scope ignored = BntRadiusProvider.at(level, pos)) {
            shape = CogwheelChainWholeShape.buildShape(chain);
        } finally {
            BntChainShapeContext.clear();
        }
        if (shape == null) {
            return;
        }
        ChainCoordinateSpace space = ChainCoordinateSpace.forRender(level, pos);
        ((CogwheelChainShapeAccessor)shape).bnt$drawOutline(new PoseStack(), this.collector, space::toWorld);
    }

    @Override
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        float width = this.params.getLineWidth();
        if (width == 0.0F) {
            return;
        }
        this.collectWhole();
        VertexConsumer consumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());
        this.params.loadColor(this.colorTemp);
        for (int i = 0; i + 1 < this.ends.size(); i += 2) {
            this.bufferCuboidLine(ms, consumer, camera, this.ends.get(i), this.ends.get(i + 1), width, this.colorTemp, LightTexture.FULL_BRIGHT, false);
        }
    }

    private final class Collector implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            BntChainOutline.this.ends.add(new Vector3d(x, y, z));
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
