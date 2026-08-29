package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.qwxon.bitsntracks.BitsNTracks;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CogAlignmentLeverItemRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel GEAR = PartialModel.of(BitsNTracks.asResource("item/cog_alignment_lever/gear"));

    protected void render(
        ItemStack stack,
        CustomRenderedItemModel model,
        PartialItemModelRenderer renderer,
        ItemDisplayContext transformType,
        PoseStack ms,
        MultiBufferSource buffer,
        int light,
        int overlay
    ) {
        renderer.render(model.getOriginalModel(), light);
        ms.pushPose();
        ms.mulPose(Axis.YP.rotationDegrees(AnimationTickHolder.getRenderTime() * 8.0F % 360.0F));
        renderer.render(GEAR.get(), light);
        ms.popPose();
    }
}
