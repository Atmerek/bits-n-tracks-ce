package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {CogwheelChainType.class},
    remap = false
)
public abstract class CogwheelChainTypeMixin {
    @Inject(
        method = {"getCogwheelPredicate()Ljava/util/function/Predicate;"},
        at = {@At("RETURN")},
        cancellable = true
    )
    private void bnt$wrapCogwheelPredicate(CallbackInfoReturnable<Predicate<Block>> cir) {
        Predicate<Block> original = (Predicate<Block>)cir.getReturnValue();
        boolean isBeltType = ((CogwheelChainType)(Object)this).getRenderType() == ChainRenderInfo.BELT;
        cir.setReturnValue(
            (Predicate<Block>)block -> {
                if (block instanceof BntFlangedCogwheelBlock) {
                    return isBeltType;
                } else {
                    BlockState state = block.defaultBlockState();
                    if (state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())) {
                        return isBeltType;
                    } else if (!HiddenCogwheelCompat.isHiddenFlangedCogwheel(state)) {
                        return original.test(block);
                    } else if (original.test(block)) {
                        return true;
                    } else {
                        BlockState visibleState = HiddenCogwheelCompat.toVisibleCogwheelState(state);
                        return visibleState != null && visibleState.getBlock() instanceof BntFlangedCogwheelBlock
                            ? isBeltType
                            : visibleState != null && original.test(visibleState.getBlock());
                    }
                }
            }
        );
    }
}
