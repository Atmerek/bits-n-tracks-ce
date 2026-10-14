package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;

@OnlyIn(Dist.CLIENT)
public final class BntChainOutline extends Outline {
    private static final String SLOT = "bnt_chain_selection";
    private static final float LINE_WIDTH = 1.0F / 64.0F;
    private static final BntChainOutline INSTANCE = new BntChainOutline();

    private final List<Vector3d> ends = new ArrayList<>();
    private final Collector collector = new Collector();
    private Vec3 origin = Vec3.ZERO;

    private BntChainOutline() {
    }

    public static VertexConsumer begin(Vec3 camera) {
        INSTANCE.ends.clear();
        INSTANCE.origin = camera;
        return INSTANCE.collector;
    }

    public static void show(int colour) {
        Outliner.getInstance().showOutline(SLOT, INSTANCE).colored(colour).lineWidth(LINE_WIDTH);
    }

    public static void hide() {
        Outliner.getInstance().remove(SLOT);
    }

    @Override
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        float width = this.params.getLineWidth();
        if (width == 0.0F) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());
        this.params.loadColor(this.colorTemp);
        for (int i = 0; i + 1 < this.ends.size(); i += 2) {
            this.bufferCuboidLine(ms, consumer, camera, this.ends.get(i), this.ends.get(i + 1), width, this.colorTemp, LightTexture.FULL_BRIGHT, false);
        }
    }

    private final class Collector implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            BntChainOutline.this.ends.add(new Vector3d(x + BntChainOutline.this.origin.x, y + BntChainOutline.this.origin.y, z + BntChainOutline.this.origin.z));
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
