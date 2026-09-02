package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEdit;
import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditInsertionPlanner;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.llamalad7.mixinextras.sugar.Local;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEdit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {CogwheelChainPartialEditInsertionPlanner.class},
    remap = false
)
public abstract class CogwheelChainPartialEditInsertionPlannerMixin {
    @Redirect(
        method = {"resolveSegment"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/edit/CogwheelChainPartialEditInsertionPlanner;resolveBetweenNodesSegment(Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChain;I)Lcom/kipti/bnb/content/kinetics/cogwheel_chain/segment/CogwheelChainSegment;"
        )
    )
    private static CogwheelChainSegment bnt$resolveRunForNode(
        CogwheelChain chain, int nodeIndex, @Local(argsOnly = true) CogwheelChainPartialEdit editContext) {
        return BntChainEdit.segmentForNodeIndex(chain, nodeIndex, editContext.chainPosition()) == null
            ? null
            : editContext.segment();
    }
}
