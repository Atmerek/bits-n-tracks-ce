package dev.qwxon.bitsntracks.content;

import com.mojang.blaze3d.vertex.PoseStack;
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

public class BntFlangedCogwheelRenderer extends KineticBlockEntityRenderer<BntFlangedCogwheelBlockEntity> {
    public BntFlangedCogwheelRenderer(Context context) {
        super(context);
    }

    protected void renderSafe(BntFlangedCogwheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever())) {
            BlockState renderState = be.getBlockState();
            SuperByteBuffer model = this.getRotatedModel(be, renderState);
            ms.pushPose();
            Vec3 translation = HiddenCogwheelCompat.getModelTranslation(be, partialTicks);
            ms.translate(translation.x, translation.y, translation.z);

            renderRotatingBuffer(be, model, ms, buffer.getBuffer(this.getRenderType(be, renderState)), light);
            ms.popPose();
        }
    }

    protected RenderType getRenderType(BntFlangedCogwheelBlockEntity be, BlockState state) {
        return !state.is((Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get())
                && !state.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())
                && !state.is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())
            ? super.getRenderType(be, state)
            : RenderType.cutout();
    }
}
