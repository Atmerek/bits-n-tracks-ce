package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
    @Unique
    private boolean bnt$isTrackModel = false;

    @Override
    public boolean bnt$isTrackModel() {
        if (this.bnt$isTrackModel) {
            return true;
        } else {
            CogwheelChainBehaviour self = (CogwheelChainBehaviour)(Object)this;
            Level level = self.getLevel();
            BlockPos controllerPos = bnt$getControllerPos(self);
            CogwheelChain chain = bnt$getTrackedChain(self);
            if (level != null && controllerPos != null && chain != null) {
                for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
                    if (bnt$isTrackCogwheel(level, controllerPos.offset(node.localPos()))) {
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }
    }

    @Override
    public void bnt$setTrackModel(boolean isTrackModel) {
        this.bnt$isTrackModel = isTrackModel;
    }

    @Inject(
        method = {"write"},
        at = {@At("TAIL")}
    )
    private void bnt$writeTrackModel(CompoundTag compound, Provider registries, boolean clientPacket, CallbackInfo ci) {
        compound.putBoolean("BNT_IsTrackModel", this.bnt$isTrackModel);
    }

    @Inject(
        method = {"read"},
        at = {@At("TAIL")}
    )
    private void bnt$readTrackModel(CompoundTag compound, Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (compound.contains("BNT_IsTrackModel")) {
            this.bnt$isTrackModel = compound.getBoolean("BNT_IsTrackModel");
        }
    }

    @Inject(
        method = {"writeSafe"},
        at = {@At("TAIL")}
    )
    private void bnt$writeSafeTrackModel(CompoundTag tag, Provider registries, CallbackInfo ci) {
        tag.putBoolean("BNT_IsTrackModel", this.bnt$isTrackModel);
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
        method = {"tryTransferOnRemoval"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$skipTransferWithoutChain(CallbackInfoReturnable<Boolean> cir) {
        CogwheelChainBehaviour self = (CogwheelChainBehaviour)(Object)this;
        if (self.isController() || self.getLevel() == null || self.getBlockEntity() == null) {
            return;
        }

        Vec3i offset = self.getControllerOffset();
        if (offset == null) {
            return;
        }

        BlockPos controllerPos = self.getBlockEntity().getBlockPos().offset(offset);
        if (self.getSameBehaviour(controllerPos) instanceof CogwheelChainBehaviour controllerBehaviour
            && controllerBehaviour.getControlledChain() == null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = {"rendersWhenVisualizationAvailable"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void bnt$forceDynamicRenderer(CallbackInfoReturnable<Boolean> cir) {
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
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BntFlangedCogwheelBlock) {
            return true;
        } else if (!HiddenCogwheelCompat.isHiddenCogwheel(state)) {
            return false;
        } else if (level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
            String originalBlock = access.bnt$getOriginalBlock();
            return originalBlock != null && !originalBlock.isEmpty()
                ? HiddenCogwheelCompat.isBitsNTracksId(originalBlock)
                : state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get());
        } else {
            return false;
        }
    }
}
