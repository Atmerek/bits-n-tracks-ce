package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BntBlockEntityStateMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void bnt$noticeStateChange(BlockState state, CallbackInfo ci) {
        if ((Object)this instanceof BntFlangedCogwheelBlockEntity cogwheel) {
            cogwheel.stateChanging(state);
        }
    }
}
