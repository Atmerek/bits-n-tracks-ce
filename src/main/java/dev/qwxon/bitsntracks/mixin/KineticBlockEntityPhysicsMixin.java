package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsRegistry;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KineticBlockEntity.class})
public abstract class KineticBlockEntityPhysicsMixin implements KineticBlockEntityPhysicsAccess, TransformableBlockEntity {
    @Unique
    private boolean bnt$physicsEnabled = false;
    @Unique
    private String bnt$originalBlock = null;
    @Unique
    private float bnt$physicalSpeed = 0.0F;
    @Unique
    private float bnt$alignmentOffsetX = 0.0F;
    @Unique
    private float bnt$alignmentOffsetY = 0.0F;
    @Unique
    private float bnt$alignmentOffsetZ = 0.0F;
    @Unique
    private boolean bnt$hiddenByLever = false;
    @Unique
    private int bnt$trackRouteSide = -1;
    @Unique
    private double bnt$extension = 0.65;
    @Unique
    private double bnt$lastExtension = 0.65;
    @Unique
    private boolean bnt$liftedUp = false;
    @Unique
    private double bnt$maxAirExtension = 0.0;
    @Unique
    private final ForceTotal bnt$forceTotal = new ForceTotal();
    @Unique
    private boolean bnt$queuedForForceApplication = false;

    @Shadow(
        remap = false
    )
    protected abstract boolean isNoisy();

    @Shadow(
        remap = false
    )
    public abstract float getSpeed();

    @Shadow(
        remap = false
    )
    public abstract float getTheoreticalSpeed();

    @Override
    public float bnt$getAlignmentOffsetX() {
        return this.bnt$alignmentOffsetX;
    }

    @Override
    public void bnt$setAlignmentOffsetX(float x) {
        this.bnt$alignmentOffsetX = x;
    }

    @Override
    public float bnt$getAlignmentOffsetY() {
        return this.bnt$alignmentOffsetY;
    }

    @Override
    public void bnt$setAlignmentOffsetY(float y) {
        this.bnt$alignmentOffsetY = y;
    }

    @Override
    public float bnt$getAlignmentOffsetZ() {
        return this.bnt$alignmentOffsetZ;
    }

    @Override
    public void bnt$setAlignmentOffsetZ(float z) {
        this.bnt$alignmentOffsetZ = z;
    }

    @Override
    public boolean bnt$isHiddenByLever() {
        return this.bnt$hiddenByLever;
    }

    @Override
    public void bnt$setHiddenByLever(boolean hidden) {
        this.bnt$hiddenByLever = hidden;
    }

    @Override
    public int bnt$getTrackRouteSide() {
        return this.bnt$trackRouteSide;
    }

    @Override
    public void bnt$setTrackRouteSide(int side) {
        this.bnt$trackRouteSide = side;
    }

    @Override
    public void bnt$setPhysicalSpeed(float speed) {
        this.bnt$physicalSpeed = speed;
    }

    @Override
    public String bnt$getOriginalBlock() {
        return this.bnt$originalBlock;
    }

    @Override
    public void bnt$setOriginalBlock(String originalBlock) {
        this.bnt$originalBlock = originalBlock;
    }

    @Override
    public boolean bnt$isPhysicsEnabled() {
        return this.bnt$physicsEnabled;
    }

    @Override
    public void bnt$setPhysicsEnabled(boolean enabled) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        this.bnt$physicsEnabled = enabled;
        if (enabled) {
            if (this.bnt$originalBlock == null && !HiddenCogwheelCompat.isHiddenCogwheel(self.getBlockState())) {
                this.bnt$originalBlock = BuiltInRegistries.BLOCK.getKey(self.getBlockState().getBlock()).toString();
            }

            BntPhysicsRegistry.add(self);
        } else {
            BntPhysicsRegistry.remove(self);
            double rest = CogwheelSizeHelper.getSuspensionRest(self.getBlockState().getBlock());
            this.bnt$extension = rest;
            this.bnt$lastExtension = rest;
            this.bnt$liftedUp = false;
        }

