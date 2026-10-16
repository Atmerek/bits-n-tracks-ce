package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import dev.qwxon.bitsntracks.content.BntVisualized;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltLinks;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {CogwheelChainBehaviour.class},
    remap = false
)
public abstract class CogwheelChainBehaviourMixin implements TrackModelBehaviourAccess {
    @Override
    public boolean bnt$isTrackModel() {
        CogwheelChainBehaviour self = (CogwheelChainBehaviour)(Object)this;
        Level level = self.getLevel();
        BlockPos controllerPos = bnt$getControllerPos(self);
        CogwheelChain chain = bnt$getTrackedChain(self);
        if (level == null || controllerPos == null || chain == null) {
            return false;
        }
        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
            if (bnt$isTrackCogwheel(level, controllerPos.offset(node.localPos()))) {
                return true;
            }
        }
        return false;
    }

    @Inject(
        method = {"remove"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false
    )
    private void bnt$preventDestroyOnSwap(CallbackInfo ci) {
        if (HiddenCogwheelCompat.isSuppressingChainDestroy()) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"onBlockBroken"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$preventBreakOnSwap(BreakEvent event, CallbackInfo ci) {
        if (HiddenCogwheelCompat.isSuppressingChainDestroy()) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"propagateRotationTo"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$blockDisengagedCogwheels(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
                                              boolean connectedViaAxes, boolean connectedViaCogs, CallbackInfoReturnable<Float> cir) {
        CogwheelChainBehaviour self = (CogwheelChainBehaviour)(Object)this;
        if (!bnt$isEngaged(self)) {
            cir.setReturnValue(0.0F);
            return;
        }

        if (target instanceof SmartBlockEntity smartBe
            && smartBe.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour targetBehaviour
            && !bnt$isEngaged(targetBehaviour)) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(
        method = {"propagateRotationTo"},
        at = {@At("RETURN")},
        cancellable = true
    )
    private void bnt$rejectUnusableRatio(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
                                         boolean connectedViaAxes, boolean connectedViaCogs, CallbackInfoReturnable<Float> cir) {
        if (!Float.isFinite(cir.getReturnValueF())) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(
        method = {"getChainRotationFactor"},
        at = {@At("RETURN")},
        cancellable = true
    )
    private void bnt$sizeRotationFactor(CallbackInfoReturnable<Float> cir) {
        float factor = cir.getReturnValueF();
        BlockEntity be = ((CogwheelChainBehaviour)(Object)this).getBlockEntity();
        if (factor == 0.0F || be == null) {
            return;
        }
        Block block = be.getBlockState().getBlock();
        if (CogwheelSizeHelper.sizedByBlock(block)) {
            cir.setReturnValue((float)(Math.signum(factor) * CogwheelSizeHelper.getChainRadius(block)));
        }
    }

    @Unique
    private static boolean bnt$isEngaged(CogwheelChainBehaviour behaviour) {
        return BntChainEngagement.isEngaged(behaviour);
    }

    @Inject(
        method = {"lazyTick"},
        at = {@At("TAIL")}
    )
    private void bnt$verifyChainKinetics(CallbackInfo ci) {
        CogwheelChainBehaviour self = (CogwheelChainBehaviour)(Object)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide || !self.isController() || self.getBlockEntity() == null) {
            return;
        }

        if (self.getControlledChain() instanceof BntChainGeometryRefresh chain) {
            chain.bnt$verifyKinetics(level, self.getBlockEntity().getBlockPos());
            if (chain.bnt$isOwn()) {
                BntBeltLinks.latchIfUnset(level, self.getBlockEntity().getBlockPos());
            }
        }
    }

    @Inject(
        method = {"rendersWhenVisualizationAvailable"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$forceDynamicRenderer(CallbackInfoReturnable<Boolean> cir) {
        BlockEntity be = ((CogwheelChainBehaviour)(Object)this).getBlockEntity();
        if (BntVisualized.ownType(be)) {
            cir.setReturnValue(!BntVisualized.drawn(be));
            return;
        }
        if (this.bnt$isTrackModel() || HiddenCogwheelCompat.shouldForceDynamicRenderer((CogwheelChainBehaviour)(Object)this)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static CogwheelChain bnt$getTrackedChain(CogwheelChainBehaviour self) {
        CogwheelChain directChain = self.getControlledChain();
        if (directChain != null) {
            return directChain;
        } else {
            Level level = self.getLevel();
            BlockPos controllerPos = bnt$getControllerPos(self);
            if (level != null && controllerPos != null) {
                if (level.getBlockEntity(controllerPos) instanceof SmartBlockEntity smartBe) {
                    CogwheelChainBehaviour controllerBehaviour = (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE);
                    if (controllerBehaviour != null) {
                        return controllerBehaviour.getControlledChain();
                    }
                }

                return null;
            } else {
                return null;
            }
        }
    }

    @Unique
    private static BlockPos bnt$getControllerPos(CogwheelChainBehaviour self) {
        if (self.getBlockEntity() == null) {
            return null;
        } else if (self.isController()) {
            return self.getBlockEntity().getBlockPos();
        } else {
            return self.getControllerOffset() == null ? null : self.getBlockEntity().getBlockPos().offset(self.getControllerOffset());
        }
    }

    @Unique
    private static boolean bnt$isTrackCogwheel(Level level, BlockPos pos) {
        return HiddenCogwheelCompat.isOwnCogwheel(level.getBlockState(pos));
    }
}
