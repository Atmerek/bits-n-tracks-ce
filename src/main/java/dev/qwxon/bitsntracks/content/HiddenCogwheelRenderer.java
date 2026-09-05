package dev.qwxon.bitsntracks.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HiddenCogwheelRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {
    public HiddenCogwheelRenderer(Context context) {
        super(context);
    }

    protected RenderType getRenderType(KineticBlockEntity be, BlockState state) {
        return !state.is((Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get())
                && !state.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())
                && !state.is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())
                && !state.is((Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get())
            ? super.getRenderType(be, state)
            : RenderType.cutout();
    }

    protected void renderSafe(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever())) {
            BlockState renderState = HiddenCogwheelCompat.toVisibleRenderState(be.getBlockState(), be);
            if (renderState != null) {
                SuperByteBuffer model = this.getRotatedModel(be, renderState);
                ms.pushPose();
                Vec3 translation = HiddenCogwheelCompat.getModelTranslation(be, partialTicks);
                ms.translate(translation.x, translation.y, translation.z);
                renderRotatingBuffer(be, model, ms, buffer.getBuffer(this.getRenderType(be, renderState)), light);
                ms.popPose();
            }
        }
    }
}