        self.setChanged();
        self.sendData();
    }

    @Override
    public double bnt$getExtension() {
        return this.bnt$extension;
    }

    @Override
    public void bnt$setExtension(double extension) {
        this.bnt$extension = extension;
    }

    @Override
    public double bnt$getLerpedExtension(float partialTick) {
        return Mth.lerp(partialTick, this.bnt$lastExtension, this.bnt$extension);
    }

    @Override
    public boolean bnt$isLiftedUp() {
        return this.bnt$liftedUp;
    }

    @Override
    public void bnt$setLiftedUp(boolean liftedUp) {
        this.bnt$liftedUp = liftedUp;
    }

    @Override
    public double bnt$getMaxAirExtension() {
        return this.bnt$maxAirExtension;
    }

    @Override
    public void bnt$setMaxAirExtension(double maxAirExtension) {
        this.bnt$maxAirExtension = maxAirExtension;
    }

    @Override
    public ForceTotal bnt$getForceTotal() {
        return this.bnt$forceTotal;
    }

    @Override
    public void bnt$markQueuedForForceApplication() {
        this.bnt$queuedForForceApplication = true;
    }

    @Override
    public boolean bnt$consumeQueuedForForceApplication() {
        if (!this.bnt$queuedForForceApplication) {
            return false;
        } else {
            this.bnt$queuedForForceApplication = false;
            return true;
        }
    }

    @Inject(
        method = {"tick"},
        at = {@At("TAIL")}
    )
    private void bnt$tick(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        if (this.bnt$physicsEnabled) {
            if (self.getLevel() != null) {
                BntPhysicsRegistry.add(self);
            }

            if (!this.bnt$tryMigrateToHiddenState(self)) {
                this.bnt$lastExtension = this.bnt$extension;
                if (self.getLevel() != null && self.getLevel().isClientSide) {
                    BntPhysicsEvents.updateClientVisual(self, this);
                }
            }
        }
    }

    @Inject(
        method = {"read"},
        at = {@At("TAIL")}
    )
    private void bnt$read(CompoundTag tag, Provider registries, boolean clientPacket, CallbackInfo ci) {
        this.bnt$physicsEnabled = tag.getBoolean("BntPhysicsEnabled");
        this.bnt$originalBlock = tag.contains("BntOriginalBlock") ? tag.getString("BntOriginalBlock") : null;
        this.bnt$alignmentOffsetX = tag.getFloat("BntAlignmentOffsetX");
        this.bnt$alignmentOffsetY = tag.getFloat("BntAlignmentOffsetY");
        this.bnt$alignmentOffsetZ = tag.getFloat("BntAlignmentOffsetZ");
        this.bnt$hiddenByLever = tag.getBoolean("BntHiddenByLever");
        this.bnt$trackRouteSide = tag.contains("BntTrackRouteSide") ? tag.getInt("BntTrackRouteSide") : -1;
        if (this.bnt$physicsEnabled) {
            KineticBlockEntity self = (KineticBlockEntity)(Object)this;
            double rest = CogwheelSizeHelper.getSuspensionRest(self.getBlockState().getBlock());
            this.bnt$extension = tag.contains("BntExtension", 6) ? tag.getDouble("BntExtension") : rest;
            this.bnt$lastExtension = this.bnt$extension;
        }
    }

    @Inject(
        method = {"write"},
        at = {@At("TAIL")}
    )
    private void bnt$write(CompoundTag tag, Provider registries, boolean clientPacket, CallbackInfo ci) {
        tag.putBoolean("BntPhysicsEnabled", this.bnt$physicsEnabled);
        if (this.bnt$originalBlock != null) {
            tag.putString("BntOriginalBlock", this.bnt$originalBlock);
        }

        tag.putFloat("BntAlignmentOffsetX", this.bnt$alignmentOffsetX);
        tag.putFloat("BntAlignmentOffsetY", this.bnt$alignmentOffsetY);
        tag.putFloat("BntAlignmentOffsetZ", this.bnt$alignmentOffsetZ);
        tag.putBoolean("BntHiddenByLever", this.bnt$hiddenByLever);
        tag.putInt("BntTrackRouteSide", this.bnt$trackRouteSide);
        if (this.bnt$physicsEnabled) {
            tag.putDouble("BntExtension", this.bnt$extension);
        }
    }

    @Inject(
        method = {"remove"},
        at = {@At("HEAD")}
    )
    private void bnt$onRemoved(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        BntPhysicsRegistry.remove(self);
    }

    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        HiddenCogwheelCompat.transformControlledChain((KineticBlockEntity)(Object)this, transform);
    }

    @Unique
    private boolean bnt$tryMigrateToHiddenState(KineticBlockEntity self) {
        if (self.getLevel() == null || self.getLevel().isClientSide || !this.bnt$physicsEnabled) {
            return false;
        } else if (HiddenCogwheelCompat.isHiddenFlangedCogwheel(self.getBlockState())) {
            return false;
        } else {
            CompoundTag tag = self.saveWithoutMetadata(self.getLevel().registryAccess());
            if (!HiddenCogwheelCompat.isHiddenCogwheel(self.getBlockState())) {
                tag.putString("BntOriginalBlock", BuiltInRegistries.BLOCK.getKey(self.getBlockState().getBlock()).toString());
            }

            BlockState hiddenState = HiddenCogwheelCompat.toHiddenCogwheelState(self.getBlockState());
            if (hiddenState == null) {
                return false;
            } else {
                HiddenCogwheelCompat.replaceBlockForPhysicsSwap(self.getLevel(), self.getBlockPos(), hiddenState);
                HiddenCogwheelCompat.restoreBlockEntity(self.getLevel(), self.getBlockPos(), tag, true);
                BntChainEngagement.rebuild(self.getLevel(), self.getBlockPos());
                return true;
            }
        }
    }

    @Inject(
        method = {"tickAudio"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false
    )
    private void bnt$tickAudio(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        Block block = self.getBlockState().getBlock();
        boolean isBntCog = block instanceof HiddenCogwheelBlock || block instanceof BntFlangedCogwheelBlock;
        if (this.bnt$physicsEnabled || isBntCog) {
            float physicalSpeedRpm = this.bnt$physicsEnabled ? Math.abs(this.bnt$physicalSpeed * 190.9859F) : 0.0F;
            float speedToUse = Math.max(Math.abs(this.getSpeed()), Math.abs(this.getTheoreticalSpeed()));
            speedToUse = Math.max(speedToUse, physicalSpeedRpm);
            if (speedToUse == 0.0F) {
                ci.cancel();
                return;
            }

            float pitch = Mth.clamp(speedToUse / 256.0F + 0.45F, 0.85F, 1.0F);
            if (this.isNoisy()) {
                SoundScapes.play(AmbienceGroup.KINETIC, self.getBlockPos(), pitch);
            }

            if (ICogWheel.isSmallCog(block) || ICogWheel.isLargeCog(block) || block instanceof GearboxBlock || isBntCog) {
                SoundScapes.play(AmbienceGroup.COG, self.getBlockPos(), pitch);
            }

            ci.cancel();
        }
    }
}
