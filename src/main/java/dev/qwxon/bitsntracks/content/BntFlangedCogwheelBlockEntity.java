package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntWheelEffects;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltLinks;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltRefit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.BntCogwheelCollision;
import dev.qwxon.bitsntracks.physics.BntExtraMass;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsRegistry;
import dev.qwxon.bitsntracks.physics.BntTuning;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BntFlangedCogwheelBlockEntity extends SimpleKineticBlockEntity implements KineticBlockEntityPhysicsAccess {
    private static final float RPM_PER_RADIAN_TICK = 190.9859F;
    private static final int DETACH_CHECK_TICKS = 20;

    private float physicalSpeed;
    private float alignmentOffsetX;
    private float alignmentOffsetY;
    private float alignmentOffsetZ;
    private boolean hiddenByLever;
    private int trackRouteSide = -1;
    private float beltTension = BntBeltTension.DEFAULT;
    private int beltLinks = BntBeltLinks.UNSET;
    private double beltFit;
    private int grousers;
    private long drawnDropTick = Long.MIN_VALUE;
    private double drawnDrop;
    private double lastDrawnDrop;
    private long trackLiftTick = Long.MIN_VALUE;
    private double trackLift;
    private long beltHoldTick = Long.MIN_VALUE;
    private double beltHold;
    private final int[] tuning = BntTuning.defaults();
    private double extension;
    private double lastExtension;
    private double lastTerrainExtension = Double.NaN;
    private long groundDropTick = Long.MIN_VALUE;
    private double groundDrop;
    private long rawDropTick = Long.MIN_VALUE;
    private double rawDrop;
    private Object terrainCast;
    private boolean liftedUp;
    private double maxAirExtension;
    private double springImpulse;
    private int suspensionSide;
    private int suspensionFacing;
    private final ForceTotal forceTotal = new ForceTotal();
    private boolean queuedForForceApplication;

    public BntFlangedCogwheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.extension = this.restExtension(state);
        this.lastExtension = this.extension;
    }

    private double restExtension(BlockState state) {
        return CogwheelSizeHelper.getSuspensionRest(state.getBlock(), this);
    }

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level != null && level.isClientSide) {
            this.clientTick();
        }
        super.tick();
        if (level != null && !level.isClientSide) {
            this.serverTick(level);
        }
    }

    private void clientTick() {
        if (this.bnt$isPhysicsEnabled() && Sable.HELPER.getContainingClient(this) instanceof SubLevel subLevel) {
            BntPhysicsEvents.updateClientRollingSpeed(this, this, subLevel);
            BntWheelEffects.tick(this, subLevel);
        }
    }

    private void serverTick(Level level) {
        BntExtraMass.tick(this);
        if (!this.bnt$isPhysicsEnabled()) {
            return;
        }
        BntPhysicsRegistry.add(this);
        this.lastExtension = this.extension;
        if (this.suspensionSide != 0
            && (level.getGameTime() + this.getBlockPos().hashCode()) % DETACH_CHECK_TICKS == 0L
            && !BntSuspension.stillHolds(level, this.getBlockPos(), this)) {
            Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, this.getBlockPos());
            BntSuspension.detach(level, this.getBlockPos(), this, true);
            BntChainEngagement.refresh(level, this.getBlockPos(), engagementBefore);
            BntBeltRefit.queue(level, this.getBlockPos());
        }
    }

    public void stateChanging(BlockState state) {
        boolean enabled = BntFlangedCogwheelBlock.physics(state);
        if (BntFlangedCogwheelBlock.physics(this.getBlockState()) == enabled) {
            return;
        }
        if (enabled) {
            BntPhysicsRegistry.add(this);
        } else {
            BntPhysicsRegistry.remove(this);
            this.extension = this.restExtension(state);
            this.lastExtension = this.extension;
            this.liftedUp = false;
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        BntCogwheelCollision.track(this);
    }

    @Override
    public void remove() {
        BntPhysicsRegistry.remove(this);
        BntExtraMass.release(this);
        super.remove();
    }

    @Override
    protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.alignmentOffsetX = tag.getFloat("BntAlignmentOffsetX");
        this.alignmentOffsetY = tag.getFloat("BntAlignmentOffsetY");
        this.alignmentOffsetZ = tag.getFloat("BntAlignmentOffsetZ");
        this.hiddenByLever = tag.getBoolean("BntHiddenByLever");
        this.trackRouteSide = tag.contains("BntTrackRouteSide") ? tag.getInt("BntTrackRouteSide") : -1;
        this.beltTension = tag.contains("BntBeltTension") ? BntBeltTension.clamp(tag.getFloat("BntBeltTension")) : BntBeltTension.DEFAULT;
        this.beltLinks = tag.getInt("BntBeltLinks");
        this.beltFit = tag.getDouble("BntBeltFit");
        this.grousers = Math.max(0, tag.getInt("BntGrousers"));
        this.bnt$setSuspension(tag.getInt("BntSuspensionSide"), tag.getInt("BntSuspensionFacing"));
        boolean current = tag.contains("BntTuningLevels");
        CompoundTag levels = tag.getCompound(current ? "BntTuningLevels" : "BntTuning");
        for (BntTuning setting : BntTuning.values()) {
            int level = levels.getInt(setting.key());
            this.tuning[setting.ordinal()] = !levels.contains(setting.key()) ? BntTuning.DEFAULT
                : current ? BntTuning.clamp(level) : BntTuning.fromOldScale(level);
        }
        if (this.bnt$isPhysicsEnabled()) {
            this.extension = tag.contains("BntExtension", Tag.TAG_DOUBLE) ? tag.getDouble("BntExtension") : this.restExtension(this.getBlockState());
            this.lastExtension = this.extension;
        }
    }

    @Override
    protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        this.writeSettings(tag);
        if (this.bnt$isPhysicsEnabled()) {
            tag.putDouble("BntExtension", this.extension);
        }
    }

    @Override
    public void writeSafe(CompoundTag tag, Provider registries) {
        super.writeSafe(tag, registries);
        this.writeSettings(tag);
    }

    private void writeSettings(CompoundTag tag) {
        tag.putFloat("BntAlignmentOffsetX", this.alignmentOffsetX);
        tag.putFloat("BntAlignmentOffsetY", this.alignmentOffsetY);
        tag.putFloat("BntAlignmentOffsetZ", this.alignmentOffsetZ);
        tag.putBoolean("BntHiddenByLever", this.hiddenByLever);
        tag.putInt("BntTrackRouteSide", this.trackRouteSide);
        tag.putFloat("BntBeltTension", this.beltTension);
        tag.putInt("BntBeltLinks", this.beltLinks);
        tag.putDouble("BntBeltFit", this.beltFit);
        if (this.grousers > 0) {
            tag.putInt("BntGrousers", this.grousers);
        }
        if (this.suspensionSide != 0) {
            tag.putInt("BntSuspensionSide", this.suspensionSide);
            tag.putInt("BntSuspensionFacing", this.suspensionFacing);
        }
        CompoundTag levels = new CompoundTag();
        for (BntTuning setting : BntTuning.values()) {
            if (this.tuning[setting.ordinal()] != BntTuning.DEFAULT) {
                levels.putInt(setting.key(), this.tuning[setting.ordinal()]);
            }
        }
        if (!levels.isEmpty()) {
            tag.put("BntTuningLevels", levels);
        }
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        ItemRequirement requirement = super.getRequiredItems(state);
        if (this.grousers > 0 && this.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour chain && chain.isController()) {
            requirement = requirement.union(new ItemRequirement(ItemUseType.CONSUME, new ItemStack(BitsNTracksItems.GROUSER.get(), this.grousers)));
        }
        return BntFlangedCogwheelBlock.physics(state) && this.suspensionSide != 0 && !BntSuspension.heldByWidePartner(this)
            ? requirement.union(new ItemRequirement(ItemUseType.CONSUME, BitsNTracksItems.SUSPENSION_PIECE.get()))
            : requirement;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        float rolling = this.bnt$isPhysicsEnabled() ? Math.abs(this.physicalSpeed * RPM_PER_RADIAN_TICK) : 0.0F;
        float speed = Math.max(Math.max(Math.abs(this.getSpeed()), Math.abs(this.getTheoreticalSpeed())), rolling);
        if (speed == 0.0F) {
            return;
        }
        float pitch = Mth.clamp(speed / 256.0F + 0.45F, 0.85F, 1.0F);
        if (this.isNoisy()) {
            SoundScapes.play(AmbienceGroup.KINETIC, this.getBlockPos(), pitch);
        }
        SoundScapes.play(AmbienceGroup.COG, this.getBlockPos(), pitch);
    }

    @Override
    public boolean bnt$isPhysicsEnabled() {
        return BntFlangedCogwheelBlock.physics(this.getBlockState());
    }

    @Override
    public double bnt$getExtension() {
        return this.extension;
    }

    @Override
    public void bnt$setExtension(double extension) {
        this.extension = extension;
    }

    @Override
    public double bnt$getLerpedExtension(float partialTick) {
        return Mth.lerp(partialTick, this.lastExtension, this.extension);
    }

    @Override
    public double bnt$getLastTerrainExtension() {
        return this.lastTerrainExtension;
    }

    @Override
    public void bnt$setLastTerrainExtension(double extension) {
        this.lastTerrainExtension = extension;
    }

    @Override
    public long bnt$getGroundDropTick() {
        return this.groundDropTick;
    }

    @Override
    public double bnt$getGroundDrop() {
        return this.groundDrop;
    }

    @Override
    public void bnt$setGroundDrop(long gameTime, double drop) {
        this.groundDropTick = gameTime;
        this.groundDrop = drop;
    }

    @Override
    public long bnt$getRawDropTick() {
        return this.rawDropTick;
    }

    @Override
    public double bnt$getRawDrop() {
        return this.rawDrop;
    }

    @Override
    public void bnt$setRawDrop(long gameTime, double drop) {
        this.rawDropTick = gameTime;
        this.rawDrop = drop;
    }

    @Override
    public Object bnt$getTerrainCast() {
        return this.terrainCast;
    }

    @Override
    public void bnt$setTerrainCast(Object cast) {
        this.terrainCast = cast;
    }

    @Override
    public boolean bnt$isLiftedUp() {
        return this.liftedUp;
    }

    @Override
    public void bnt$setLiftedUp(boolean liftedUp) {
        this.liftedUp = liftedUp;
    }

    @Override
    public double bnt$getMaxAirExtension() {
        return this.maxAirExtension;
    }

    @Override
    public void bnt$setMaxAirExtension(double maxAirExtension) {
        this.maxAirExtension = maxAirExtension;
    }

    @Override
    public double bnt$getSpringImpulse() {
        return this.springImpulse;
    }

    @Override
    public void bnt$setSpringImpulse(double springImpulse) {
        this.springImpulse = springImpulse;
    }

    @Override
    public int bnt$getSuspensionSide() {
        return this.suspensionSide;
    }

    @Override
    public int bnt$getSuspensionFacing() {
        return this.suspensionFacing;
    }

    @Override
    public void bnt$setSuspension(int side, int facing) {
        boolean attached = side != 0 && facing != 0;
        this.suspensionSide = attached ? Mth.clamp(side, -BntSuspension.WIDE_BOGIE, BntSuspension.WIDE_BOGIE) : 0;
        this.suspensionFacing = attached ? Integer.signum(facing) : 0;
        this.springImpulse = 0.0;
    }

    @Override
    public ForceTotal bnt$getForceTotal() {
        return this.forceTotal;
    }

    @Override
    public void bnt$markQueuedForForceApplication() {
        this.queuedForForceApplication = true;
    }

    @Override
    public boolean bnt$consumeQueuedForForceApplication() {
        boolean queued = this.queuedForForceApplication;
        this.queuedForForceApplication = false;
        return queued;
    }

    @Override
    public void bnt$setPhysicalSpeed(float speed) {
        this.physicalSpeed = speed;
    }

    @Override
    public float bnt$getAlignmentOffsetX() {
        return this.alignmentOffsetX;
    }

    @Override
    public void bnt$setAlignmentOffsetX(float x) {
        this.alignmentOffsetX = x;
    }

    @Override
    public float bnt$getAlignmentOffsetY() {
        return this.alignmentOffsetY;
    }

    @Override
    public void bnt$setAlignmentOffsetY(float y) {
        this.alignmentOffsetY = y;
    }

    @Override
    public float bnt$getAlignmentOffsetZ() {
        return this.alignmentOffsetZ;
    }

    @Override
    public void bnt$setAlignmentOffsetZ(float z) {
        this.alignmentOffsetZ = z;
    }

    @Override
    public boolean bnt$isHiddenByLever() {
        return this.hiddenByLever;
    }

    @Override
    public void bnt$setHiddenByLever(boolean hidden) {
        this.hiddenByLever = hidden;
    }

    @Override
    public int bnt$getTrackRouteSide() {
        return this.trackRouteSide;
    }

    @Override
    public void bnt$setTrackRouteSide(int side) {
        this.trackRouteSide = side;
    }

    @Override
    public float bnt$getBeltTension() {
        return this.beltTension;
    }

    @Override
    public void bnt$setBeltTension(float tension) {
        this.beltTension = BntBeltTension.clamp(tension);
    }

    @Override
    public int bnt$getBeltLinks() {
        return this.beltLinks;
    }

    @Override
    public void bnt$setBeltLinks(int links) {
        this.beltLinks = Math.max(BntBeltLinks.UNSET, links);
    }

    @Override
    public int bnt$getGrousers() {
        return this.grousers;
    }

    @Override
    public void bnt$setGrousers(int grousers) {
        this.grousers = Math.max(0, grousers);
    }

    @Override
    public long bnt$getDrawnDropTick() {
        return this.drawnDropTick;
    }

    @Override
    public void bnt$advanceDrawnDrop(long gameTime, double sample, double rate) {
        boolean running = this.drawnDropTick == gameTime - 1L;
        this.lastDrawnDrop = running ? this.drawnDrop : sample;
        this.drawnDrop = running ? Mth.lerp(rate, this.drawnDrop, sample) : sample;
        this.drawnDropTick = gameTime;
    }

    @Override
    public double bnt$getDrawnDrop(float partialTick) {
        return Mth.lerp(partialTick, this.lastDrawnDrop, this.drawnDrop);
    }

    @Override
    public double bnt$getBeltFit() {
        return this.beltFit;
    }

    @Override
    public void bnt$setBeltFit(double fit) {
        this.beltFit = Math.max(0.0, fit);
    }

    @Override
    public long bnt$getTrackLiftTick() {
        return this.trackLiftTick;
    }

    @Override
    public double bnt$getTrackLift() {
        return this.trackLift;
    }

    @Override
    public void bnt$setTrackLift(long tick, double lift) {
        this.trackLiftTick = tick;
        this.trackLift = lift;
    }

    @Override
    public long bnt$getBeltHoldTick() {
        return this.beltHoldTick;
    }

    @Override
    public double bnt$getBeltHold() {
        return this.beltHold;
    }

    @Override
    public void bnt$setBeltHold(long tick, double hold) {
        this.beltHoldTick = tick;
        this.beltHold = hold;
    }

    @Override
    public int bnt$getTuning(BntTuning setting) {
        return this.tuning[setting.ordinal()];
    }

    @Override
    public void bnt$setTuning(BntTuning setting, int level) {
        this.tuning[setting.ordinal()] = BntTuning.clamp(level);
    }
}
