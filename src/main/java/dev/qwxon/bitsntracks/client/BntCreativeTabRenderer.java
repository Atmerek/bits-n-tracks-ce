package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.mixin.accessor.CreativeModeInventoryScreenAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BntCreativeTabRenderer {
    private static final ResourceLocation BANNER = BitsNTracks.asResource("textures/gui/bitsntracks_banner.png");
    private static final ResourceLocation BANNER_STILL = BitsNTracks.asResource("bitsntracks_banner_still");
    private static final int BANNER_FRAMES = 15;
    private static final long FRAME_MILLIS = 300L;
    private static long hoveredMillis;
    private static long lastFrameMillis;

    public static void renderBanners(CreativeModeInventoryScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = ((CreativeModeInventoryScreenAccessor)screen).getLeftPos() + 8;
        int top = ((CreativeModeInventoryScreenAccessor)screen).getTopPos() + 17;
        graphics.pose().translate(left, top, 0.0F);
        long now = Util.getMillis();
        banner(graphics, font, BntCreativeTabHelper.tracksRow, Component.translatable("tab.bits_n_tracks.base"), left, top, mouseX, mouseY, now);
        banner(graphics, font, BntCreativeTabHelper.teamRow, Component.translatable("tab.bits_n_tracks.team"), left, top, mouseX, mouseY, now);
        lastFrameMillis = now;

        graphics.pose().popPose();
        RenderSystem.disableDepthTest();
    }

    private static void banner(GuiGraphics graphics, Font font, int row, Component title, int left, int top, int mouseX, int mouseY, long now) {
        int shown = row - BntCreativeTabHelper.currentRow;
        if (shown < 0 || shown > 4) {
            return;
        }
        int y = shown * 18;
        int w = 162;
        int h = 18;
        boolean isHovering = mouseX >= left && mouseX <= left + w && mouseY >= top + y && mouseY <= top + y + h;
        if (isHovering) {
            hoveredMillis += Math.min(now - lastFrameMillis, FRAME_MILLIS);
            int frame = (int)(hoveredMillis / FRAME_MILLIS % BANNER_FRAMES);
            graphics.blit(BANNER, 0, y, w, h, 0.0F, frame * h, w, h, w, h * BANNER_FRAMES);
        } else {
            graphics.blitSprite(BANNER_STILL, 0, y, w, h);
        }
        int textWidth = font.width(title);
        graphics.fill(2, y + 2, textWidth + 8, y + 16, -1441262572);
        drawAuraText(graphics, title, -8224383, -1907998, 5, y + 5);
    }

    public static void drawAuraText(GuiGraphics graphics, Component text, int color1, int color2, int x, int y) {
        Font font = Minecraft.getInstance().font;
        Window window = Minecraft.getInstance().getWindow();
        float scale = (float)window.getGuiScale();
        graphics.drawString(font, text, x + 1, y + 1, -15199212, false);
        graphics.drawString(font, text, x, y, color1, false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1.0F);
        Matrix4f pose = graphics.pose().last().pose();
        Vector3f position = pose.transformPosition(new Vector3f(x, y, 0.0F));
        Vector3f corner = pose.transformPosition(new Vector3f(x + font.width(text), y + 9.0F / 1.8F, 0.0F));
        position.mul(scale);
        corner.mul(scale);
        int height = (int)(corner.y - position.y);
        int width = (int)(corner.x - position.x);
        RenderSystem.enableScissor((int)position.x, window.getHeight() - (int)position.y - height, width, height);
        graphics.drawString(font, text, x, y, color2, false);
        RenderSystem.disableScissor();
        graphics.pose().popPose();
    }
}
