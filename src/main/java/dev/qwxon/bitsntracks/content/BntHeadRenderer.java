package dev.qwxon.bitsntracks.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.neoforged.neoforge.client.model.data.ModelData;

public class BntHeadRenderer implements BlockEntityRenderer<BntHeadBlockEntity> {
    private static final double WALL_LIFT = 0.25;

    public BntHeadRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        BntHeadBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay
    ) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof BntHeadBlock)) {
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        float turn = RotationSegment.convertToDegrees(state.getValue(BntHeadBlock.ROTATION));

        ms.pushPose();
        ms.translate(0.5, 0.0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-turn));
        ms.translate(-0.5, 0.0, -0.5);
        if (state.getValue(BntHeadBlock.WALL)) {
            ms.translate(0.0, WALL_LIFT, WALL_LIFT);
        }

        dispatcher.getModelRenderer()
            .renderModel(
                ms.last(), buffer.getBuffer(RenderType.cutout()), state, model, 1.0F, 1.0F, 1.0F, light, overlay,
                ModelData.EMPTY, RenderType.cutout()
            );
        ms.popPose();
    }
}
