package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.network.packets.from_client.WrenchCogwheelChainPacket;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntGrousers;
import dev.qwxon.bitsntracks.interaction.BntReach;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WrenchCogwheelChainPacket.class, remap = false)
public abstract class WrenchCogwheelChainPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void bnt$stripGrousersFirst(ServerPlayer player, CallbackInfo ci) {
        Level level = player.level();
        BlockPos controllerPos = ((WrenchCogwheelChainPacket)(Object)this).controllerPos();
        if (level.isLoaded(controllerPos) && BntReach.track(level, player, controllerPos)
            && BntGrousers.strip(level, controllerPos, player)) {
            ci.cancel();
        }
    }
}
