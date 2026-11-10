package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainStops;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RotationPropagator.class})
public class RotationPropagatorMixin {
    @Unique
    private static final ThreadLocal<Set<KineticBlockEntity>> bnt$onStack =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<KineticBlockEntity, Boolean>()));

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
            if (!BntChainEngagement.isEngaged(BntChains.behaviour(from)) || !BntChainEngagement.isEngaged(BntChains.behaviour(to))) {
                cir.setReturnValue(0.0F);
            }
            return;
        }
        if (BntChainEngagement.cutOffFromNeighbour(from, to) || BntChainEngagement.meshingWouldFightChain(from, to)) {
            cir.setReturnValue(0.0F);
        }
    }

    @WrapOperation(
        method = {"propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            ordinal = 0
        ),
        require = 0,
        remap = false
    )
    private static boolean bnt$delayOverspeedBreak(Level level, BlockPos pos, boolean drop, Operation<Boolean> original) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic && BntChainStops.overspeed(level, kinetic)) {
            return false;
        }
        return original.call(level, pos, drop);
    }

    @ModifyExpressionValue(
        method = {"propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Integer;intValue()I"
        ),
        require = 0,
        remap = false
    )
    private static int bnt$overspeedLimit(int limit, @Local(argsOnly = true) KineticBlockEntity current) {
        return BntChainStops.speedLimit(current, limit);
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
        Set<KineticBlockEntity> onStack = bnt$onStack.get();
        if (!onStack.add(next)) {
            BntChainStops.runaway(next.getLevel(), next);
            return;
        }
        try {
            original.call(next);
        } finally {
            onStack.remove(next);
        }
    }
}
