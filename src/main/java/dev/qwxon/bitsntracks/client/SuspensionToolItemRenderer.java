package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.physics.BntTuning;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SuspensionToolItemRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel NEEDLE = PartialModel.of(BitsNTracks.asResource("item/suspension_tool/needle"));
    private static final PartialModel TICK = PartialModel.of(BitsNTracks.asResource("item/suspension_tool/tick"));
    private static final PartialModel SCREEN = PartialModel.of(BitsNTracks.asResource("item/suspension_tool/screen"));
    private static final float PIVOT_X = (7.5F - 8.0F) / 16.0F;
    private static final float PIVOT_Y = (25.5F - 8.0F) / 16.0F;
    private static final float NEEDLE_MODEL_ANGLE = 90.0F;

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
        BntShaderHand.Pass pass = BntShaderHand.pass();
        if (pass != BntShaderHand.Pass.TRANSLUCENT || !BntShaderHand.solidDrawn()) {
            renderBody(stack, model, renderer, ms, light);
            if (pass == BntShaderHand.Pass.SOLID) {
                BntShaderHand.markSolidDrawn();
            }
        }
        if (pass != BntShaderHand.Pass.SOLID) {
            renderer.render(SCREEN.get(), Sheets.translucentCullBlockSheet(), light);
        }
    }

    private static void renderBody(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, PoseStack ms, int light) {
        RenderType cutout = Sheets.cutoutBlockSheet();
        renderer.render(model.getOriginalModel(), cutout, light);

        for (int level = BntTuning.MIN; level <= BntTuning.MAX; level++) {
            aroundDial(ms, BntTunerGauge.levelAngle(level));
            renderer.render(TICK.get(), cutout, light);
            ms.popPose();
        }

        aroundDial(ms, BntTunerGauge.needleAngle(stack) + NEEDLE_MODEL_ANGLE);
        renderer.render(NEEDLE.get(), cutout, light);
        ms.popPose();
    }

    private static void aroundDial(PoseStack ms, float degrees) {
        ms.pushPose();
        ms.translate(PIVOT_X, PIVOT_Y, 0.0F);
        ms.mulPose(Axis.XP.rotationDegrees(degrees));
        ms.translate(-PIVOT_X, -PIVOT_Y, 0.0F);
    }
}
