package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.ResidualChainResult;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
    value = {ResidualChainResult.class},
    remap = false
)
public abstract class ResidualChainResultMixin {
    @WrapMethod(
        method = "tryBuildResidualChain"
    )
    private static ResidualChainResult bnt$keepOwner(CogwheelChain chain, BlockPos controllerPos, BlockPos removedPos,
                                                    Operation<ResidualChainResult> original) {
        boolean own = chain instanceof BntChainGeometryRefresh refresh && refresh.bnt$isOwn();
        boolean previous = BntChainGeometry.beginOwn(own);
        try {
            return original.call(chain, controllerPos, removedPos);
        } finally {
            BntChainGeometry.endOwn(previous);
        }
    }
}
