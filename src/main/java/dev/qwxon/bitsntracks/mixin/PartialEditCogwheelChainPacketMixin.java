package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.kipti.bnb.network.packets.from_client.PartialEditCogwheelChainPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEdit;
import net.minecraft.server.level.ServerPlayer;
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
        return BntChainEdit.segmentForNodeIndex(
            chain, nodeIndex, ((PartialEditCogwheelChainPacket)(Object)this).chainPosition());
    }

    @WrapMethod(
        method = "handle"
    )
    private void bnt$withPlacementLevel(ServerPlayer player, Operation<Void> original) {
        HiddenCogwheelCompat.setPlacementLevel(player.level());
        try {
            original.call(player);
        } finally {
            HiddenCogwheelCompat.setPlacementLevel(null);
        }
    }
}
