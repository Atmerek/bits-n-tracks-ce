package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.client.BntCreativeTabHelper;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemPickerMenu.class})
public abstract class BntItemPickerMenuMixin {
    @Shadow
    protected abstract int getRowIndexForScroll(float var1);

    @Inject(
        method = {"scrollTo"},
        at = {@At("HEAD")}
    )
    private void bitsntracks$scrollTo(float f, CallbackInfo ci) {
        BntCreativeTabHelper.CURRENT_ROW = this.getRowIndexForScroll(f);
    }
}
