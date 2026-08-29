package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {PathedCogwheelNode.class},
    remap = false
)
public abstract class PathedCogwheelNodeMixin {
    @Shadow
    public abstract int side();

    @Shadow
    public abstract boolean isLarge();

    @Shadow
    public abstract boolean hasSmallCogwheelOffset();

    @Inject(
        method = {"sideFactor"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$modifySideFactor(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float)(this.side() * CogwheelSizeHelper.getChainRadius(this.isLarge(), this.hasSmallCogwheelOffset())));
    }
}
