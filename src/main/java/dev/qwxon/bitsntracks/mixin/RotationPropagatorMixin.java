package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RotationPropagator.class})
public class RotationPropagatorMixin {
    @Inject(
        method = {"getRotationSpeedModifier(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false
    )
    private static void bnt$interceptGetRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to, CallbackInfoReturnable<Float> cir) {
        BlockState stateFrom = from.getBlockState();
        BlockState stateTo = to.getBlockState();
        Block blockFrom = stateFrom.getBlock();
        Block blockTo = stateTo.getBlock();
        boolean isFromMedium = CogwheelSizeHelper.isMedium(blockFrom);
        boolean isToMedium = CogwheelSizeHelper.isMedium(blockTo);
        boolean isFromTiny = CogwheelSizeHelper.isTiny(blockFrom);
        boolean isToTiny = CogwheelSizeHelper.isTiny(blockTo);
        if (isFromMedium || isToMedium || isFromTiny || isToTiny) {
            if (blockFrom instanceof IRotate defFrom && blockTo instanceof IRotate defTo) {
                BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
                double sizeFrom = getSizeMultiplier(blockFrom);
                double sizeTo = getSizeMultiplier(blockTo);
                if (sizeFrom != sizeTo
                    && (isLargeToSmallCogCompatible(stateFrom, stateTo, defTo, diff) || isLargeToSmallCogCompatible(stateTo, stateFrom, defFrom, diff))) {
                    cir.setReturnValue((float)(-(sizeFrom / sizeTo)));
                }
            }
        }
    }

    private static double getSizeMultiplier(Block block) {
        if (CogwheelSizeHelper.isLarge(block)) {
            return 2.0;
        } else if (CogwheelSizeHelper.isMedium(block)) {
            return 1.5;
        } else {
            return CogwheelSizeHelper.isTiny(block) ? 0.5 : 1.0;
        }
    }

    private static boolean isLargeToSmallCogCompatible(BlockState from, BlockState to, IRotate defTo, BlockPos diff) {
        if (!from.hasProperty(BlockStateProperties.AXIS)) {
            return false;
        } else {
            Axis axisFrom = (Axis)from.getValue(BlockStateProperties.AXIS);
            if (axisFrom != defTo.getRotationAxis(to)) {
                return false;
            } else if (axisFrom.choose(diff.getX(), diff.getY(), diff.getZ()) != 0) {
                return false;
            } else {
                int absDx = 0;
                int absDy = 0;
                int absDz = 0;
                if (axisFrom != Axis.X) {
                    absDx = Math.abs(diff.getX());
                }

                if (axisFrom != Axis.Y) {
                    absDy = Math.abs(diff.getY());
                }

                if (axisFrom != Axis.Z) {
                    absDz = Math.abs(diff.getZ());
                }

                int sum = absDx + absDy + absDz;
                int max = Math.max(absDx, Math.max(absDy, absDz));
                if (max != 1) {
                    return false;
                } else if (sum != 1 && sum != 2) {
                    return false;
                } else {
                    double radiusFrom = getSizeMultiplier(from.getBlock()) / 2.0;
                    double radiusTo = getSizeMultiplier(to.getBlock()) / 2.0;
                    double radiusSum = radiusFrom + radiusTo;
                    return sum == 1 ? radiusSum >= 0.99 : radiusSum >= 1.41;
                }
            }
        }
    }
}
