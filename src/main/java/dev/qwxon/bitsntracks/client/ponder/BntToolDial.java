package dev.qwxon.bitsntracks.client.ponder;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntTunerGauge;
import dev.qwxon.bitsntracks.client.BntTunerNeedle;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.List;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

final class BntToolDial extends PonderElementBase implements PonderOverlayElement {
    private static final double TICK_SECONDS = 0.05;
    private static final int RAISE_TICKS = 15;
    private static final int MESSAGE_TICKS = 60;
    private static final int MESSAGE_FADE = 20;
    private static final float SCALE = 176.0F;
    private static final float RIGHT = 78.0F;
    private static final float HEIGHT = 0.56F;
    private static final float DEPTH = 800.0F;
    private static final float TURN = -16.0F;
    private static final float TILT = 6.0F;
    private static final float MESSAGE_GAP = 54.0F;
    private static final float DIAL_X = 7.4F / 16.0F;
    private static final float DIAL_Y = 25.5F / 16.0F;
    private static final float DIAL_Z = 0.5F;

    private final ItemStack tool = BitsNTracksItems.SUSPENSION_TOOL.asStack();
    private final BntTunerNeedle needle = new BntTunerNeedle();
    private BntTuning setting = BntTuning.STIFFNESS;
    private BlockPos looking;
    private boolean raised;
    private float lift;
    private float liftBefore;
    private float angleBefore = BntTunerGauge.REST_ANGLE;
    private Component message;
    private int messageAge;

    PonderInstruction place() {
        return PonderInstruction.simple(scene -> {
            needle.rest();
            setting = BntTuning.STIFFNESS;
            looking = null;
            raised = false;
            lift = 0.0F;
            liftBefore = 0.0F;
            angleBefore = needle.angle();
            message = null;
            setVisible(true);
            scene.addElement(this);
        });
    }

    PonderInstruction raise(boolean up) {
        return PonderInstruction.simple(scene -> {
            raised = up;
            if (up) {
                say(selected(setting));
            }
        });
    }

    PonderInstruction look(BlockPos pos) {
        return PonderInstruction.simple(scene -> looking = pos);
    }

    PonderInstruction mode(BntTuning next) {
        return PonderInstruction.simple(scene -> {
            setting = next;
            say(selected(next));
        });
    }

    PonderInstruction tune(List<BlockPos> cogs, int value) {
        return PonderInstruction.simple(scene -> {
            for (BlockPos pos : cogs) {
                if (scene.getWorld().getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
                    access.bnt$setTuning(setting, value);
                }
            }
            say(Component.translatable("chat.bits_n_tracks.tuning.level", Component.translatable(setting.translationKey()), value, BntTuning.MAX)
                .withStyle(ChatFormatting.GOLD));
        });
    }

    private static Component selected(BntTuning setting) {
        return Component.translatable("chat.bits_n_tracks.tuning.selected", Component.translatable(setting.translationKey()),
            SuspensionToolItem.scopeName(false)).withStyle(ChatFormatting.GOLD);
    }

    private void say(Component text) {
        message = text;
        messageAge = 0;
    }

    @Override
    public void tick(PonderScene scene) {
        liftBefore = lift;
        lift = Mth.approach(lift, raised ? 1.0F : 0.0F, 1.0F / RAISE_TICKS);
        angleBefore = needle.angle();
        BlockEntity be = looking == null ? null : scene.getWorld().getBlockEntity(looking);
        needle.advance(TICK_SECONDS, BntTunerGauge.levelAngle(be == null ? 0 : setting.levelOf(be)), be != null);
        messageAge++;
    }

    @Override
    public void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks) {
        float shown = Mth.lerp(partialTicks, liftBefore, lift);
        if (shown <= 0.0F) {
            return;
        }
        float eased = shown * shown * (3.0F - 2.0F * shown);
        float x = screen.width - RIGHT;
        float y = screen.height * HEIGHT + (1.0F - eased) * screen.height * 0.75F;

        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(x, y, DEPTH);
        ms.scale(SCALE, -SCALE, SCALE);
        ms.mulPose(Axis.XP.rotationDegrees(TILT));
        ms.mulPose(Axis.YP.rotationDegrees(90.0F + TURN));
        ms.translate(-DIAL_X, -DIAL_Y, -DIAL_Z);
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        BntTunerGauge.stage(Mth.lerp(partialTicks, angleBefore, needle.angle()));
        try {
            IClientItemExtensions.of(tool).getCustomRenderer()
                .renderByItem(tool, ItemDisplayContext.GUI, ms, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            graphics.flush();
        } finally {
            BntTunerGauge.unstage();
        }
        ms.popPose();

        if (message != null && messageAge < MESSAGE_TICKS) {
            float left = (MESSAGE_TICKS - messageAge - partialTicks) / MESSAGE_FADE;
            int alpha = (int)(Mth.clamp(left, 0.0F, 1.0F) * eased * 255.0F);
            if (alpha > 8) {
                ms.pushPose();
                ms.translate(0.0F, 0.0F, DEPTH + 100.0F);
                graphics.drawCenteredString(screen.getFontRenderer(), message, (int)x, (int)(y - MESSAGE_GAP), 0xFFFFFF | alpha << 24);
                ms.popPose();
            }
        }
        RenderSystem.disableDepthTest();
    }
}
