package dev.qwxon.bitsntracks.content.suspension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BntSuspensionPieceItemRenderer extends CustomRenderedItemModelRenderer {
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
        BntSuspensionPieceRenderer.renderItem(ms, buffer, light, overlay);
    }
}
