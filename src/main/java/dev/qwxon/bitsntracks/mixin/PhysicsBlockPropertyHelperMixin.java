package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    value = {PhysicsBlockPropertyHelper.class},
    remap = false
)
public class PhysicsBlockPropertyHelperMixin {
    @WrapOperation(
        method = {"getMass", "getInertia"},
        at = @At(
            value = "INVOKE",
            target = "Ldev/ryanhcode/sable/physics/chunk/VoxelNeighborhoodState;isSolid(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        ),
        require = 2
    )
    private static boolean bnt$weighHiddenCogwheel(BlockGetter level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        return BntFlangedCogwheelBlock.physics(state) || original.call(level, pos, state);
    }
}
