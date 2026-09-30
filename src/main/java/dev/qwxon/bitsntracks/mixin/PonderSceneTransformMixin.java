package dev.qwxon.bitsntracks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.qwxon.bitsntracks.client.ponder.BntPonderCamera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    targets = {"net.createmod.ponder.foundation.PonderScene$SceneTransform"},
    remap = false
)
public abstract class PonderSceneTransformMixin {
    @Inject(
        method = {"apply(Lcom/mojang/blaze3d/vertex/PoseStack;F)Lcom/mojang/blaze3d/vertex/PoseStack;"},
        at = {@At("RETURN")}
    )
    private void bnt$frame(PoseStack ms, float pt, CallbackInfoReturnable<PoseStack> cir) {
        BntPonderCamera.frame(this, ms, pt);
    }

    @Inject(
        method = {"screenToScene"},
        at = {@At("RETURN")},
        cancellable = true
    )
    private void bnt$unframe(double x, double y, int depth, float pt, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(BntPonderCamera.unframe(this, cir.getReturnValue(), pt));
    }
}
