package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BntPhysicsEvents {
    private static final double NO_GROUND = 5.0;
    private static final double CAST_HEADROOM = 0.0625;
    private static final double BLOCKS_PER_SECOND_PER_RPM_RADIUS = Math.PI * 2.0 / 60.0;
    private static final int TRACTION_ITERATIONS = 8;
    private static final double MAX_COMMANDED_YAW_RATE = 20.0;
    private static final int SUPPORT_ITERATIONS = 8;
    private static final double CONTACT_REACH = 1.0 / 16.0;
    private static final double REFERENCE_STEP = 0.05 / 2.0;
    private static final double LEVEL_FOUR_DENOMINATOR = 1.0
        + BntPhysicsTuning.SUSPENSION_GAIN * BntPhysicsTuning.SPRING_SCALE * BntPhysicsTuning.WHEEL_SPRING
            * REFERENCE_STEP * REFERENCE_STEP
        + BntPhysicsTuning.SUSPENSION_GAIN * BntPhysicsTuning.DAMPING_SCALE * BntPhysicsTuning.WHEEL_DAMPING * REFERENCE_STEP;
    private static final double DAMPING_CALIBRATION = 1.0 / Math.sqrt(LEVEL_FOUR_DENOMINATOR);
    private static final double CONTACT_GAP = 1.0 / 32.0;
    private static final int ARM_ITERATIONS = 4;
    private static final double ARM_TOLERANCE = 1.0E-3;
    private static final double RIGID_SLOP = 0.005;
    private static final double RIGID_BIAS = 0.2;
    private static final double ARM_SPRING_RATE = 0.15;
    private static final double SHARE_MEMORY = 3.0;
    private static final double WAKE_SHARE = 0.05;
    private static final double GRIP_LOAD = 0.5;
    private static final Map<ServerSubLevel, Long> REPORTED = new WeakHashMap<>();
    private static final Map<ServerSubLevel, double[]> SHARES = new WeakHashMap<>();

    private BntPhysicsEvents() {
    }

    public static void register() {
        SableEventPlatform.INSTANCE.onPhysicsTick(BntPhysicsEvents::onPhysicsTick);
    }

    public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();
        Map<ServerSubLevel, List<BntPhysicsEvents.WheelContact>> contactsByBody = new Reference2ObjectOpenHashMap<>();
        Map<ServerSubLevel, List<KineticBlockEntity>> wheelsByBody = new Reference2ObjectOpenHashMap<>();

        Iterator<KineticBlockEntity> iterator = BntPhysicsRegistry.getEnabled(level).iterator();

        while (iterator.hasNext()) {
            KineticBlockEntity kbe = iterator.next();
            if (kbe.isRemoved()) {
                iterator.remove();
            } else {
                KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
                if (mixin.bnt$isPhysicsEnabled() && Sable.HELPER.getContaining(kbe) instanceof ServerSubLevel subLevel && !subLevel.isRemoved()) {
                    if (level.getGameTime() % 20L == 0L) {
                        BntChainEngagement.clearPhantomDrive(level, kbe);
                    }

                    wheelsByBody.computeIfAbsent(subLevel, ignored -> new ArrayList<>()).add(kbe);
                    BntPhysicsEvents.WheelContact contact = resolveContact(kbe, mixin, subLevel);
                    if (contact != null) {
                        contactsByBody.computeIfAbsent(subLevel, ignored -> new ArrayList<>()).add(contact);
                    }
                }
            }
        }

        for (Map.Entry<ServerSubLevel, List<KineticBlockEntity>> entry : wheelsByBody.entrySet()) {
            ServerSubLevel subLevel = entry.getKey();
            List<BntPhysicsEvents.WheelContact> nearGround = contactsByBody.getOrDefault(subLevel, List.of());
            BntBeltContacts.BntBeltLoads belt = BntBeltContacts.build(subLevel, entry.getValue(), level);
            KineticBlockEntityPhysicsAccess carrier = (KineticBlockEntityPhysicsAccess)entry.getValue().get(0);

            List<BntPhysicsEvents.WheelContact> loaded = new ArrayList<>(nearGround.size());
            for (BntPhysicsEvents.WheelContact contact : nearGround) {
                if (contact.loaded) {
                    contact.previousPush = contact.mixin.bnt$getSpringImpulse();
                    loaded.add(contact);
                }
            }
            for (KineticBlockEntity wheel : entry.getValue()) {
                ((KineticBlockEntityPhysicsAccess)wheel).bnt$setSpringImpulse(0.0);
            }

            List<BntPhysicsEvents.WheelContact> contacts = new ArrayList<>(loaded);
            for (BntBeltContacts.BntBeltContact beltContact : belt.contacts()) {
                contacts.add(fromBelt(beltContact, carrier));
            }

            double share = wheelShare(subLevel, nearGround.size(), timeStep);
            if (!contacts.isEmpty()) {
                if (solveSupport(level, subLevel, contacts, share, belt.contacts().size(), timeStep)) {
                    physicsSystem.getPipeline().wakeUp(subLevel);
                }
                solveTraction(level, subLevel, contacts, share, timeStep);
                for (BntPhysicsEvents.WheelContact contact : contacts) {
                    applyContactForces(contact);
                }
                for (BntPhysicsEvents.WheelContact contact : loaded) {
                    contact.mixin.bnt$setSpringImpulse(contact.push);
                }
            }

            for (BntBeltContacts.BntBeltWeight weight : belt.weights()) {
                BntBeltContacts.assignCarrier(weight, carrier);
                BntBeltContacts.applyWeight(weight, level, timeStep);
            }
            report(level, subLevel, entry.getValue().size(), nearGround, contacts);
        }

        applyAllBatchedForces(level);
    }

    public static void updateClientRollingSpeed(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, SubLevel subLevel) {
        Level level = kbe.getLevel();
        BlockState state = kbe.getBlockState();
        if (level != null && state != null && state.hasProperty(BlockStateProperties.AXIS)) {
            Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
            Vec3 localPos = getWheelCenter(kbe, state);
            Pose3dc pose = subLevel.logicalPose();
            Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(localPos.x, localPos.y, localPos.z));
            Vector3d localVelocity = pose.transformNormalInverse(new Vector3d(velocity)).div(20.0);
            Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
            Vec3i normalVec;
            if (axis == Axis.Y) {
                normalVec = new Vec3i(1, 0, 0);
            } else {
                normalVec = new Vec3i(-sideVec.getZ(), 0, sideVec.getX());
            }

            Vector3dc normalD = new Vector3d(normalVec.getX(), normalVec.getY(), normalVec.getZ());
            double translation = localVelocity.dot(normalD);
            double wheelRadius = CogwheelSizeHelper.getChainRadius(state.getBlock());
            double angularVelocity = translation / wheelRadius;
            if (axis == Axis.Z) {
                angularVelocity = -angularVelocity;
            }

            mixin.bnt$setPhysicalSpeed((float)angularVelocity);
        }
    }

    public static void updateClientVisual(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin) {
        Level level = kbe.getLevel();
        if (level != null && level.isClientSide) {
            BlockState state = kbe.getBlockState();
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                SubLevel subLevel = Sable.HELPER.getContainingClient(kbe);
                if (subLevel != null) {
                    mixin.bnt$setExtension(Mth.lerp(0.7, mixin.bnt$getExtension(), computeMaxExtensionVisual(kbe, mixin, subLevel)));
                    updateClientRollingSpeed(kbe, mixin, subLevel);
                }
            }
        }
    }

    /** Drop the chain is shaped by, raw terrain only. */
    public static double getClientRenderExtension(KineticBlockEntity kbe, float partialTick) {
        return getRawRenderExtension(kbe, partialTick);
    }

    /** Drop a wheel is drawn at, eased across the tick so terrain steps do not snap. */
    public static double getHeldRenderExtension(KineticBlockEntity kbe, float partialTick) {
        double staged = BntPonderPhysics.wheelDrop(kbe);
        if (!Double.isNaN(staged)) {
            return staged;
        }

        Level level = kbe.getLevel();
        if (level == null || !level.isClientSide || !(kbe instanceof KineticBlockEntityPhysicsAccess mixin)) {
            return heldDrop(kbe, partialTick);
        }

        long now = level.getGameTime();
        if (mixin.bnt$getDrawnDropTick() != now) {
            mixin.bnt$advanceDrawnDrop(now, heldDrop(kbe, 1.0F), BntPhysicsTuning.getSuspensionSmoothing());
        }
        return mixin.bnt$getDrawnDrop(partialTick);
    }

    /** Terrain drop less what the belt holds the wheel up by and what the track carries it on. */
    private static double heldDrop(KineticBlockEntity kbe, float partialTick) {
        double raw = getRawRenderExtension(kbe, partialTick);
        Level level = kbe.getLevel();
        return raw <= 0.0
            ? raw
            : Math.max(0.0, raw - BntBeltHold.at(level, kbe) - BntTrackFloor.at(level, kbe));
    }

    /** Drop terrain alone puts a wheel at, before the belt has a say. */
    public static double getRawRenderExtension(KineticBlockEntity kbe, float partialTick) {
        if (kbe instanceof KineticBlockEntityPhysicsAccess mixin && mixin.bnt$isPhysicsEnabled()) {
            double staged = BntPonderPhysics.wheelDrop(kbe);
            if (!Double.isNaN(staged)) {
                return staged;
            }

            Level level = kbe.getLevel();
            if (level != null && level.isClientSide) {
                BlockState state = kbe.getBlockState();
                if (!state.hasProperty(BlockStateProperties.AXIS)) {
                    return mixin.bnt$getLerpedExtension(partialTick);
                } else {
                    ClientSubLevel subLevel = Sable.HELPER.getContainingClient(kbe);
                    if (subLevel == null) {
                        return 0.0;
                    }
                    if (partialTick != 1.0F) {
                        return computeRenderExtensionForPose(kbe, subLevel.renderPose(partialTick), subLevel);
                    }
                    long now = level.getGameTime();
                    if (mixin.bnt$getRawDropTick() != now) {
                        mixin.bnt$setRawDrop(now, computeRenderExtensionForPose(kbe, subLevel.renderPose(1.0F), subLevel));
                    }
                    return mixin.bnt$getRawDrop();
                }
            } else {
                BlockState state = kbe.getBlockState();
                return state.hasProperty(BlockStateProperties.AXIS) && Sable.HELPER.getContaining(kbe) instanceof SubLevel subLevel
                    ? computeRenderExtensionForPose(kbe, subLevel.logicalPose(), subLevel)
                    : 0.0;
            }
        } else {
            return 0.0;
        }
    }

    @Nullable
    private static BntPhysicsEvents.WheelContact resolveContact(
        KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, ServerSubLevel subLevel
    ) {
        BlockState state = kbe.getBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return null;
        }

        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return null;
        }

        Block block = state.getBlock();
        Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
        Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
        Vector3dc sideD = new Vector3d(sideVec.getX(), sideVec.getY(), sideVec.getZ());
        Vector3dc normalD = getTravelDirection(axis);
        Pose3d pose = subLevel.logicalPose();
        KineticBlockEntity track = BntCogwheelPairing.beltTwin(kbe);
        CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)track.getBehaviour(CogwheelChainBehaviour.TYPE);
        boolean isConnected = behaviour != null && behaviour.isPartOfChain();

        BntSuspension.Arm arm = BntSuspension.arm(kbe);
        double travel = arm == null ? 0.0 : BntSuspension.travel(kbe);
        double up = arm == null ? 0.0 : arm.upTravel(travel);
        double down = arm == null ? 0.0 : arm.downTravel(travel);
        Vec3 seat = getContactSeat(kbe, state);
        double radius = getContactRadius(block, isConnected);
        double carried = BntTrackFloor.at(kbe.getLevel(), kbe);
        BntPhysicsEvents.Reach reach = reachGround(kbe, normalD, pose, subLevel, seat, radius, arm, up, down, carried);

        boolean wasLiftedUp = mixin.bnt$isLiftedUp();
        if (!reach.grounded() || reach.rise() < -down - (arm == null ? CONTACT_GAP : RIGID_SLOP)) {
            double air = reach.grounded() ? -down - reach.rise() : NO_GROUND;
            mixin.bnt$setMaxAirExtension(Math.max(mixin.bnt$getMaxAirExtension(), air));
            mixin.bnt$setLiftedUp(true);
            mixin.bnt$setExtension(down);
            return null;
        }

        mixin.bnt$setLiftedUp(false);
        double settled = arm == null ? 0.0 : Mth.clamp(reach.rise(), -down, up);
        Vec3 centre = arm == null ? seat : seat.add(arm.displacement(settled));
        Vector3d forcePoint = new Vector3d(centre.x, centre.y, centre.z);
        double inverseNormalMass = massData.getInverseNormalMass(forcePoint, OrientedBoundingBox3d.UP);
        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.0) {
            return null;
        }

        Vector3d velocity = Sable.HELPER.getVelocity(kbe.getLevel(), new Vector3d(forcePoint));
        double verticalSpeed = velocity.y;
        Vector3d localVelocity = pose.transformNormalInverse(new Vector3d(velocity));
        if (wasLiftedUp && verticalSpeed < -0.5 && mixin.bnt$getMaxAirExtension() >= BntPhysicsTuning.getLandingSoundMinFallBlocks()) {
            playLandingSound(kbe, state);
        }
        if (wasLiftedUp || mixin.bnt$getMaxAirExtension() > 0.0) {
            mixin.bnt$setMaxAirExtension(0.0);
        }

        double touchingFriction = 1.0;
        if (reach.cast().minInteractingBlock != null) {
            touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(kbe.getLevel().getBlockState(reach.cast().minInteractingBlock)));
        }
        mixin.bnt$setExtension(-settled);

        Vec3i hitNormal = reach.cast().normal.getNormal();
        Vector3d normal = new Vector3d(hitNormal.getX(), hitNormal.getY(), hitNormal.getZ());
        if (reach.cast().subLevel != null) {
            reach.cast().subLevel.logicalPose().transformNormal(normal);
        }
        pose.transformNormalInverse(normal);

        BntPhysicsEvents.WheelContact contact = new BntPhysicsEvents.WheelContact();
        contact.mixin = mixin;
        contact.subLevel = subLevel;
        contact.pose = pose;
        contact.forcePoint = forcePoint;
        contact.normal = normal;
        Vector3d groundUp = pose.transformNormalInverse(new Vector3d(0.0, 1.0, 0.0));
        contact.normalD = groundAxis(normalD, groundUp);
        contact.sideD = groundAxis(sideD, groundUp);
        contact.localVelocity = localVelocity;
        contact.surfaceSpeed = surfaceSpeed(CogwheelSizeHelper.getChainRadius(block), track.getSpeed());
        contact.touchingFriction = touchingFriction;
        contact.normalMass = 1.0 / inverseNormalMass;
        contact.hasTraction = axis != Axis.Y;
        contact.isDriven = isConnected && BntChainEngagement.isEngaged(behaviour);
        contact.isTrackModel = isConnected || block.getDescriptionId().contains("track");
        contact.brakeStrength = kbe.getLevel().getSignal(kbe.getBlockPos().above(), Direction.DOWN) / 15.0;
        contact.loaded = true;
        contact.verticalSpeed = verticalSpeed;
        contact.pos = kbe.getBlockPos();
        contact.carried = carried;
        contact.stiffness = BntTuning.STIFFNESS.scale(kbe);
        contact.damping = BntTuning.DAMPING.scale(kbe);
        if (arm == null) {
            contact.hasTraction = contact.hasTraction && reach.rise() > -RIGID_SLOP;
            contact.penetration = reach.rise();
            contact.compression = reach.rise();
        } else {
            double hold = BntBeltHold.at(kbe.getLevel(), kbe);
            double sprung = reach.rise() - hold;
            contact.arm = true;
            contact.hold = hold;
            contact.compression = sprung;
            contact.overTravel = Math.max(0.0, sprung - up);
            contact.armRise = Mth.clamp(sprung, -down, up) - arm.ride();
        }
        return contact;
    }

    /** How far a cogwheel has to rise from its seat for its contact circle to rest on the terrain. */
    private record Reach(double rise, BntPhysicsEvents.TerrainCastResult cast, boolean grounded) {
    }

    private static BntPhysicsEvents.Reach reachGround(
        KineticBlockEntity kbe, Vector3dc normalD, Pose3dc pose, SubLevel subLevel, Vec3 seat, double radius,
        @Nullable BntSuspension.Arm arm, double up, double down, double carried
    ) {
        double rise = 0.0;
        BntPhysicsEvents.TerrainCastResult cast = null;
        for (int iteration = 0; iteration < ARM_ITERATIONS; iteration++) {
            double at = Mth.clamp(rise, -down, up);
            Vec3 centre = arm == null ? seat : seat.add(arm.displacement(at));
            cast = computeMaxExtensionToTerrain(kbe, centre, radius, radius + up - at, radius + down + at + 1.0, normalD, pose, subLevel);
            if (cast.minInteractingBlock == null && cast.maxExtension >= NO_GROUND) {
                return new BntPhysicsEvents.Reach(-NO_GROUND, cast, false);
            }
            double next = at + radius - (cast.maxExtension - carried);
            if (arm == null || Math.abs(next - rise) < ARM_TOLERANCE || next > up && at >= up || next < -down && at <= -down) {
                return new BntPhysicsEvents.Reach(next, cast, true);
            }
            rise = next;
        }
        return new BntPhysicsEvents.Reach(rise, cast, true);
    }

    private static Vec3 getContactSeat(KineticBlockEntity kbe, BlockState state) {
        Vec3 centre = kbe.getBlockPos()
            .getCenter()
            .add(0.0, CogwheelSizeHelper.getVisualVerticalOffset(state.getBlock()), 0.0)
            .add(BntCogwheelPairing.seamOffset(state));
        if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
            centre = centre.add(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        }
        return centre;
    }

    private static double getContactRadius(Block block, boolean inTrack) {
        return inTrack ? CogwheelSizeHelper.getTrackRadius(block) : CogwheelSizeHelper.getDrawnRestRadius(block);
    }

    private static boolean isInTrack(KineticBlockEntity kbe) {
        CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)BntCogwheelPairing.beltTwin(kbe).getBehaviour(CogwheelChainBehaviour.TYPE);
        return behaviour != null && behaviour.isPartOfChain();
    }

    static double surfaceSpeed(double chainRadius, double kineticSpeed) {
        return chainRadius * kineticSpeed * BLOCKS_PER_SECOND_PER_RPM_RADIUS;
    }

    private static BntPhysicsEvents.WheelContact fromBelt(
        BntBeltContacts.BntBeltContact belt, KineticBlockEntityPhysicsAccess carrier
    ) {
        Pose3d pose = belt.subLevel.logicalPose();
        Vector3d groundUp = pose.transformNormalInverse(new Vector3d(0.0, 1.0, 0.0));
        Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, belt.axis).getNormal();
        BntPhysicsEvents.WheelContact contact = new BntPhysicsEvents.WheelContact();
        contact.belt = true;
        contact.mixin = carrier;
        contact.subLevel = belt.subLevel;
        contact.pose = pose;
        contact.forcePoint = belt.forcePoint;
        contact.normal = belt.normal;
        contact.normalD = groundAxis(getTravelDirection(belt.axis), groundUp);
        contact.sideD = groundAxis(new Vector3d(sideVec.getX(), sideVec.getY(), sideVec.getZ()), groundUp);
        contact.localVelocity = pose.transformNormalInverse(new Vector3d(belt.velocity));
        contact.verticalSpeed = belt.velocity.y;
        contact.penetration = belt.penetration;
        contact.support = belt.support;
        contact.normalMass = belt.normalMass;
        contact.touchingFriction = fudgeFriction(belt.friction) * BntBeltTension.gripScale(belt.tension);
        contact.surfaceSpeed = belt.surfaceSpeed;
        contact.brakeStrength = belt.brake;
        contact.hasTraction = true;
        contact.isDriven = belt.driven;
        contact.isTrackModel = true;
        contact.loaded = true;
        return contact;
    }

    /** Wheels a body's weight is shared over, kept while some of them leave the ground so the rest do not stiffen. */
    private static double wheelShare(ServerSubLevel subLevel, int wheels, double timeStep) {
        double[] held = SHARES.computeIfAbsent(subLevel, ignored -> new double[1]);
        if (wheels > 0) {
            held[0] = Math.max(wheels, held[0] * Math.exp(-timeStep / SHARE_MEMORY));
        }
        return Math.max(held[0], 1.0);
    }

    /** Springs and belt supports solved together as soft constraints; true while the springs are still moving the body. */
    private static boolean solveSupport(
        Level level, ServerSubLevel subLevel, List<BntPhysicsEvents.WheelContact> contacts,
        double wheelShare, int beltShare, double timeStep
    ) {
        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return false;
        }

        double inverseMass = massData.getInverseMass();
        Matrix3dc inverseInertia = massData.getInverseInertiaTensor();
        Vector3dc centerOfMass = massData.getCenterOfMass();
        if (!Double.isFinite(inverseMass) || inverseMass <= 0.0 || inverseInertia == null) {
            return false;
        }

        int count = contacts.size();
        double[] inverseEffectiveMass = new double[count];
        double[] softness = new double[count];
        double[] bias = new double[count];
        double[] ceiling = new double[count];
        double[] accumulated = new double[count];
        double[] resting = new double[count];
        double[] tolerance = new double[count];
        Vector3d[] levers = new Vector3d[count];
        Vector3d[] responses = new Vector3d[count];
        double gravity = DimensionPhysicsData.getGravity(level).length();

        for (int i = 0; i < count; i++) {
            BntPhysicsEvents.WheelContact contact = contacts.get(i);
            double massShare = contact.belt
                ? contact.normalMass / Math.max(beltShare, 1)
                : massData.getMass() / wheelShare;
            double gain;
            double springStrength;
            double dampingStrength;
            double compression;
            double impulseMultiplier;
            double headroomScale;
            boolean bottomed;
            if (contact.belt) {
                gain = BntPhysicsTuning.SUSPENSION_GAIN * massShare;
                springStrength = gain * BntPhysicsTuning.SPRING_SCALE * contact.support / LEVEL_FOUR_DENOMINATOR;
                dampingStrength = gain * BntPhysicsTuning.DAMPING_SCALE * contact.support * DAMPING_CALIBRATION
                    * Math.min(1.0, contact.penetration / CONTACT_REACH);
                compression = contact.penetration;
                impulseMultiplier = BntPhysicsTuning.getTrackMaxImpulseMultiplier();
                headroomScale = Math.max(1.0, contact.support);
                bottomed = false;
            } else if (contact.arm) {
                gain = BntPhysicsTuning.SUSPENSION_GAIN * massShare;
                double springRate = contact.stiffness * ARM_SPRING_RATE;
                springStrength = gain * BntPhysicsTuning.SPRING_SCALE * BntPhysicsTuning.WHEEL_SPRING * springRate / LEVEL_FOUR_DENOMINATOR;
                dampingStrength = gain * BntPhysicsTuning.DAMPING_SCALE * BntPhysicsTuning.WHEEL_DAMPING * contact.damping
                    * Math.sqrt(springRate) * DAMPING_CALIBRATION;
                compression = springStrength > 0.0
                    ? massShare * gravity / springStrength + contact.armRise + contact.overTravel
                    : 0.0;
                impulseMultiplier = BntPhysicsTuning.getTrackMaxImpulseMultiplier();
                headroomScale = Math.max(1.0, contact.stiffness);
                bottomed = contact.overTravel > 0.0;
            } else {
                gain = 0.0;
                springStrength = 0.0;
                dampingStrength = 0.0;
                compression = 0.0;
                impulseMultiplier = 0.0;
                headroomScale = 0.0;
                bottomed = false;
            }

            levers[i] = new Vector3d(contact.forcePoint).sub(centerOfMass);
            Vector3d angular = levers[i].cross(contact.normal, new Vector3d());
            responses[i] = inverseInertia.transform(angular, new Vector3d());
            inverseEffectiveMass[i] = inverseMass + angular.dot(responses[i]);
            if (!Double.isFinite(inverseEffectiveMass[i]) || inverseEffectiveMass[i] <= 0.0) {
                continue;
            }

            if (!contact.belt && (!contact.arm || contact.overTravel > RIGID_SLOP)) {
                double penetration = contact.arm ? contact.overTravel : contact.penetration;
                bias[i] = penetration < 0.0
                    ? -penetration / timeStep
                    : -Math.min(BntPhysicsTuning.getMaxSuspensionSpeed(),
                        RIGID_BIAS * Math.max(0.0, penetration - RIGID_SLOP) / timeStep);
                ceiling[i] = Double.MAX_VALUE;
                continue;
            }

            if (bottomed) {
                dampingStrength = Math.max(dampingStrength, 2.0 * Math.sqrt(springStrength / inverseEffectiveMass[i]));
            }
            double compliance = timeStep * (dampingStrength + timeStep * springStrength);
            if (compliance > 0.0) {
                softness[i] = 1.0 / compliance;
                bias[i] = -(springStrength / (dampingStrength + timeStep * springStrength)) * compression;
            }
            double bump = bottomed ? BntPhysicsTuning.getBumpStopScale() : 1.0;
            double headroom = impulseMultiplier * gain * BntPhysicsTuning.getImpulseScale() * timeStep * bump * headroomScale;
            double ramp = contact.previousPush
                + massShare * (BntPhysicsTuning.getMaxSuspensionSpeed() + Math.abs(contact.verticalSpeed));
            ceiling[i] = Math.max(0.0, Math.min(headroom, ramp));
            resting[i] = Mth.clamp(timeStep * springStrength * compression, 0.0, ceiling[i]);
            tolerance[i] = WAKE_SHARE * massShare * gravity * timeStep;
        }

        Vector3d fall = DimensionPhysicsData.getGravity(level).mul(timeStep);
        Vector3d deltaVelocity = subLevel.logicalPose().transformNormalInverse(fall);
        Vector3d deltaAngularVelocity = new Vector3d();
        Vector3d pointVelocity = new Vector3d();

        for (int iteration = 0; iteration < SUPPORT_ITERATIONS; iteration++) {
            for (int i = 0; i < count; i++) {
                if (ceiling[i] <= 0.0) {
                    continue;
                }

                BntPhysicsEvents.WheelContact contact = contacts.get(i);
                deltaAngularVelocity.cross(levers[i], pointVelocity).add(deltaVelocity).add(contact.localVelocity);
                double normalSpeed = pointVelocity.dot(contact.normal);
                double delta = -(normalSpeed + bias[i] + softness[i] * accumulated[i]) / (inverseEffectiveMass[i] + softness[i]);
                double total = Mth.clamp(accumulated[i] + delta, 0.0, ceiling[i]);
                double applied = total - accumulated[i];
                accumulated[i] = total;
                deltaVelocity.fma(applied * inverseMass, contact.normal);
                deltaAngularVelocity.fma(applied, responses[i]);
            }
        }

        boolean moving = false;
        for (int i = 0; i < count; i++) {
            contacts.get(i).push = accumulated[i];
            moving |= tolerance[i] > 0.0 && Math.abs(accumulated[i] - resting[i]) > tolerance[i];
        }
        return moving;
    }

    private static void applyContactForces(BntPhysicsEvents.WheelContact contact) {
        Vector3d impulse = new Vector3d(contact.normal).mul(contact.push);
        impulse.fma(contact.longitudinalImpulse, contact.normalD);
        impulse.fma(contact.lateralImpulse, contact.sideD);
        if (impulse.lengthSquared() <= 0.0) {
            return;
        }

        ForceTotal forceTotal = contact.mixin.bnt$getForceTotal();
        forceTotal.applyImpulseAtPoint(contact.subLevel, contact.forcePoint, impulse);
        contact.mixin.bnt$markQueuedForForceApplication();
    }

    private static void solveTraction(
        Level level, ServerSubLevel subLevel, List<BntPhysicsEvents.WheelContact> contacts, double wheelShare, double timeStep
    ) {
        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return;
        }

        double inverseMass = massData.getInverseMass();
        Matrix3dc inverseInertia = massData.getInverseInertiaTensor();
        Vector3dc centerOfMass = massData.getCenterOfMass();
        if (!Double.isFinite(inverseMass) || inverseMass <= 0.0 || inverseInertia == null) {
            return;
        }

        double loadShare = massData.getMass() / wheelShare;
        double gravity = DimensionPhysicsData.getGravity(level).length();
        double response = BntPhysicsTuning.getTractionResponse();
        double pivotScrub = BntPhysicsTuning.getPivotScrub();
        boolean anyTraction = false;

        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (!contact.hasTraction) {
                continue;
            }

            anyTraction = true;
            contact.lever = new Vector3d(contact.forcePoint).sub(centerOfMass);
            double gripMultiplier = contact.isTrackModel ? BntPhysicsTuning.getTrackGripMultiplier() : BntPhysicsTuning.getCogwheelGripMultiplier();
            double grip = contact.touchingFriction * gripMultiplier;
            double targetSpeed = contact.isDriven ? contact.surfaceSpeed * (1.0 - contact.brakeStrength) : 0.0;
            double driveTraction = contact.isDriven
                ? BntPhysicsTuning.getDriveTraction() + contact.brakeStrength * BntPhysicsTuning.getBrakeTraction()
                : BntPhysicsTuning.getRollingResistance();
            double longitudinalSpeed = contact.localVelocity.dot(contact.normalD);
            contact.targetSpeed = targetSpeed;
            contact.goalLongitudinal = longitudinalSpeed + response * (targetSpeed - longitudinalSpeed);
            if (contact.belt) {
                double carried = gravity > 1.0E-6 ? contact.push / gravity : 0.0;
                contact.limitLongitudinal = driveTraction * grip * carried;
                contact.limitLateral = BntPhysicsTuning.getLateralTraction() * grip * carried;
            } else {
                double resting = GRIP_LOAD * loadShare * gravity * timeStep;
                double load = resting > 0.0 ? Mth.clamp(contact.push / resting, 0.0, 1.0) : 1.0;
                contact.limitLongitudinal = driveTraction * grip * loadShare * timeStep * load;
                contact.limitLateral = BntPhysicsTuning.getLateralTraction() * grip * loadShare * timeStep * load;
            }
        }

        if (!anyTraction) {
            return;
        }

        double commandedYawRate = pivotScrub > 0.0 ? solveCommandedYawRate(contacts) : 0.0;

        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (contact.hasTraction) {
                contact.yawRate = commandedYawRate;
                double lateralSpeed = contact.localVelocity.dot(contact.sideD);
                double scrubSpeed = commandedYawRate
                    * pivotScrub
                    * (contact.lever.z() * contact.sideD.x() - contact.lever.x() * contact.sideD.z());
                contact.goalLateral = lateralSpeed - response * (lateralSpeed - scrubSpeed);
            }
        }

        Vector3d deltaVelocity = new Vector3d();
        Vector3d deltaAngularVelocity = new Vector3d();
        Vector3d pointVelocity = new Vector3d();
        Vector3d angularAxis = new Vector3d();
        Vector3d transformed = new Vector3d();

        for (int iteration = 0; iteration < TRACTION_ITERATIONS; iteration++) {
            for (BntPhysicsEvents.WheelContact contact : contacts) {
                if (contact.hasTraction) {
                    contact.longitudinalImpulse = solveTractionAxis(
                        contact,
                        contact.normalD,
                        contact.goalLongitudinal,
                        contact.limitLongitudinal,
                        contact.longitudinalImpulse,
                        inverseMass,
                        inverseInertia,
                        deltaVelocity,
                        deltaAngularVelocity,
                        pointVelocity,
                        angularAxis,
                        transformed
                    );
                    contact.lateralImpulse = solveTractionAxis(
                        contact,
                        contact.sideD,
                        contact.goalLateral,
                        contact.limitLateral,
                        contact.lateralImpulse,
                        inverseMass,
                        inverseInertia,
                        deltaVelocity,
                        deltaAngularVelocity,
                        pointVelocity,
                        angularAxis,
                        transformed
                    );
                }
            }
        }
    }

    private static double solveCommandedYawRate(List<BntPhysicsEvents.WheelContact> contacts) {
        double m00 = 0.0;
        double m01 = 0.0;
        double m02 = 0.0;
        double m11 = 0.0;
        double m12 = 0.0;
        double m22 = 0.0;
        double r0 = 0.0;
        double r1 = 0.0;
        double r2 = 0.0;
        int driven = 0;
        boolean left = false;
        boolean right = false;

        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (!contact.hasTraction || !contact.isDriven || contact.belt && contact.push <= 0.0) {
                continue;
            }

            driven++;
            double side = contact.lever.dot(contact.sideD);
            left |= side < 0.0;
            right |= side > 0.0;
            double a0 = contact.normalD.x();
            double a1 = contact.normalD.z();
            double a2 = contact.lever.z() * contact.normalD.x() - contact.lever.x() * contact.normalD.z();
            double b = contact.targetSpeed;
            m00 += a0 * a0;
            m01 += a0 * a1;
            m02 += a0 * a2;
            m11 += a1 * a1;
            m12 += a1 * a2;
            m22 += a2 * a2;
            r0 += a0 * b;
            r1 += a1 * b;
            r2 += a2 * b;
        }

        if (driven < 2 || !left || !right) {
            return 0.0;
        }

        double ridge = 1.0E-6 * (m00 + m11 + m22) + 1.0E-12;
        m00 += ridge;
        m11 += ridge;
        m22 += ridge;
        double cofactor02 = m01 * m12 - m02 * m11;
        double cofactor12 = m01 * m02 - m00 * m12;
        double cofactor22 = m00 * m11 - m01 * m01;
        double determinant = m00 * (m11 * m22 - m12 * m12) + m01 * (m02 * m12 - m01 * m22) + m02 * cofactor02;
        if (!Double.isFinite(determinant) || Math.abs(determinant) < 1.0E-12) {
            return 0.0;
        }

        double yawRate = (cofactor02 * r0 + cofactor12 * r1 + cofactor22 * r2) / determinant;
        return Double.isFinite(yawRate) ? Mth.clamp(yawRate, -MAX_COMMANDED_YAW_RATE, MAX_COMMANDED_YAW_RATE) : 0.0;
    }

    private static double solveTractionAxis(
        BntPhysicsEvents.WheelContact contact,
        Vector3dc axis,
        double goal,
        double limit,
        double accumulated,
        double inverseMass,
        Matrix3dc inverseInertia,
        Vector3d deltaVelocity,
        Vector3d deltaAngularVelocity,
        Vector3d pointVelocity,
        Vector3d angularAxis,
        Vector3d transformed
    ) {
        deltaAngularVelocity.cross(contact.lever, pointVelocity).add(deltaVelocity).add(contact.localVelocity);
        double error = goal - pointVelocity.dot(axis);
        contact.lever.cross(axis, angularAxis);
        inverseInertia.transform(angularAxis, transformed);
        double inverseEffectiveMass = inverseMass + angularAxis.dot(transformed);
        if (!Double.isFinite(inverseEffectiveMass) || inverseEffectiveMass <= 0.0) {
            return accumulated;
        }

        double total = Mth.clamp(accumulated + error / inverseEffectiveMass, -limit, limit);
        double applied = total - accumulated;
        deltaVelocity.fma(applied * inverseMass, axis);
        deltaAngularVelocity.fma(applied, transformed);
        return total;
    }

    private static void report(ServerLevel level, ServerSubLevel subLevel, int wheels,
                               List<BntPhysicsEvents.WheelContact> nearGround, List<BntPhysicsEvents.WheelContact> solved) {
        long now = level.getGameTime();
        if (!BntDebugLog.enabled() || now % 20L != 0L || nearGround.isEmpty() && solved.isEmpty()
            || Long.valueOf(now).equals(REPORTED.get(subLevel))) {
            return;
        }
        REPORTED.put(subLevel, now);

        StringBuilder line = new StringBuilder();
        double yaw = 0.0;
        for (BntPhysicsEvents.WheelContact contact : nearGround) {
            yaw = contact.yawRate;
            line.append(String.format(" [%s %s %s comp%.2f hold%.2f floor%.2f push%.2f vy%.2f %s tgt%.2f lon%.2f lat%.2f]",
                contact.pos.toShortString(), contact.loaded ? "on" : "off", contact.arm ? "arm" : "rigid", contact.compression,
                contact.hold, contact.carried, contact.push, contact.verticalSpeed, contact.isDriven ? "drive" : "free",
                contact.targetSpeed, contact.longitudinalImpulse, contact.lateralImpulse));
        }

        int beltContacts = 0;
        double beltPush = 0.0;
        double beltLongitudinal = 0.0;
        double beltLateral = 0.0;
        double deepest = 0.0;
        for (BntPhysicsEvents.WheelContact contact : solved) {
            if (contact.belt) {
                beltContacts++;
                beltPush += contact.push;
                beltLongitudinal += contact.longitudinalImpulse;
                beltLateral += contact.lateralImpulse;
                deepest = Math.max(deepest, contact.penetration);
            }
        }
        Vector3d first = solved.isEmpty() ? nearGround.get(0).forcePoint : solved.get(0).forcePoint;
        Vec3 at = subLevel.logicalPose().transformPosition(new Vec3(first.x, first.y, first.z));
        BntDebugLog.LOG.info("vehicle at {} wheels {} near ground {} belt contacts {} belt push {} belt lon {} belt lat {} belt depth {} yaw {}{}",
            String.format("%.1f %.1f %.1f", at.x, at.y, at.z), wheels, nearGround.size(), beltContacts,
            String.format("%.2f", beltPush), String.format("%.2f", beltLongitudinal), String.format("%.2f", beltLateral),
            String.format("%.3f", deepest), String.format("%.2f", yaw), line);
    }

    private static void applyAllBatchedForces(ServerLevel level) {
        for (KineticBlockEntity kbe : BntPhysicsRegistry.getEnabled(level)) {
            KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
            if (mixin.bnt$consumeQueuedForForceApplication()) {
                ForceTotal forceTotal = mixin.bnt$getForceTotal();
                RigidBodyHandle handle = null;
                if (!kbe.isRemoved() && Sable.HELPER.getContaining(kbe) instanceof ServerSubLevel subLevel && !subLevel.isRemoved()) {
                    handle = RigidBodyHandle.of(subLevel);
                }

                if (handle != null && handle.isValid()) {
                    handle.applyForcesAndReset(forceTotal);
                } else {
                    forceTotal.reset();
                }
            }
        }
    }

    private static void playLandingSound(KineticBlockEntity kbe, BlockState state) {
        Level level = kbe.getLevel();
        if (level instanceof ServerLevel && BntPhysicsTuning.isLandingSoundsEnabled()) {
            BlockPos soundPos = kbe.getBlockPos();
            boolean isIndustrial = isIndustrialCog(kbe, state);
            SoundEvent hitSound;
            float volume;
            float pitch;
            if (isIndustrial) {
                Block industrialIronBlock = (Block)BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "industrial_iron_block"));
                if (industrialIronBlock != Blocks.AIR) {
                    hitSound = industrialIronBlock.getSoundType(industrialIronBlock.defaultBlockState(), level, soundPos, null).getBreakSound();
                } else {
                    hitSound = SoundEvents.NETHERITE_BLOCK_BREAK;
                }

                volume = 0.9F;
                pitch = 0.95F;
            } else {
                hitSound = Blocks.STRIPPED_OAK_WOOD.getSoundType(Blocks.STRIPPED_OAK_WOOD.defaultBlockState(), level, soundPos, null).getBreakSound();
                volume = 0.55F;
                pitch = 1.15F;
            }

            level.playSound(null, soundPos, hitSound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private static boolean isIndustrialCog(KineticBlockEntity kbe, BlockState state) {
        if (state.getBlock().getDescriptionId().contains("industrial")) {
            return true;
        } else if (!(kbe instanceof KineticBlockEntityPhysicsAccess access)) {
            return false;
        } else {
            String originalBlock = access.bnt$getOriginalBlock();
            return originalBlock != null && originalBlock.contains("industrial");
        }
    }

    private static double computeMaxExtensionVisual(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, SubLevel subLevel) {
        return computeRenderExtensionForPose(kbe, subLevel.logicalPose(), subLevel);
    }

    private static double computeRenderExtensionForPose(
        KineticBlockEntity kbe, Pose3dc pose, SubLevel subLevel
    ) {
        BlockState state = kbe.getBlockState();
        BntSuspension.Arm arm = BntSuspension.arm(kbe);
        if (!state.hasProperty(BlockStateProperties.AXIS) || arm == null) {
            return 0.0;
        }
        double travel = BntSuspension.travel(kbe);
        double up = arm.upTravel(travel);
        double down = arm.downTravel(travel);
        Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
        BntPhysicsEvents.Reach reach = reachGround(kbe, getTravelDirection(axis), pose, subLevel, getContactSeat(kbe, state),
            getContactRadius(state.getBlock(), isInTrack(kbe)), arm, up, down, 0.0);
        return reach.grounded() ? -Mth.clamp(reach.rise(), -down, up) : down;
    }

    private static BntPhysicsEvents.TerrainCastResult computeMaxExtensionToTerrain(
        KineticBlockEntity kbe, Vec3 centre, double radius, double above, double below,
        Vector3dc normalD, Pose3dc pose, SubLevel containingSubLevel
    ) {
        Vec3 sampleAxis = JOMLConversion.toMojang(normalD).normalize();
        long now = kbe.getLevel().getGameTime();
        double[] castKey = castKey(pose, centre, sampleAxis, radius, above, below);
        if (kbe instanceof KineticBlockEntityPhysicsAccess cached
            && cached.bnt$getTerrainCast() instanceof BntPhysicsEvents.TerrainCast cast
            && cast.matches(now, containingSubLevel, castKey)) {
            return cast.result;
        }

        double maxCastHeight = above + CAST_HEADROOM;
        double minExtension = NO_GROUND;
        Direction minNormal = Direction.UP;
        SubLevel minHitSubLevel = null;
        BlockPos minInteractingBlock = null;
        boolean blocked = false;

        for (double sampleOffset : getTerrainSampleOffsets(radius)) {
            Vec3 localPosO = centre.add(sampleAxis.scale(sampleOffset));
            Vec3 localRayStart = localPosO.add(0.0, maxCastHeight, 0.0);
            Vec3 localRayEnd = localPosO.subtract(0.0, NO_GROUND, 0.0);
            Vec3 globalRayStart = pose.transformPosition(localRayStart);
            Vec3 globalRayEnd = pose.transformPosition(localRayEnd);
            List<SubLevel> ignoredSubLevels = new ArrayList<>();

            for (int attempts = 0; attempts < 8; attempts++) {
                ClipContext clipContext = new ClipContext(
                    globalRayStart, globalRayEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
                );
                ((ClipContextExtension)clipContext)
                    .sable$setSubLevelIgnoring(subLevel -> subLevel == containingSubLevel || ignoredSubLevels.contains(subLevel));
                BlockHitResult clipResult = kbe.getLevel().clip(clipContext);
                if (clipResult.getType() == Type.MISS) {
                    break;
                }

                SubLevel hitSubLevel = Sable.HELPER.getContaining(kbe.getLevel(), clipResult.getLocation());
                if (clipResult.isInside()) {
                    if (hitSubLevel != null && hitSubLevel != containingSubLevel && !ignoredSubLevels.contains(hitSubLevel)) {
                        ignoredSubLevels.add(hitSubLevel);
                        continue;
                    }

                    blocked = true;
                    break;
                }

                Vec3 localHitPos = pose.transformPositionInverse(
                    hitSubLevel == null ? clipResult.getLocation() : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation())
                );
                if (localHitPos.y > centre.y + above) {
                    if (hitSubLevel == null || ignoredSubLevels.contains(hitSubLevel)) {
                        break;
                    }

                    ignoredSubLevels.add(hitSubLevel);
                } else if (hitSubLevel != null && hitSubLevel != containingSubLevel && localHitPos.y > centre.y - radius * 0.25) {
                    if (ignoredSubLevels.contains(hitSubLevel)) {
                        break;
                    }

                    ignoredSubLevels.add(hitSubLevel);
                } else {
                    if (localHitPos.y < centre.y - below) {
                        break;
                    }

                    Direction dir = clipResult.getDirection();
                    Vector3d hitNormal = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                    if (hitSubLevel != null) {
                        hitSubLevel.logicalPose().transformNormal(hitNormal);
                    }

                    if (!(hitNormal.dot(0.0, 1.0, 0.0) < 0.5)) {
                        double bulge = radius - Math.sqrt(Math.max(0.0, radius * radius - sampleOffset * sampleOffset));
                        double dist = centre.y - localHitPos.y + bulge;
                        if (dist < minExtension) {
                            minExtension = dist;
                            minNormal = clipResult.getDirection();
                            minHitSubLevel = hitSubLevel;
                            minInteractingBlock = clipResult.getBlockPos();
                        }
                        break;
                    }

                    if (hitSubLevel == null || ignoredSubLevels.contains(hitSubLevel)) {
                        break;
                    }

                    ignoredSubLevels.add(hitSubLevel);
                }
            }
        }

        if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
            if (minInteractingBlock != null) {
                access.bnt$setLastTerrainExtension(minExtension);
            } else if (blocked && Double.isFinite(access.bnt$getLastTerrainExtension())) {
                minExtension = access.bnt$getLastTerrainExtension();
            }
        }

        BntPhysicsEvents.TerrainCastResult result =
            new BntPhysicsEvents.TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
        if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setTerrainCast(new BntPhysicsEvents.TerrainCast(now, containingSubLevel, castKey, result));
        }
        return result;
    }

    /** The rays a cast fires all lie in the plane through the wheel along its travel, so three points fix them. */
    private static double[] castKey(Pose3dc pose, Vec3 centre, Vec3 sampleAxis, double radius, double above, double below) {
        Vec3 origin = pose.transformPosition(centre);
        Vec3 along = pose.transformPosition(centre.add(sampleAxis));
        Vec3 up = pose.transformPosition(centre.add(0.0, 1.0, 0.0));
        return new double[]{
            centre.x, centre.y, centre.z, origin.x, origin.y, origin.z, along.x, along.y, along.z, up.x, up.y, up.z,
            radius, above, below
        };
    }

    private static double[] getTerrainSampleOffsets(double wheelRadius) {
        double outer = wheelRadius * 2.0 / 3.0;
        double inner = wheelRadius / 3.0;
        return new double[]{-wheelRadius, -outer, -inner, 0.0, inner, outer, wheelRadius};
    }

    private static Vec3 getWheelCenter(KineticBlockEntity kbe, BlockState state) {
        Vec3 center = kbe.getBlockPos()
            .getCenter()
            .add(0.0, CogwheelSizeHelper.getVerticalOffset(state.getBlock()), 0.0)
            .add(BntCogwheelPairing.seamOffset(state));
        if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
            center = center.add(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        }

        return center;
    }

    private static Vector3dc groundAxis(Vector3dc axis, Vector3dc up) {
        Vector3d flattened = new Vector3d(axis).fma(-axis.dot(up), up);
        double length = flattened.length();
        return length < 1.0E-4 ? axis : flattened.div(length);
    }

    private static Vector3dc getTravelDirection(Axis axis) {
        if (axis == Axis.Y) {
            return new Vector3d(1.0, 0.0, 0.0);
        } else {
            Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
            return new Vector3d(-sideVec.getZ(), 0.0, sideVec.getX());
        }
    }

    private static double fudgeFriction(double realValue) {
        return realValue < 1.0 ? 0.1 + 0.9 * realValue : realValue;
    }

    private static class WheelContact {
        KineticBlockEntityPhysicsAccess mixin;
        ServerSubLevel subLevel;
        Pose3d pose;
        Vector3d forcePoint;
        Vector3d normal;
        Vector3d lever;
        Vector3dc normalD;
        Vector3dc sideD;
        Vector3d localVelocity;
        double compression;
        double overTravel;
        double armRise;
        double surfaceSpeed;
        double targetSpeed;
        double penetration;
        double support;
        double previousPush;
        double touchingFriction;
        double normalMass;
        double verticalSpeed;
        double brakeStrength;
        double stiffness;
        double damping;
        BlockPos pos;
        double hold;
        double carried;
        double push;
        double yawRate;
        double goalLongitudinal;
        double goalLateral;
        double limitLongitudinal;
        double limitLateral;
        double longitudinalImpulse;
        double lateralImpulse;
        boolean hasTraction;
        boolean isDriven;
        boolean isTrackModel;
        boolean loaded;
        boolean belt;
        boolean arm;
    }

    private static class TerrainCastResult {
        final double maxExtension;
        final Direction normal;
        @Nullable
        final SubLevel subLevel;
        @Nullable
        final BlockPos minInteractingBlock;

        TerrainCastResult(double maxExtension, Direction normal, @Nullable SubLevel subLevel, @Nullable BlockPos minInteractingBlock) {
            this.maxExtension = maxExtension;
            this.normal = normal;
            this.subLevel = subLevel;
            this.minInteractingBlock = minInteractingBlock;
        }
    }

    /** Last cast a wheel fired, reused while the tick and the rays are the same. */
    private record TerrainCast(long tick, SubLevel from, double[] key, BntPhysicsEvents.TerrainCastResult result) {
        boolean matches(long now, SubLevel containing, double[] castKey) {
            return this.tick == now && this.from == containing && Arrays.equals(this.key, castKey);
        }
    }
}
