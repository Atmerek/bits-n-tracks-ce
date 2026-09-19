package dev.qwxon.bitsntracks.mixin.compat;

import dev.qwxon.bitsntracks.client.BntShaderHand;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public class IrisHandRendererMixin {
    @Inject(method = "renderSolid", at = @At("HEAD"), require = 0)
    private void bnt$beginSolid(CallbackInfo ci) {
        BntShaderHand.begin(BntShaderHand.Pass.SOLID);
    }

    @Inject(method = "renderSolid", at = @At("RETURN"), require = 0)
    private void bnt$endSolid(CallbackInfo ci) {
        BntShaderHand.end();
    }

    @Inject(method = "renderTranslucent", at = @At("HEAD"), require = 0)
    private void bnt$beginTranslucent(CallbackInfo ci) {
        BntShaderHand.begin(BntShaderHand.Pass.TRANSLUCENT);
    }

    @Inject(method = "renderTranslucent", at = @At("RETURN"), require = 0)
    private void bnt$endTranslucent(CallbackInfo ci) {
        BntShaderHand.end();
    }

    @Inject(method = "isHandTranslucent", at = @At("HEAD"), cancellable = true, require = 0)
    private void bnt$splitSuspensionTool(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof SuspensionToolItem) {
            cir.setReturnValue(BntShaderHand.pass() != BntShaderHand.Pass.SOLID);
        }
    }
}
