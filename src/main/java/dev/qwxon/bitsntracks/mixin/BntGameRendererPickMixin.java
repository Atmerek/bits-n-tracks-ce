package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.client.BntCogwheelPicker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class BntGameRendererPickMixin {
    @Inject(
        method = "pick(F)V",
        at = @At("TAIL")
    )
    private void bnt$pickOffsetCogwheels(float partialTicks, CallbackInfo ci) {
        BntCogwheelPicker.correctPick(partialTicks);
    }
}
