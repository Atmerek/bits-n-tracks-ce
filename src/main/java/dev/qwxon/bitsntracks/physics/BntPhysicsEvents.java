package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
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
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import org.joml.Quaterniondc;
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
    private static final double GRIP_MEMORY = 0.1;
    private static final double SLIP_GRIP = 0.25;
    private static final double SLIP_SLIDE = 1.0;
    private static final long TREAD_SWEEP = 200L;
    private static final double CLIMB_CREEP = 0.25;
    private static final Map<ServerSubLevel, Long> REPORTED = new WeakHashMap<>();
    private static final Map<ServerSubLevel, double[]> SHARES = new WeakHashMap<>();
    private static final Map<ServerSubLevel, Parked> PARKED = new WeakHashMap<>();
    private static final Map<KineticBlockEntity, double[]> HELD = new WeakHashMap<>();
    private static final Map<KineticBlockEntity, double[]> CLIMB = new WeakHashMap<>();
    private static final Map<ServerLevel, long[]> STEPS = new WeakHashMap<>();
    private static final Map<Level, Map<BlockPos, double[]>> TREAD_LOADS = new WeakHashMap<>();
    private static final long PARKED_REFRESH = 20L;
    private static final double PARKED_DRIFT = 1.0E-4;
    private static final double PARKED_TURN = 1.0E-5;

    private BntPhysicsEvents() {
    }

    public static void register() {
        SableEventPlatform.INSTANCE.onPhysicsTick(BntPhysicsEvents::onPhysicsTick);
    }

    public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();
        long step = ++STEPS.computeIfAbsent(level, ignored -> new long[1])[0];
        Map<BlockPos, double[]> treadLoads = TREAD_LOADS.get(level);
        if (step % TREAD_SWEEP == 0L && treadLoads != null) {
            treadLoads.values().removeIf(last -> (long)last[0] < step - 1L);
        }
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
                }
            }
        }

        for (List<ServerSubLevel> assembly : BntSubLevelLinks.assemblies(level, physicsSystem.getPipeline(), wheelsByBody.keySet())) {
            if (parked(level, assembly, wheelsByBody, timeStep)) {
                for (ServerSubLevel body : assembly) {
                    for (KineticBlockEntity wheel : wheelsByBody.getOrDefault(body, List.of())) {
                        reapply(wheel);
                    }
                }
                continue;
            }

            ServerSubLevel frame = assembly.size() == 1 ? assembly.get(0) : BntAssemblyMass.heaviest(assembly);
            MassData massData = frame == null ? null : BntAssemblyMass.of(frame, assembly);
            List<BntPhysicsEvents.WheelContact> contacts = new ArrayList<>();
            List<BntPhysicsEvents.WheelContact> loaded = new ArrayList<>();
            List<BodyLoads> bodies = new ArrayList<>();
            double share = 0.0;
            int beltShare = 0;

            for (ServerSubLevel subLevel : assembly) {
                List<KineticBlockEntity> wheels = wheelsByBody.get(subLevel);
                park(level, subLevel, wheels == null ? 0 : signature(level, wheels), timeStep);
                if (wheels == null) {
                    continue;
                }

                List<BntPhysicsEvents.WheelContact> nearGround = new ArrayList<>();
                for (KineticBlockEntity wheel : wheels) {
                    HELD.remove(wheel);
                    BntPhysicsEvents.WheelContact contact = resolveContact(wheel, (KineticBlockEntityPhysicsAccess)wheel, subLevel);
                    if (contact != null) {
                        if (contact.arm) {
                            contact.climbRate = climbRate(wheel, contact, step, timeStep);
                        }
                        nearGround.add(contact);
                    }
                }
                BntBeltContacts.BntBeltLoads belt = BntBeltContacts.build(subLevel, wheels, level);
                KineticBlockEntityPhysicsAccess carrier = (KineticBlockEntityPhysicsAccess)wheels.get(0);

                List<BntPhysicsEvents.WheelContact> bodyContacts = new ArrayList<>();
                for (BntPhysicsEvents.WheelContact contact : nearGround) {
                    if (contact.loaded) {
                        contact.previousPush = contact.mixin.bnt$getSpringImpulse();
                        loaded.add(contact);
                        bodyContacts.add(contact);
                    }
                }
                for (KineticBlockEntity wheel : wheels) {
                    ((KineticBlockEntityPhysicsAccess)wheel).bnt$setSpringImpulse(0.0);
                }
                for (BntBeltContacts.BntBeltContact beltContact : belt.contacts()) {
                    bodyContacts.add(fromBelt(beltContact, carrier));
                }
                for (BntPhysicsEvents.WheelContact contact : bodyContacts) {
                    joinTread(contact, belt.treads());
                }

                if (massData != null && subLevel != frame) {
                    for (BntPhysicsEvents.WheelContact contact : bodyContacts) {
                        toFrame(contact, frame.logicalPose(), massData);
                    }
                }
                contacts.addAll(bodyContacts);
                share += wheelShare(subLevel, nearGround.size(), timeStep);
                beltShare += belt.contacts().size();
                bodies.add(new BodyLoads(subLevel, wheels.size(), carrier, belt, nearGround, bodyContacts));
            }

            share = Math.max(share, 1.0);
            if (!contacts.isEmpty()) {
                if (frame != null && solveSupport(level, frame.logicalPose(), massData, contacts, share, beltShare, timeStep)) {
                    for (ServerSubLevel subLevel : assembly) {
                        physicsSystem.getPipeline().wakeUp(subLevel);
                    }
                }
                solveTraction(level, massData, contacts, timeStep, step);
                for (BntPhysicsEvents.WheelContact contact : contacts) {
                    applyContactForces(contact);
                }
                for (BntPhysicsEvents.WheelContact contact : loaded) {
                    contact.mixin.bnt$setSpringImpulse(contact.push);
                }
            }

            for (BodyLoads body : bodies) {
                for (BntBeltContacts.BntBeltWeight weight : body.belt().weights()) {
                    BntBeltContacts.assignCarrier(weight, body.carrier());
                    BntBeltContacts.applyWeight(weight, level, timeStep);
                }
                report(level, body.subLevel(), body.wheels(), body.nearGround(), body.contacts(),
                    assembly.size(), massData == null ? 0.0 : massData.getMass());
            }
        }

        applyAllBatchedForces(level);
    }

    private static double climbRate(KineticBlockEntity wheel, BntPhysicsEvents.WheelContact contact, long step, double timeStep) {
        double[] last = CLIMB.get(wheel);
        double owed = 0.0;
        if (last != null && (long)last[0] == step - 1L) {
            double climb = contact.rise - last[1] + contact.localVelocity.dot(contact.normal) * timeStep;
            owed = Math.max(0.0, last[2] + (Math.abs(climb) < CONTACT_GAP ? 0.0 : climb));
        }
        double approach = contact.localVelocity.dot(contact.normal);
        double crossing = contact.localVelocity.sub(contact.normal.x * approach, contact.normal.y * approach,
            contact.normal.z * approach, new Vector3d()).length();
        if (crossing < CLIMB_CREEP) {
            owed = 0.0;
        }
        double rate = Math.min(owed / timeStep, crossing);
        CLIMB.put(wheel, new double[]{step, contact.rise, owed - rate * timeStep});
        return rate;
    }

    private record BodyLoads(
        ServerSubLevel subLevel, int wheels, KineticBlockEntityPhysicsAccess carrier, BntBeltContacts.BntBeltLoads belt,
        List<BntPhysicsEvents.WheelContact> nearGround, List<BntPhysicsEvents.WheelContact> contacts
    ) {
    }

    private static void toFrame(BntPhysicsEvents.WheelContact contact, Pose3dc frame, MassData massData) {
        Pose3dc own = contact.pose;
        contact.framePose = frame;
        contact.ownPoint = contact.forcePoint;
        contact.forcePoint = frame.transformPositionInverse(own.transformPosition(contact.forcePoint, new Vector3d()));
        contact.normal = frame.transformNormalInverse(own.transformNormal(contact.normal, new Vector3d()));
        contact.normalD = frame.transformNormalInverse(own.transformNormal(contact.normalD, new Vector3d()));
        contact.sideD = frame.transformNormalInverse(own.transformNormal(contact.sideD, new Vector3d()));
        contact.localVelocity = frame.transformNormalInverse(own.transformNormal(contact.localVelocity, new Vector3d()));
        if (contact.belt) {
            Vector3d up = frame.transformNormalInverse(own.transformNormal(OrientedBoundingBox3d.UP, new Vector3d()));
            double inverseNormalMass = massData.getInverseNormalMass(contact.forcePoint, up);
            if (Double.isFinite(inverseNormalMass) && inverseNormalMass > 0.0) {
                contact.normalMass = 1.0 / inverseNormalMass;
            }
        }
    }

    private static boolean parked(
        ServerLevel level, List<ServerSubLevel> assembly, Map<ServerSubLevel, List<KineticBlockEntity>> wheelsByBody, double timeStep
    ) {
        for (ServerSubLevel subLevel : assembly) {
            List<KineticBlockEntity> wheels = wheelsByBody.get(subLevel);
            if (!parked(level, subLevel, wheels == null ? 0 : signature(level, wheels), timeStep)) {
                return false;
            }
        }
        return true;
    }

    private record Parked(double x, double y, double z, double qx, double qy, double qz, double qw, int signature, double timeStep, long since) {
    }

    private static int signature(ServerLevel level, List<KineticBlockEntity> wheels) {
        int hash = wheels.size();
        for (KineticBlockEntity wheel : wheels) {
            hash = 31 * hash + System.identityHashCode(wheel);
            hash = 31 * hash + Float.hashCode(wheel.getSpeed());
            hash = 31 * hash + level.getSignal(wheel.getBlockPos().above(), Direction.DOWN);
        }
        return hash;
    }

    private static boolean parked(ServerLevel level, ServerSubLevel subLevel, int signature, double timeStep) {
        Parked last = PARKED.get(subLevel);
        if (last == null || last.signature() != signature || last.timeStep() != timeStep
            || level.getGameTime() - last.since() >= PARKED_REFRESH) {
            return false;
        }
        Pose3d pose = subLevel.logicalPose();
        Vector3dc position = pose.position();
        Quaterniondc orientation = pose.orientation();
        return Math.abs(position.x() - last.x()) < PARKED_DRIFT
            && Math.abs(position.y() - last.y()) < PARKED_DRIFT
            && Math.abs(position.z() - last.z()) < PARKED_DRIFT
            && Math.abs(orientation.x() - last.qx()) < PARKED_TURN
            && Math.abs(orientation.y() - last.qy()) < PARKED_TURN
            && Math.abs(orientation.z() - last.qz()) < PARKED_TURN
            && Math.abs(orientation.w() - last.qw()) < PARKED_TURN;
    }

    private static void park(ServerLevel level, ServerSubLevel subLevel, int signature, double timeStep) {
        Pose3d pose = subLevel.logicalPose();
        Vector3dc position = pose.position();
        Quaterniondc orientation = pose.orientation();
        PARKED.put(subLevel, new Parked(position.x(), position.y(), position.z(),
            orientation.x(), orientation.y(), orientation.z(), orientation.w(), signature, timeStep, level.getGameTime()));
    }

    private static void reapply(KineticBlockEntity wheel) {
        double[] held = HELD.get(wheel);
        if (held == null) {
            return;
        }
        KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)wheel;
        mixin.bnt$getForceTotal().applyLinearAndAngularImpulse(new Vector3d(held[0], held[1], held[2]), new Vector3d(held[3], held[4], held[5]));
        mixin.bnt$markQueuedForForceApplication();
    }

    public static void updateClientRollingSpeed(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, SubLevel subLevel) {
        Level level = kbe.getLevel();
        BlockState state = kbe.getBlockState();
        if (level != null && state != null && state.hasProperty(BlockStateProperties.AXIS)) {
            Axis axis = state.getValue(BlockStateProperties.AXIS);
            Vec3 localPos = getWheelCenter(kbe, state);
            Pose3dc pose = subLevel.logicalPose();
            Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(localPos.x, localPos.y, localPos.z));
            Vector3d localVelocity = pose.transformNormalInverse(new Vector3d(velocity)).div(20.0);
            double translation = localVelocity.dot(getTravelDirection(axis));
            double wheelRadius = CogwheelSizeHelper.getChainRadius(state.getBlock());
            double angularVelocity = translation / wheelRadius;
            if (axis == Axis.Z) {
                angularVelocity = -angularVelocity;
            }

            mixin.bnt$setPhysicalSpeed((float)angularVelocity);
        }
    }

    public record GroundContact(
        @Nullable BlockPos ground, Vec3 point, Vec3 travel, Vec3 side, double rolling, double target, double vertical
    ) {
        public boolean grounded() {
            return this.ground != null;
        }
    }

    @Nullable
    public static GroundContact groundContact(KineticBlockEntity kbe, SubLevel subLevel) {
        BlockState state = kbe.getBlockState();
        Level level = kbe.getLevel();
        if (level == null || !state.hasProperty(BlockStateProperties.AXIS)) {
            return null;
        }
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        if (axis == Axis.Y) {
            return null;
        }

        Block block = state.getBlock();
        Pose3dc pose = subLevel.logicalPose();
        KineticBlockEntity track = BntCogwheelPairing.beltTwin(kbe);
        boolean inTrack = isInTrack(kbe);
        BntSuspension.Arm arm = BntSuspension.arm(kbe);
        double travel = arm == null ? 0.0 : BntSuspension.travel(kbe);
        double up = arm == null ? 0.0 : arm.upTravel(travel);
        double down = arm == null ? 0.0 : arm.downTravel(travel);
        Vec3 seat = getContactSeat(kbe, state);
        double radius = getContactRadius(block, inTrack);
        Vector3dc normalD = getTravelDirection(axis);
        BntPhysicsEvents.Reach reach = reachGround(kbe, normalD, pose, subLevel, seat, radius, arm, up, down, 0.0, false);
        boolean grounded = reach.grounded() && reach.cast().minInteractingBlock != null
            && reach.rise() >= -down - (arm == null ? CONTACT_GAP : RIGID_SLOP);

        double settled = arm == null ? 0.0 : Mth.clamp(reach.rise(), -down, up);
        Vec3 centre = arm == null ? seat : seat.add(arm.displacement(settled));
        Vec3 point = pose.transformPosition(centre.subtract(0.0, radius, 0.0));
        Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(centre.x, centre.y, centre.z));
        Vector3d localVelocity = pose.transformNormalInverse(new Vector3d(velocity));
        Vector3dc groundNormalD = groundAxis(normalD, pose.transformNormalInverse(new Vector3d(0.0, 1.0, 0.0)));
        double rolling = localVelocity.dot(groundNormalD);
        double target = rolling;
        if (inTrack && track.getSpeed() != 0.0F) {
            target = surfaceSpeed(CogwheelSizeHelper.getChainRadius(block), track.getSpeed()) * (1.0 - treadBrake(level, track));
        }

        Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
        Vector3d travelWorld = pose.transformNormal(new Vector3d(groundNormalD));
        Vector3d sideWorld = pose.transformNormal(new Vector3d(sideVec.getX(), sideVec.getY(), sideVec.getZ()));
        return new GroundContact(
            grounded ? reach.cast().minInteractingBlock : null,
            point,
            new Vec3(travelWorld.x, travelWorld.y, travelWorld.z),
            new Vec3(sideWorld.x, sideWorld.y, sideWorld.z),
            rolling,
            target,
            velocity.y
        );
    }

    private static double treadBrake(Level level, KineticBlockEntity track) {
        BlockPos controllerPos = BntChains.controllerPos(track);
        if (controllerPos == null) {
            return level.getSignal(track.getBlockPos().above(), Direction.DOWN) / 15.0;
        }
        int signal = 0;
        for (PathedCogwheelNode node : BntChains.beltOrder(level, controllerPos)) {
            BlockPos nodePos = controllerPos.offset(node.localPos());
            signal = Math.max(signal, brakeSignal(level, nodePos));
            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, nodePos);
            if (partnerPos != null) {
                signal = Math.max(signal, brakeSignal(level, partnerPos));
            }
        }
        return signal / 15.0;
    }

    private static int brakeSignal(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled()
            ? level.getSignal(pos.above(), Direction.DOWN)
            : 0;
    }

    public static double getHeldRenderExtension(KineticBlockEntity kbe, float partialTick) {
        double staged = BntPonderPhysics.wheelDrop(kbe);
        if (!Double.isNaN(staged)) {
            return staged <= 0.0
                ? staged
                : Math.max(0.0, staged - BntBeltHold.at(kbe.getLevel(), kbe) - BntTrackFloor.at(kbe.getLevel(), kbe));
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

    private static double heldDrop(KineticBlockEntity kbe, float partialTick) {
        double raw = getRawRenderExtension(kbe, partialTick);
        Level level = kbe.getLevel();
        return raw <= 0.0
            ? raw
            : Math.max(0.0, raw - BntBeltHold.at(level, kbe) - BntTrackFloor.at(level, kbe));
    }

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
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
        Vector3dc sideD = new Vector3d(sideVec.getX(), sideVec.getY(), sideVec.getZ());
        Vector3dc normalD = getTravelDirection(axis);
        Pose3d pose = subLevel.logicalPose();
        KineticBlockEntity track = BntCogwheelPairing.beltTwin(kbe);
        CogwheelChainBehaviour behaviour = track.getBehaviour(CogwheelChainBehaviour.TYPE);
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

        BntGround.Surface ground = reach.cast().minInteractingBlock == null
            ? BntGround.UNKNOWN
            : BntGround.of(kbe.getLevel().getBlockState(reach.cast().minInteractingBlock));
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
        contact.ground = ground;
        contact.patch = BntGround.patch(radius) * (BntCogwheelPairing.isWide(state) ? 0.5 : 1.0);
        contact.normalMass = 1.0 / inverseNormalMass;
        contact.tread = isConnected && BntChainEngagement.isEngaged(behaviour) ? BntChains.controllerPos(track) : null;
        contact.hasTraction = axis != Axis.Y && contact.tread != null;
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
            contact.rise = reach.rise();
            contact.hold = hold;
            contact.compression = sprung;
            contact.overTravel = Math.max(0.0, sprung - up);
            contact.armRise = Mth.clamp(sprung, -down, up) - arm.ride();
            double room = up - arm.ride();
            contact.travelUsed = room > 1.0E-6 ? Mth.clamp(contact.armRise / room, 0.0, 1.0) : 1.0;
        }
        return contact;
    }

    public static double armSpring(double stiffness) {
        return BntPhysicsTuning.SUSPENSION_GAIN * BntPhysicsTuning.SPRING_SCALE * BntPhysicsTuning.WHEEL_SPRING
            * stiffness * ARM_SPRING_RATE / LEVEL_FOUR_DENOMINATOR;
    }

    public static double armDamping(double stiffness, double damping) {
        return BntPhysicsTuning.SUSPENSION_GAIN * BntPhysicsTuning.DAMPING_SCALE * BntPhysicsTuning.WHEEL_DAMPING
            * damping * Math.sqrt(stiffness * ARM_SPRING_RATE) * DAMPING_CALIBRATION;
    }

    private record Reach(double rise, BntPhysicsEvents.TerrainCastResult cast, boolean grounded) {
    }

    private static BntPhysicsEvents.Reach reachGround(
        KineticBlockEntity kbe, Vector3dc normalD, Pose3dc pose, SubLevel subLevel, Vec3 seat, double radius,
        @Nullable BntSuspension.Arm arm, double up, double down, double carried
    ) {
        return reachGround(kbe, normalD, pose, subLevel, seat, radius, arm, up, down, carried, true);
    }

    private static BntPhysicsEvents.Reach reachGround(
        KineticBlockEntity kbe, Vector3dc normalD, Pose3dc pose, SubLevel subLevel, Vec3 seat, double radius,
        @Nullable BntSuspension.Arm arm, double up, double down, double carried, boolean shared
    ) {
        double rise = 0.0;
        BntPhysicsEvents.TerrainCastResult cast = null;
        for (int iteration = 0; iteration < ARM_ITERATIONS; iteration++) {
            double at = Mth.clamp(rise, -down, up);
            Vec3 centre = arm == null ? seat : seat.add(arm.displacement(at));
            cast = computeMaxExtensionToTerrain(kbe, centre, radius, radius + up - at, radius + down + at + 1.0, normalD, pose, subLevel, shared);
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
            .add(BntCogwheelPairing.seamOffset(state));
        if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
            centre = centre.add(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        }
        return centre;
    }

    private static double getContactRadius(Block block, boolean inTrack) {
        return inTrack ? CogwheelSizeHelper.getTrackRestRadius(block) : CogwheelSizeHelper.getDrawnRestRadius(block);
    }

    private static boolean isInTrack(KineticBlockEntity kbe) {
        CogwheelChainBehaviour behaviour = BntCogwheelPairing.beltTwin(kbe).getBehaviour(CogwheelChainBehaviour.TYPE);
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
        contact.ground = belt.ground;
        contact.patch = belt.length;
        contact.tread = belt.tread;
        contact.hasTraction = true;
        contact.loaded = true;
        return contact;
    }

    private static void joinTread(BntPhysicsEvents.WheelContact contact, Map<BlockPos, BntBeltContacts.BntTread> treads) {
        BntBeltContacts.BntTread tread = contact.tread == null ? null : treads.get(contact.tread);
        if (tread == null) {
            contact.hasTraction = false;
            return;
        }
        contact.isDriven = tread.driven();
        contact.surfaceSpeed = tread.surfaceSpeed();
        contact.brakeStrength = tread.brake();
        contact.touchingFriction = BntGround.grip(contact.ground, tread.type(), tread.chained());
        contact.width = tread.width();
    }

    private static double treadLoad(Level level, BlockPos tread, double pressed, double timeStep, long step) {
        Map<BlockPos, double[]> loads = TREAD_LOADS.computeIfAbsent(level, ignored -> new HashMap<>());
        double force = pressed / timeStep;
        double[] last = loads.get(tread);
        if (last != null && (long)last[0] == step - 1L) {
            force = last[1] + (force - last[1]) * (1.0 - Math.exp(-timeStep / GRIP_MEMORY));
        }
        loads.put(tread, new double[]{step, force});
        return force * timeStep;
    }

    private static double wheelShare(ServerSubLevel subLevel, int wheels, double timeStep) {
        double[] held = SHARES.computeIfAbsent(subLevel, ignored -> new double[1]);
        if (wheels > 0) {
            held[0] = Math.max(wheels, held[0] * Math.exp(-timeStep / SHARE_MEMORY));
        }
        return held[0];
    }

    private static boolean solveSupport(
        Level level, Pose3dc framePose, @Nullable MassData massData, List<BntPhysicsEvents.WheelContact> contacts,
        double wheelShare, int beltShare, double timeStep
    ) {
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return false;
        }

        double inverseMass = massData.getInverseMass();
        Matrix3dc inverseInertia = massData.getInverseInertiaTensor();
        Vector3dc centerOfMass = massData.getCenterOfMass();
        if (!Double.isFinite(inverseMass) || inverseMass <= 0.0 || inverseInertia == null) {
            return false;
        }

        double gravity = DimensionPhysicsData.getGravity(level).length();
        Vector3d fall = DimensionPhysicsData.getGravity(level).mul(timeStep);
        Vector3d deltaVelocity = framePose.transformNormalInverse(fall);
        return solveSupport(contacts, massData.getMass(), inverseMass, inverseInertia, centerOfMass, gravity, deltaVelocity,
            wheelShare, beltShare, timeStep);
    }

    public record SupportContact(
        Vector3dc point, double speed, boolean arm, double rise, double up, double down, double ride,
        double stiffness, double damping, double previousPush
    ) {
    }

    public static double[] solveSupport(
        List<SupportContact> supports, double mass, Matrix3dc inverseInertia, Vector3dc centerOfMass, double gravity, double timeStep
    ) {
        List<BntPhysicsEvents.WheelContact> contacts = new ArrayList<>(supports.size());
        for (SupportContact support : supports) {
            BntPhysicsEvents.WheelContact contact = new BntPhysicsEvents.WheelContact();
            contact.forcePoint = new Vector3d(support.point());
            contact.normal = new Vector3d(0.0, 1.0, 0.0);
            contact.localVelocity = new Vector3d(0.0, support.speed(), 0.0);
            contact.verticalSpeed = support.speed();
            contact.previousPush = support.previousPush();
            contact.stiffness = support.stiffness();
            contact.damping = support.damping();
            contact.arm = support.arm();
            contact.loaded = true;
            if (support.arm()) {
                contact.compression = support.rise();
                contact.overTravel = Math.max(0.0, support.rise() - support.up());
                contact.armRise = Mth.clamp(support.rise(), -support.down(), support.up()) - support.ride();
            } else {
                contact.penetration = support.rise();
                contact.compression = support.rise();
            }
            contacts.add(contact);
        }
        solveSupport(contacts, mass, 1.0 / mass, inverseInertia, centerOfMass, gravity,
            new Vector3d(0.0, -gravity * timeStep, 0.0), contacts.size(), 0, timeStep);
        double[] pushes = new double[contacts.size()];
        for (int i = 0; i < pushes.length; i++) {
            pushes[i] = contacts.get(i).push;
        }
        return pushes;
    }

    private static boolean solveSupport(
        List<BntPhysicsEvents.WheelContact> contacts, double mass, double inverseMass, Matrix3dc inverseInertia,
        Vector3dc centerOfMass, double gravity, Vector3d deltaVelocity, double wheelShare, int beltShare, double timeStep
    ) {
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

        for (int i = 0; i < count; i++) {
            BntPhysicsEvents.WheelContact contact = contacts.get(i);
            double massShare = contact.belt
                ? contact.normalMass / Math.max(beltShare, 1)
                : mass / wheelShare;
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
                springStrength = massShare * armSpring(contact.stiffness);
                dampingStrength = massShare * armDamping(contact.stiffness, contact.damping);
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
                bias[i] = -(springStrength * compression + dampingStrength * contact.climbRate * contact.travelUsed)
                    / (dampingStrength + timeStep * springStrength);
            }
            double bump = bottomed ? BntPhysicsTuning.getBumpStopScale() : 1.0;
            double headroom = impulseMultiplier * gain * BntPhysicsTuning.getImpulseScale() * timeStep * bump * headroomScale;
            double ramp = contact.previousPush
                + massShare * (BntPhysicsTuning.getMaxSuspensionSpeed() + Math.abs(contact.verticalSpeed));
            ceiling[i] = Math.max(0.0, Math.min(headroom, ramp));
            resting[i] = Mth.clamp(timeStep * springStrength * compression, 0.0, ceiling[i]);
            tolerance[i] = WAKE_SHARE * massShare * gravity * timeStep;
        }

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

        if (contact.framePose != null) {
            contact.pose.transformNormalInverse(contact.framePose.transformNormal(impulse));
        }

        ForceTotal forceTotal = contact.mixin.bnt$getForceTotal();
        forceTotal.applyImpulseAtPoint(contact.subLevel, contact.ownPoint(), impulse);
        contact.mixin.bnt$markQueuedForForceApplication();
    }

    private static void solveTraction(
        Level level, @Nullable MassData massData, List<BntPhysicsEvents.WheelContact> contacts, double timeStep, long step
    ) {
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return;
        }

        double inverseMass = massData.getInverseMass();
        Matrix3dc inverseInertia = massData.getInverseInertiaTensor();
        Vector3dc centerOfMass = massData.getCenterOfMass();
        if (!Double.isFinite(inverseMass) || inverseMass <= 0.0 || inverseInertia == null) {
            return;
        }

        Map<BlockPos, double[]> treads = new HashMap<>();
        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (contact.hasTraction) {
                double[] tread = treads.computeIfAbsent(contact.tread, ignored -> new double[6]);
                tread[0] += Math.max(0.0, contact.push);
                tread[1]++;
                tread[3] += contact.patch * contact.width;
            }
        }
        if (treads.isEmpty()) {
            return;
        }
        double gravity = DimensionPhysicsData.getGravity(level).length();
        for (Map.Entry<BlockPos, double[]> entry : treads.entrySet()) {
            double[] tread = entry.getValue();
            tread[2] = treadLoad(level, entry.getKey(), tread[0], timeStep, step);
            tread[3] = tread[3] > 1.0E-9 && gravity > 1.0E-6 ? tread[2] / (gravity * timeStep * tread[3]) : 0.0;
        }

        double response = BntPhysicsTuning.getTractionResponse();
        double pivotScrub = BntPhysicsTuning.getPivotScrub();
        double trackGrip = BntPhysicsTuning.getTrackGrip();
        double rollingResistance = BntPhysicsTuning.getRollingResistance();

        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (!contact.hasTraction) {
                continue;
            }

            double[] tread = treads.get(contact.tread);
            double share = tread[0] > 1.0E-9 ? Math.max(0.0, contact.push) / tread[0] : 1.0 / tread[1];
            double load = tread[2] * share;
            contact.lever = new Vector3d(contact.forcePoint).sub(centerOfMass);
            double targetSpeed = contact.isDriven ? contact.surfaceSpeed * (1.0 - contact.brakeStrength) : 0.0;
            double longitudinalSpeed = contact.localVelocity.dot(contact.normalD);
            contact.targetSpeed = targetSpeed;
            contact.goalLongitudinal = longitudinalSpeed + response * (targetSpeed - longitudinalSpeed);
            contact.pressure = tread[3];
            double spread = contact.ground.soft() ? BntGround.spread(contact.pressure) : 1.0;
            contact.grip = trackGrip * contact.touchingFriction * spread * load;
            contact.longitudinalCap = contact.isDriven
                ? contact.grip
                : Mth.lerp(contact.brakeStrength, Math.min(rollingResistance * load, contact.grip), contact.grip);

            Vector3d angular = contact.lever.cross(contact.normalD, new Vector3d());
            contact.longitudinalResponse = inverseInertia.transform(angular, new Vector3d());
            contact.longitudinalInverseMass = inverseMass + angular.dot(contact.longitudinalResponse);
            angular = contact.lever.cross(contact.sideD, new Vector3d());
            contact.lateralResponse = inverseInertia.transform(angular, new Vector3d());
            contact.lateralInverseMass = inverseMass + angular.dot(contact.lateralResponse);
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

                double[] tread = treads.get(contact.tread);
                double weight = tread[0] > 1.0E-9 ? Math.max(0.0, contact.push) : 1.0;
                tread[4] += weight * Math.abs(lateralSpeed - scrubSpeed);
                tread[5] += weight;
            }
        }

        double slidingGrip = BntPhysicsTuning.getSlidingGrip();
        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (contact.hasTraction) {
                double[] tread = treads.get(contact.tread);
                double slide = Mth.smoothstep(Mth.clamp(
                    ((tread[5] > 0.0 ? tread[4] / tread[5] : 0.0) - SLIP_GRIP) / (SLIP_SLIDE - SLIP_GRIP), 0.0, 1.0));
                contact.slide = slide;
                contact.grip *= Mth.lerp(slide, 1.0, slidingGrip);
                contact.longitudinalCap = Math.min(contact.longitudinalCap, contact.grip);
            }
        }

        Vector3d deltaVelocity = new Vector3d();
        Vector3d deltaAngularVelocity = new Vector3d();
        Vector3d pointVelocity = new Vector3d();

        for (int iteration = 0; iteration < TRACTION_ITERATIONS; iteration++) {
            for (BntPhysicsEvents.WheelContact contact : contacts) {
                if (contact.hasTraction) {
                    solveGrip(contact, inverseMass, deltaVelocity, deltaAngularVelocity, pointVelocity);
                }
            }
        }
    }

    private static void solveGrip(
        BntPhysicsEvents.WheelContact contact, double inverseMass,
        Vector3d deltaVelocity, Vector3d deltaAngularVelocity, Vector3d pointVelocity
    ) {
        if (!(contact.longitudinalInverseMass > 0.0) || !(contact.lateralInverseMass > 0.0)
            || !Double.isFinite(contact.longitudinalInverseMass) || !Double.isFinite(contact.lateralInverseMass)) {
            return;
        }

        deltaAngularVelocity.cross(contact.lever, pointVelocity).add(deltaVelocity).add(contact.localVelocity);
        double longitudinal = contact.longitudinalImpulse
            + (contact.goalLongitudinal - pointVelocity.dot(contact.normalD)) / contact.longitudinalInverseMass;
        double lateral = contact.lateralImpulse
            + (contact.goalLateral - pointVelocity.dot(contact.sideD)) / contact.lateralInverseMass;
        double magnitude = Math.hypot(longitudinal, lateral);
        if (magnitude > contact.grip) {
            double scale = contact.grip / magnitude;
            longitudinal *= scale;
            lateral *= scale;
        }
        longitudinal = Mth.clamp(longitudinal, -contact.longitudinalCap, contact.longitudinalCap);

        double appliedLongitudinal = longitudinal - contact.longitudinalImpulse;
        double appliedLateral = lateral - contact.lateralImpulse;
        deltaVelocity.fma(appliedLongitudinal * inverseMass, contact.normalD).fma(appliedLateral * inverseMass, contact.sideD);
        deltaAngularVelocity.fma(appliedLongitudinal, contact.longitudinalResponse).fma(appliedLateral, contact.lateralResponse);
        contact.longitudinalImpulse = longitudinal;
        contact.lateralImpulse = lateral;
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

    private static void report(ServerLevel level, ServerSubLevel subLevel, int wheels,
                               List<BntPhysicsEvents.WheelContact> nearGround, List<BntPhysicsEvents.WheelContact> solved,
                               int bodies, double mass) {
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
            line.append(String.format(" [%s %s %s comp%.2f hold%.2f floor%.2f push%.2f vy%.2f %s tgt%.2f grip%.2f pressure%.1f slide%.2f lon%.2f lat%.2f]",
                contact.pos.toShortString(), contact.loaded ? "on" : "off", contact.arm ? "arm" : "rigid", contact.compression,
                contact.hold, contact.carried, contact.push, contact.verticalSpeed,
                contact.tread == null ? "none" : contact.isDriven ? "drive" : "free",
                contact.targetSpeed, contact.grip, contact.pressure, contact.slide, contact.longitudinalImpulse, contact.lateralImpulse));
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
        Vector3d first = solved.isEmpty() ? nearGround.get(0).ownPoint() : solved.get(0).ownPoint();
        Vec3 at = subLevel.logicalPose().transformPosition(new Vec3(first.x, first.y, first.z));
        BntDebugLog.LOG.info("vehicle at {} bodies {} mass {} wheels {} near ground {} belt contacts {} belt push {} belt lon {} belt lat {} belt depth {} yaw {}{}",
            String.format("%.1f %.1f %.1f", at.x, at.y, at.z), bodies, String.format("%.1f", mass), wheels, nearGround.size(), beltContacts,
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
                    Vector3d force = forceTotal.getLocalForce();
                    Vector3d torque = forceTotal.getLocalTorque();
                    HELD.put(kbe, new double[]{force.x, force.y, force.z, torque.x, torque.y, torque.z});
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
            SoundEvent hitSound;
            float volume;
            float pitch;
            if (BntCogwheels.isIndustrial(state)) {
                Block industrialIronBlock = AllBlocks.INDUSTRIAL_IRON_BLOCK.get();
                hitSound = industrialIronBlock.getSoundType(industrialIronBlock.defaultBlockState(), level, soundPos, null).getBreakSound();
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
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        BntPhysicsEvents.Reach reach = reachGround(kbe, getTravelDirection(axis), pose, subLevel, getContactSeat(kbe, state),
            getContactRadius(state.getBlock(), isInTrack(kbe)), arm, up, down, 0.0);
        return reach.grounded() ? -Mth.clamp(reach.rise(), -down, up) : down;
    }

    private static BntPhysicsEvents.TerrainCastResult computeMaxExtensionToTerrain(
        KineticBlockEntity kbe, Vec3 centre, double radius, double above, double below,
        Vector3dc normalD, Pose3dc pose, SubLevel containingSubLevel, boolean shared
    ) {
        Vec3 sampleAxis = JOMLConversion.toMojang(normalD).normalize();
        long now = kbe.getLevel().getGameTime();
        double[] castKey = castKey(pose, centre, sampleAxis, radius, above, below);
        if (shared && kbe instanceof KineticBlockEntityPhysicsAccess cached
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

        if (shared && kbe instanceof KineticBlockEntityPhysicsAccess access) {
            if (minInteractingBlock != null) {
                access.bnt$setLastTerrainExtension(minExtension);
            } else if (blocked && Double.isFinite(access.bnt$getLastTerrainExtension())) {
                minExtension = access.bnt$getLastTerrainExtension();
            }
        }

        BntPhysicsEvents.TerrainCastResult result =
            new BntPhysicsEvents.TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
        if (shared && kbe instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setTerrainCast(new BntPhysicsEvents.TerrainCast(now, containingSubLevel, castKey, result));
        }
        return result;
    }

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

    private static class WheelContact {
        KineticBlockEntityPhysicsAccess mixin;
        ServerSubLevel subLevel;
        Pose3d pose;
        @Nullable
        Pose3dc framePose;
        @Nullable
        Vector3d ownPoint;
        Vector3d forcePoint;
        Vector3d normal;
        Vector3d lever;
        Vector3dc normalD;
        Vector3dc sideD;
        Vector3d localVelocity;
        double compression;
        double overTravel;
        double armRise;
        double rise;
        double climbRate;
        double travelUsed;
        double surfaceSpeed;
        double targetSpeed;
        double penetration;
        double support;
        double previousPush;
        double touchingFriction;
        BntGround.Surface ground = BntGround.UNKNOWN;
        double patch;
        double width;
        double pressure;
        double slide;
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
        double grip;
        double longitudinalCap;
        double longitudinalInverseMass;
        double lateralInverseMass;
        Vector3d longitudinalResponse;
        Vector3d lateralResponse;
        double longitudinalImpulse;
        double lateralImpulse;
        @Nullable
        BlockPos tread;
        boolean hasTraction;
        boolean isDriven;
        boolean loaded;
        boolean belt;
        boolean arm;

        Vector3d ownPoint() {
            return this.ownPoint != null ? this.ownPoint : this.forcePoint;
        }
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

    private record TerrainCast(long tick, SubLevel from, double[] key, BntPhysicsEvents.TerrainCastResult result) {
        boolean matches(long now, SubLevel containing, double[] castKey) {
            return this.tick == now && this.from == containing && Arrays.equals(this.key, castKey);
        }
    }
}
