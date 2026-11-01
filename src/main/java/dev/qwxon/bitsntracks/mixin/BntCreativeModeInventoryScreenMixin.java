package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.client.BntCreativeTabRenderer;
import dev.qwxon.bitsntracks.index.BitsNTracksCreativeTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CreativeModeInventoryScreen.class})
public class BntCreativeModeInventoryScreenMixin {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Inject(method = "render", at = @At("TAIL"))
    private void bnt$renderBanners(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (selectedTab == BitsNTracksCreativeTabs.BASE.get()) {
            BntCreativeTabRenderer.renderBanners((CreativeModeInventoryScreen)(Object)this, guiGraphics, mouseX, mouseY);
        }
    }
}
