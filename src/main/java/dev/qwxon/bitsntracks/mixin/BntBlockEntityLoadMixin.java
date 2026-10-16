package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.physics.BntCogwheelCollision;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BntBlockEntityLoadMixin {
    @Shadow
    private BlockState blockState;

    @Inject(method = "clearRemoved", at = @At("TAIL"))
    private void bnt$trackCogwheel(CallbackInfo ci) {
        if ((Object)this instanceof KineticBlockEntity be && be.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock) {
            BntCogwheelCollision.track(be);
        }
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void bnt$noticePhysicsChange(BlockState state, CallbackInfo ci) {
        boolean enabled = BntFlangedCogwheelBlock.physics(state);
        if ((Object)this instanceof KineticBlockEntityPhysicsAccess access && BntFlangedCogwheelBlock.physics(this.blockState) != enabled) {
            access.bnt$physicsChanged(state, enabled);
        }
    }
}
