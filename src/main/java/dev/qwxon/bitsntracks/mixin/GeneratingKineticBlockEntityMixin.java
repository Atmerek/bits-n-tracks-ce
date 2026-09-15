package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    value = {GeneratingKineticBlockEntity.class},
    remap = false
)
public abstract class GeneratingKineticBlockEntityMixin {
    /** Stops the chain instead of letting an overpowered source break itself. */
    @WrapOperation(
        method = {"applyNewSpeed(FF)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        require = 0
    )
    private boolean bnt$stopInsteadOfBreaking(Level level, BlockPos pos, boolean drop, Operation<Boolean> original) {
        if (BntChainEngagement.stopChainNetwork(level, (GeneratingKineticBlockEntity)(Object)this, "an overpowered source")) {
            return false;
        }
        return original.call(level, pos, drop);
    }
}
