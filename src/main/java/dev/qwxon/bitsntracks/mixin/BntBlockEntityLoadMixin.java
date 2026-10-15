package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.physics.BntCogwheelCollision;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BntBlockEntityLoadMixin {
    @Inject(method = "clearRemoved", at = @At("TAIL"))
    private void bnt$trackHiddenCogwheel(CallbackInfo ci) {
        if ((Object)this instanceof KineticBlockEntity be && be.getBlockState().getBlock() instanceof HiddenCogwheelBlock) {
            BntCogwheelCollision.track(be);
        }
    }
}
