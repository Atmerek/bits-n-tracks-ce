package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.registry.core.BnbTags.BnbBlockTags;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
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
            case MEDIUM -> cir.setReturnValue(new CogwheelChainCandidate(original.axis(), true, true));
            case TINY -> cir.setReturnValue(new CogwheelChainCandidate(original.axis(), false, false));
            case SMALL -> cir.setReturnValue(new CogwheelChainCandidate(original.axis(), false, true));
            case LARGE -> {
            }
        }
    }

    @Unique
    private static CogwheelSize bnt$size(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock) {
            return CogwheelSize.of(block);
        }
        return BnbBlockTags.SMALL_FLANGED_COGWHEEL.matches(state) ? CogwheelSize.SMALL : null;
    }
}
