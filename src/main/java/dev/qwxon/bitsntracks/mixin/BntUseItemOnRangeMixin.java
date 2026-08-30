package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class BntUseItemOnRangeMixin {
    @Shadow
    public ServerPlayer player;

    @Redirect(
        method = "handleUseItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 bnt$relativeToDrawnCogwheel(Vec3 location, Vec3 center) {
        Vec3 relative = location.subtract(center);
        Level level = this.player.level();
        BlockPos pos = BlockPos.containing(center);
        if (!HiddenCogwheelCompat.isFlangedCogwheelBlock(level.getBlockState(pos))) {
            return relative;
        }

        return relative.subtract(HiddenCogwheelCompat.getModelTranslation(level.getBlockEntity(pos), 1.0F));
    }
}
