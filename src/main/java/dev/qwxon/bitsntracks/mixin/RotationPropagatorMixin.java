package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainStops;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RotationPropagator.class})
public class RotationPropagatorMixin {
    @Unique
    private static KineticBlockEntity bnt$reentered;
    @Unique
    private static int bnt$reentries;

    @Inject(
        method = {"getRotationSpeedModifier(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false
    )
    private static void bnt$interceptGetRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to, CallbackInfoReturnable<Float> cir) {
        if (BntChainEngagement.sharesShaft(from, to)) {
            return;
        }
        if (BntChainEngagement.sharesChain(from, to)) {
            if (!BntChainEngagement.isEngaged(BntChainEngagement.chainBehaviour(from))
                || !BntChainEngagement.isEngaged(BntChainEngagement.chainBehaviour(to))) {
                cir.setReturnValue(0.0F);
            }
            return;
        }
        if (BntChainEngagement.meshingWouldFightChain(from, to)) {
            cir.setReturnValue(0.0F);
            return;
        }

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

    /** Stops the chain at the end of the tick instead of breaking the driving block, until that keeps failing. */
    @WrapOperation(
        method = {"propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        require = 0,
        remap = false
    )
    private static boolean bnt$stopInsteadOfBreaking(Level level, BlockPos pos, boolean drop, Operation<Boolean> original) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic && BntChainStops.intercept(level, kinetic)) {
            return false;
        }
        return original.call(level, pos, drop);
    }

    @WrapOperation(
        method = {"propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/RotationPropagator;getConveyedSpeed(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F"
        ),
        require = 0,
        remap = false
    )
    private static float bnt$settleConveyed(KineticBlockEntity from, KineticBlockEntity to, Operation<Float> original) {
        return BntChainEngagement.settleConveyed(from, to, original.call(from, to));
    }

    /** A block handed back and forth between two sources re-enters itself until the stack runs out. */
    @WrapOperation(
        method = {"propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/RotationPropagator;propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"
        ),
        require = 0,
        remap = false
    )
    private static void bnt$cutRunawayResourcing(KineticBlockEntity next, Operation<Void> original) {
        KineticBlockEntity outer = bnt$reentered;
        int outerCount = bnt$reentries;
        bnt$reentries = next == outer ? outerCount + 1 : 1;
        bnt$reentered = next;
        try {
            if (bnt$reentries > BntChainStops.RUNAWAY_REENTRIES) {
                BntChainStops.runaway(next.getLevel(), next);
                return;
            }
            original.call(next);
        } finally {
            bnt$reentered = outer;
            bnt$reentries = outerCount;
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
