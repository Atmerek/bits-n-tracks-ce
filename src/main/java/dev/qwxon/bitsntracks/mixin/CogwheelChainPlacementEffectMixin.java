package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.ChainDriveDisplayRenderer;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.CogwheelChainPlacementEffect;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.CogwheelChainPlacementInteraction;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {CogwheelChainPlacementEffect.class},
    remap = false
)
public abstract class CogwheelChainPlacementEffectMixin {
    @WrapMethod(
        method = "tick"
    )
    private static void bnt$previewAsOwner(LocalPlayer player, Operation<Void> original) {
        CogwheelChainType type = CogwheelChainPlacementInteraction.getCurrentChainType();
        PlacingCogwheelChain building = CogwheelChainPlacementInteraction.getCurrentBuildingChain();
        boolean own = type != null && BntCogwheelChainTypes.isOwn(type)
            || building != null && BntChainGeometry.ownNodes(player.level(), building.getVisitedNodes());
        boolean previous = BntChainGeometry.beginOwn(own);
        try {
            original.call(player);
        } finally {
            BntChainGeometry.endOwn(previous);
        }
    }

    @Inject(
        method = {"displayTargetCandidate"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void bnt$refuseTarget(
        ClientLevel level, PlacingCogwheelNode lastNode, BlockPos targetedPos, BlockState targetedState,
        CogwheelChainCandidate candidate, CallbackInfoReturnable<BlockPos> cir
    ) {
        CogwheelChainType type = CogwheelChainPlacementInteraction.getCurrentChainType();
        if (type != null && BntCogwheelChainTypes.refusal(type, level, targetedPos, targetedState) != null) {
            ChainDriveDisplayRenderer.renderParticlesBetween(level, lastNode.center(), targetedPos.getCenter(), 16735581);
            cir.setReturnValue(null);
        }
    }
}
