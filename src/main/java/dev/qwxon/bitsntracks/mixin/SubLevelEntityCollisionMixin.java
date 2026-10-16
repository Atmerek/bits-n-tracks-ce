package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.physics.BntCogwheelCollision;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    value = {SubLevelEntityCollision.class},
    remap = false
)
public class SubLevelEntityCollisionMixin {
    @WrapOperation(
        method = {"getSubLevelEntityCollisionShape", "hasCollision"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
        ),
        require = 2
    )
    private static VoxelShape bnt$collideWithPhysicsCogwheel(BlockState state, BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        return BntFlangedCogwheelBlock.physics(state)
            ? ((BntFlangedCogwheelBlock)state.getBlock()).entityCollisionShape(state, level, pos)
            : original.call(state, level, pos);
    }

    @WrapOperation(
        method = {"collide"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"
        ),
        require = 1
    )
    private static Iterable<BlockPos> bnt$reachDisplacedCogwheels(
        BlockPos from, BlockPos to, Operation<Iterable<BlockPos>> original, @Local(argsOnly = true) Entity entity
    ) {
        return BntCogwheelCollision.withDisplaced(entity.level(), from, to, original.call(from, to));
    }
}
