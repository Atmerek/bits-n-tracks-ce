package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.network.packets.from_client.PlaceCogwheelChainPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksAdvancements;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PlaceCogwheelChainPacket.class, remap = false)
public abstract class PlaceCogwheelChainPacketMixin {
    @Shadow
    public abstract PlacingCogwheelChain worldSpacePartialChain();

    @WrapMethod(method = "handle")
    private void bnt$placeAsOwn(ServerPlayer player, Operation<Void> original) {
        Level level = player.level();
        BntCogwheels.setPlacementLevel(level);
        boolean previous = BntChainGeometry.beginOwn(BntChainGeometry.ownNodes(level, this.worldSpacePartialChain().getVisitedNodes()));
        try {
            original.call(player);
        } finally {
            BntChainGeometry.endOwn(previous);
            BntCogwheels.setPlacementLevel(null);
        }
        bnt$award(player, level);
    }

    @Unique
    private void bnt$award(ServerPlayer player, Level level) {
        List<PlacingCogwheelNode> nodes = this.worldSpacePartialChain().getVisitedNodes();
        BlockPos controllerPos = nodes.isEmpty() ? null : BntChains.controllerPos(level, nodes.get(0).pos());
        if (controllerPos == null || !BntCogwheels.isOwnChain(level, controllerPos)) {
            return;
        }
        BitsNTracksAdvancements.TRACK.award(player);
        CogwheelChain chain = BntChains.chainAt(level, controllerPos);
        if (chain != null && chain.getChainType() == BntCogwheelChainTypes.INDUSTRIAL_BELT_CHAIN.get()) {
            BitsNTracksAdvancements.INDUSTRIAL_BELT.award(player);
        } else if (chain != null && chain.getChainType() == BntCogwheelChainTypes.TANK_TREAD_CHAIN.get()) {
            BitsNTracksAdvancements.TANK_TREAD.award(player);
        }
    }
}
