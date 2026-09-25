package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviourVisual;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {CogwheelChainBehaviourVisual.class},
    remap = false
)
public abstract class CogwheelChainBehaviourVisualMixin {
    @Shadow
    private KineticBlockEntity kineticBlockEntity;
    @Shadow
    private CogwheelChainBehaviour cogwheelChainBehaviour;

    @Shadow
    private void deleteInstance() {
        throw new AssertionError();
    }

    @Inject(
        method = {"update"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$onUpdate(float pt, CallbackInfo ci) {
        if (this.bnt$shouldHide()) {
            this.deleteInstance();
            ci.cancel();
        }
    }

    @Inject(
        method = {"updateLight"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$onUpdateLight(float pt, CallbackInfo ci) {
        if (this.bnt$shouldHide()) {
            this.deleteInstance();
            ci.cancel();
        }
    }

    @Unique
    private boolean bnt$shouldHide() {
        if (this.cogwheelChainBehaviour instanceof TrackModelBehaviourAccess access && access.bnt$isTrackModel()) {
            return true;
        } else {
            CogwheelChain chain = this.cogwheelChainBehaviour.getControlledChain();
            return chain != null && chain.getChainType() != null && chain.getChainType().getRenderTexture().getPath().contains("industrial")
                ? true
                : HiddenCogwheelCompat.shouldForceDynamicRenderer(this.cogwheelChainBehaviour);
        }
    }
}
