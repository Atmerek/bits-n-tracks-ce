package dev.qwxon.bitsntracks.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;

public class HiddenCogwheelRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {
    public HiddenCogwheelRenderer(Context context) {
        super(context);
    }

    protected void renderSafe(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever())) {
            BlockState renderState = HiddenCogwheelCompat.toVisibleRenderState(be.getBlockState(), be);
            if (renderState != null) {
                SuperByteBuffer model = this.getRotatedModel(be, renderState);
                ms.pushPose();
                double alignX = be instanceof KineticBlockEntityPhysicsAccess accessx ? accessx.bnt$getAlignmentOffsetX() : 0.0;
                double alignY = be instanceof KineticBlockEntityPhysicsAccess accessxx ? accessxx.bnt$getAlignmentOffsetY() : 0.0;
                double alignZ = be instanceof KineticBlockEntityPhysicsAccess accessxxx ? accessxxx.bnt$getAlignmentOffsetZ() : 0.0;
                ms.translate(alignX, HiddenCogwheelCompat.getVisualVerticalTranslation(be, partialTicks) + alignY, alignZ);
                renderRotatingBuffer(be, model, ms, buffer.getBuffer(this.getRenderType(be, renderState)), light);
                ms.popPose();
            }
        }
    }
}
