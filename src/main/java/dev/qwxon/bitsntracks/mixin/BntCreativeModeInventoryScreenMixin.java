package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.registry.datagen.BnbCreativeTabs;
import dev.qwxon.bitsntracks.client.BntCreativeTabRenderer;
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

    @Inject(
        method = {"render"},
        at = {@At("TAIL")}
    )
    private void bitsntracks$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (selectedTab == BnbCreativeTabs.BASE_CREATIVE_TAB.get()) {
            BntCreativeTabRenderer.renderBanners((CreativeModeInventoryScreen)(Object)this, guiGraphics, mouseX, mouseY);
        }
    }
}
