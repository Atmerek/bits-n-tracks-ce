package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.qwxon.bitsntracks.mixin.accessor.CreativeModeInventoryScreenAccessor;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BntCreativeTabRenderer {
    public static void renderBanners(CreativeModeInventoryScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = ((CreativeModeInventoryScreenAccessor)screen).getLeftPos() + 8;
        int top = ((CreativeModeInventoryScreenAccessor)screen).getTopPos() + 17;
        graphics.pose().translate(left, top, 0.0F);
        int bntRow = BntCreativeTabHelper.BNT_ROW - BntCreativeTabHelper.CURRENT_ROW;
        if (bntRow >= 0 && bntRow <= 4) {
            int y = bntRow * 18;
            int x = 0;
            int w = 162;
            int h = 18;
            boolean isHovering = mouseX >= left + x && mouseX <= left + x + w && mouseY >= top + y && mouseY <= top + y + h;
            ResourceLocation bannerStill = ResourceLocation.fromNamespaceAndPath("bits_n_tracks", "bitsntracks_banner_still");
            ResourceLocation bannerAnim = ResourceLocation.fromNamespaceAndPath("bits_n_tracks", "bitsntracks_banner");
            setPlaying(bannerAnim, isHovering);
            ResourceLocation activeBanner = isHovering ? bannerAnim : bannerStill;
            graphics.blitSprite(activeBanner, 0, y, w, h);
            Component title = Component.translatable("tab.bits_n_tracks.base");
            int textWidth = font.width(title);
            graphics.fill(2, y + 2, textWidth + 8, y + 16, -1441262572);
            drawAuraText(graphics, title, -8224383, -1907998, 5, y + 5);
        }

        graphics.pose().popPose();
        RenderSystem.disableDepthTest();
    }

    public static void setPlaying(ResourceLocation resourceLocation, boolean playing) {
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(resourceLocation);
            if (sprite == null) {
                return;
            }

            SpriteContents contents = sprite.contents();
            if (contents == null) {
                return;
            }

            Method getTickerMethod = null;

            for (Method m : contents.getClass().getMethods()) {
                if (m.getName().equals("simulated$getTicker")) {
                    getTickerMethod = m;
                    break;
                }
            }

            if (getTickerMethod == null) {
                return;
            }

            Object ticker = getTickerMethod.invoke(contents);
            if (ticker == null) {
                return;
            }

            Method setPlayingMethod = null;

            for (Method mx : ticker.getClass().getMethods()) {
                if (mx.getName().equals("simulated$setPlaying")) {
                    setPlayingMethod = mx;
                    break;
                }
            }

            if (setPlayingMethod == null) {
                return;
            }

            setPlayingMethod.invoke(ticker, playing);
        } catch (Exception var11) {
        }
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
