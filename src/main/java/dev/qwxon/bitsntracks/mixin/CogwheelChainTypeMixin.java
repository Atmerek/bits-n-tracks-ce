package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
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
                }
                if (block instanceof HiddenCogwheelBlock) {
                    return isBeltType || original.test(block);
                }
                return original.test(block);
            }
        );
    }
}
