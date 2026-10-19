package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.registry.core.BnbTags.BnbBlockTags;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {CogwheelChainCandidate.class},
    remap = false
)
public abstract class CogwheelChainCandidateMixin {
    @Inject(
        method = {"getForBlock"},
        at = {@At("RETURN")},
        cancellable = true
    )
    private static void bnt$modifyOffsetForMedium(BlockState state, CallbackInfoReturnable<CogwheelChainCandidate> cir) {
        CogwheelChainCandidate original = (CogwheelChainCandidate)cir.getReturnValue();
        CogwheelSize size = bnt$size(state);
        if (original == null || size == null) {
            return;
        }
        switch (size) {
            case MEDIUM, MEDIUM_LARGE -> cir.setReturnValue(new CogwheelChainCandidate(original.axis(), true, true));
            case TINY, INTERMEDIATE -> cir.setReturnValue(new CogwheelChainCandidate(original.axis(), false, false));
            case SMALL -> cir.setReturnValue(new CogwheelChainCandidate(original.axis(), false, true));
            case LARGE -> {
            }
        }
    }

    @Unique
    private static CogwheelSize bnt$size(BlockState state) {
        Block block = state.getBlock();
        if (BntCogwheels.isOwnOrLegacyCogwheel(block)) {
            return CogwheelSize.of(block);
        }
        return BntChainGeometry.ownGeometry() && BnbBlockTags.SMALL_FLANGED_COGWHEEL.matches(state) ? CogwheelSize.SMALL : null;
    }
}
