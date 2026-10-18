package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.network.packets.from_client.PlaceCogwheelChainPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
    }
}
