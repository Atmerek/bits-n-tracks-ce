package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditInsertionPlanner;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.kipti.bnb.network.packets.from_client.PartialEditCogwheelChainPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEdit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {PartialEditCogwheelChainPacket.class},
    remap = false
)
public abstract class PartialEditCogwheelChainPacketMixin {
    @Redirect(
        method = {"resolveEditContext"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/edit/CogwheelChainPartialEditInsertionPlanner;resolveBetweenNodesSegment(Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChain;I)Lcom/kipti/bnb/content/kinetics/cogwheel_chain/segment/CogwheelChainSegment;"
        )
    )
    private CogwheelChainSegment bnt$resolveRunForNode(CogwheelChain chain, int nodeIndex) {
        if (!(chain instanceof BntChainGeometryRefresh own) || !own.bnt$isOwn()) {
            return CogwheelChainPartialEditInsertionPlanner.resolveBetweenNodesSegment(chain, nodeIndex);
        }
        return BntChainEdit.segmentForNodeIndex(
            chain, nodeIndex, ((PartialEditCogwheelChainPacket)(Object)this).chainPosition());
    }

    @WrapMethod(
        method = "handle"
    )
    private void bnt$withPlacementLevel(ServerPlayer player, Operation<Void> original) {
        Level level = player.level();
        BlockPos controllerPos = ((PartialEditCogwheelChainPacket)(Object)this).controllerPos();
        boolean own = level.getBlockEntity(controllerPos) instanceof SmartBlockEntity smart
            && smart.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour
            && behaviour.getControlledChain() instanceof BntChainGeometryRefresh chain
            && (chain.bnt$isOwn() || HiddenCogwheelCompat.isOwnChain(level, controllerPos));
        HiddenCogwheelCompat.setPlacementLevel(level);
        boolean previous = BntChainGeometry.beginOwn(own);
        try {
            original.call(player);
        } finally {
            BntChainGeometry.endOwn(previous);
            HiddenCogwheelCompat.setPlacementLevel(null);
        }
    }
}
