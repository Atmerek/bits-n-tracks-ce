package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEdit;
import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditInsertionPlan;
import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditInsertionPlanner;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEdit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (!bnt$own(chain)) {
            return CogwheelChainPartialEditInsertionPlanner.resolveBetweenNodesSegment(chain, nodeIndex);
        }
        return BntChainEdit.segmentForNodeIndex(chain, nodeIndex, editContext.chainPosition()) == null
            ? null
            : editContext.segment();
    }

    @WrapMethod(
        method = "plan"
    )
    private static CogwheelChainPartialEditInsertionPlan bnt$planAsOwner(
        CogwheelChain existingChain, CogwheelChainPartialEdit editContext, BlockPos proposedPos, BlockState proposedState,
        Operation<CogwheelChainPartialEditInsertionPlan> original
    ) {
        boolean previous = BntChainGeometry.beginOwn(BntChainGeometry.ownGeometry() || bnt$own(existingChain));
        try {
            return original.call(existingChain, editContext, proposedPos, proposedState);
        } finally {
            BntChainGeometry.endOwn(previous);
        }
    }

    @WrapMethod(
        method = "planWithCandidate"
    )
    private static CogwheelChainPartialEditInsertionPlan bnt$planCandidateAsOwner(
        CogwheelChain existingChain, CogwheelChainPartialEdit editContext, BlockPos proposedPos, CogwheelChainCandidate candidate,
        Operation<CogwheelChainPartialEditInsertionPlan> original
    ) {
        boolean previous = BntChainGeometry.beginOwn(BntChainGeometry.ownGeometry() || bnt$own(existingChain));
        try {
            return original.call(existingChain, editContext, proposedPos, candidate);
        } finally {
            BntChainGeometry.endOwn(previous);
        }
    }

    private static boolean bnt$own(CogwheelChain chain) {
        return chain instanceof BntChainGeometryRefresh own && own.bnt$isOwn();
    }

    @Inject(
        method = {"plan"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void bnt$refuseInsertion(
        CogwheelChain existingChain, CogwheelChainPartialEdit editContext, BlockPos proposedPos, BlockState proposedState,
        CallbackInfoReturnable<CogwheelChainPartialEditInsertionPlan> cir
    ) {
        if (BntCogwheelChainTypes.refusal(
            editContext.chainType(), BntCogwheels.getPlacementLevel(), proposedPos, proposedState) != null) {
            cir.setReturnValue(null);
        }
    }
}
