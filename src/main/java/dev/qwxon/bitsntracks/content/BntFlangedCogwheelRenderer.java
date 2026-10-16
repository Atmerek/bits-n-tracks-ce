package dev.qwxon.bitsntracks.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.client.BntSuspensionPlacement;
import dev.qwxon.bitsntracks.content.suspension.BntBogieRenderer;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionPieceRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BntFlangedCogwheelRenderer extends KineticBlockEntityRenderer<BntFlangedCogwheelBlockEntity> {
    public BntFlangedCogwheelRenderer(Context context) {
        super(context);
    }

    @Override
    public int getViewDistance() {
        return BntClientCompat.trackViewDistance();
    }

    public static BlockState modelState(KineticBlockEntity be) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof BntFlangedCogwheelBlock)) {
            return state;
        }
        state = state.setValue(BntFlangedCogwheelBlock.PHYSICS, false);
        return BntSuspension.isWideBogie(be) ? state.setValue(BntCogwheelPairing.WIDE, BntWideSide.NONE) : state;
    }

    public static boolean cutout(BlockState state) {
        return HiddenCogwheelCompat.isIndustrial(state);
    }

    protected void renderSafe(BntFlangedCogwheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever()) {
            return;
        }
        if (!BntVisualized.drawn(be)) {
            BlockState renderState = modelState(be);
            SuperByteBuffer model = this.getRotatedModel(be, renderState);
            ms.pushPose();
            Vec3 translation = HiddenCogwheelCompat.getModelTranslation(be, partialTicks);
            ms.translate(translation.x, translation.y, translation.z);
            renderRotatingBuffer(be, model, ms, buffer.getBuffer(this.getRenderType(be, renderState)), light);
            ms.popPose();
        }
        if (!BntFlangedCogwheelBlock.physics(be.getBlockState())) {
            return;
        }
        if (!BntVisualized.drawn(be)) {
            BntSuspensionPieceRenderer.renderAttached(be, partialTicks, ms, buffer, light, overlay);
        }
        if (BntSuspensionPlacement.previewAt(be.getBlockPos())) {
            BntSuspensionPieceRenderer.renderGhost(be, BntSuspensionPlacement.side(), BntSuspensionPlacement.facing(), partialTicks, ms, buffer);
        }
        if (BntSuspensionPlacement.bogiePreviewAt(be.getBlockPos())) {
            BntBogieRenderer.renderGhost(be, BntSuspensionPlacement.bogieFacing(), BntSuspensionPlacement.bogieDrop(), ms, buffer);
        }
    }

    protected RenderType getRenderType(BntFlangedCogwheelBlockEntity be, BlockState state) {
        return cutout(state) ? RenderType.cutout() : super.getRenderType(be, state);
    }
}
