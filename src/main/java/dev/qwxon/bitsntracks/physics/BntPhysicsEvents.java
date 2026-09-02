package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
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
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private static final double BLOCKS_PER_SECOND_PER_RPM_RADIUS = Math.PI * 2.0 / 60.0;
    private static final int TRACTION_ITERATIONS = 8;
    private static final double MAX_COMMANDED_YAW_RATE = 20.0;

    private BntPhysicsEvents() {
    }

    public static void register() {
        SableEventPlatform.INSTANCE.onPhysicsTick(BntPhysicsEvents::onPhysicsTick);
    }

    public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();
        Map<ServerSubLevel, List<BntPhysicsEvents.WheelContact>> contactsByBody = new Reference2ObjectOpenHashMap<>();

        Iterator<KineticBlockEntity> iterator = BntPhysicsRegistry.getEnabled(level).iterator();

        while (iterator.hasNext()) {
            KineticBlockEntity kbe = iterator.next();
            if (kbe.isRemoved()) {
                iterator.remove();
            } else {
                KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
                if (mixin.bnt$isPhysicsEnabled() && Sable.HELPER.getContaining(kbe) instanceof ServerSubLevel subLevel && !subLevel.isRemoved()) {
                    BntPhysicsEvents.WheelContact contact = resolveContact(kbe, mixin, subLevel);
                    if (contact != null) {
                        contactsByBody.computeIfAbsent(subLevel, ignored -> new ArrayList<>()).add(contact);
                    }
                }
            }
        }

        for (Map.Entry<ServerSubLevel, List<BntPhysicsEvents.WheelContact>> entry : contactsByBody.entrySet()) {
            List<BntPhysicsEvents.WheelContact> nearGround = entry.getValue();
            List<BntPhysicsEvents.WheelContact> loaded = new ArrayList<>(nearGround.size());
            for (BntPhysicsEvents.WheelContact contact : nearGround) {
                if (contact.loaded) {
                    loaded.add(contact);
                }
            }
            if (loaded.isEmpty()) {
                continue;
            }

            solveTraction(entry.getKey(), loaded, timeStep);

            for (BntPhysicsEvents.WheelContact contact : loaded) {
                applyWheelForces(contact, nearGround.size(), timeStep);
            }
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

    public static double getClientRenderExtension(KineticBlockEntity kbe, float partialTick) {
        if (kbe instanceof KineticBlockEntityPhysicsAccess mixin && mixin.bnt$isPhysicsEnabled()) {
            Level level = kbe.getLevel();
            if (level != null && level.isClientSide) {
                BlockState state = kbe.getBlockState();
                if (!state.hasProperty(BlockStateProperties.AXIS)) {
                    return mixin.bnt$getLerpedExtension(partialTick);
                } else {
                    ClientSubLevel subLevel = Sable.HELPER.getContainingClient(kbe);
                    return subLevel == null ? 0.0 : computeRenderExtensionForPose(kbe, subLevel.renderPose(partialTick), subLevel);
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
        double wheelRadius = CogwheelSizeHelper.getRadius(block);
        double suspensionRest = CogwheelSizeHelper.getSuspensionRest(block);
        Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
        Vec3 localPos = getWheelCenter(kbe, state);
        Vector3d forcePoint = new Vector3d(localPos.x, localPos.y, localPos.z);
        Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
        Vector3dc sideD = new Vector3d(sideVec.getX(), sideVec.getY(), sideVec.getZ());
        Vector3dc normalD = getTravelDirection(axis);
        Pose3d pose = subLevel.logicalPose();

        double inverseNormalMass = massData.getInverseNormalMass(forcePoint, OrientedBoundingBox3d.UP);
        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.0) {
            return null;
        }

        double normalMass = 1.0 / inverseNormalMass;

        BntPhysicsEvents.TerrainCastResult extResult = computeMaxExtensionToTerrain(kbe, normalD, pose, subLevel);
        double maxExtension = extResult.maxExtension;
        boolean wasLiftedUp = mixin.bnt$isLiftedUp();
        if (maxExtension > suspensionRest + wheelRadius + 0.25) {
            mixin.bnt$setMaxAirExtension(Math.max(mixin.bnt$getMaxAirExtension(), maxExtension));
            mixin.bnt$setLiftedUp(true);
            mixin.bnt$setExtension(suspensionRest);
            return null;
        }

        mixin.bnt$setLiftedUp(false);
        Vector3d velocity = Sable.HELPER.getVelocity(kbe.getLevel(), JOMLConversion.toJOML(localPos));
        Vector3d localVelocity = pose.transformNormalInverse(velocity);
        double maxAirExtension = mixin.bnt$getMaxAirExtension();
        if (wasLiftedUp && velocity.y < -0.5 && maxAirExtension >= suspensionRest + wheelRadius + BntPhysicsTuning.getLandingSoundMinFallBlocks()) {
            playLandingSound(kbe, state);
        }

        if (wasLiftedUp || maxAirExtension > 0.0) {
            mixin.bnt$setMaxAirExtension(0.0);
        }

        double touchingFriction = 1.0;
        if (extResult.minInteractingBlock != null) {
            touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(kbe.getLevel().getBlockState(extResult.minInteractingBlock)));
        }

        mixin.bnt$setExtension(maxExtension);

        double distance = suspensionRest / 6.0 + maxExtension;
        double springLength = Mth.clamp(distance - wheelRadius, -suspensionRest * 2.0, suspensionRest);
        CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)kbe.getBehaviour(CogwheelChainBehaviour.TYPE);
        boolean isConnected = behaviour != null && behaviour.isPartOfChain();
        BntPhysicsEvents.WheelContact contact = new BntPhysicsEvents.WheelContact();
        contact.mixin = mixin;
        contact.subLevel = subLevel;
        contact.pose = pose;
        contact.forcePoint = forcePoint;
        contact.normalD = normalD;
        contact.sideD = sideD;
        contact.localVelocity = localVelocity;
        contact.extResult = extResult;
        contact.suspensionRest = suspensionRest;
        contact.springLength = springLength;
        contact.chainRadius = CogwheelSizeHelper.getChainRadius(block);
        contact.kineticSpeed = kbe.getSpeed();
        contact.touchingFriction = touchingFriction;
        contact.normalMass = normalMass;
        contact.hasTraction = axis != Axis.Y;
        contact.isConnected = isConnected;
        contact.isTrackModel = isConnected || block.getDescriptionId().contains("track");
        contact.brakeStrength = kbe.getLevel().getSignal(kbe.getBlockPos().above(), Direction.DOWN) / 15.0;
        contact.loaded = extResult.minInteractingBlock != null && springLength < suspensionRest;
        return contact;
    }

    private static void applyWheelForces(BntPhysicsEvents.WheelContact contact, int shareCount, double timeStep) {
        Vector3d queuedForce = new Vector3d();
        boolean suspensionEnabled = contact.isTrackModel
            ? BntPhysicsTuning.isTrackSuspensionEnabled()
            : BntPhysicsTuning.isCogwheelSuspensionEnabled();
        double normalMassShare = contact.normalMass / shareCount;
        double suspensionGain = suspensionEnabled ? BntPhysicsTuning.getBaseSuspensionStrength() * normalMassShare : 0.0;
        double springMult = contact.isTrackModel ? BntPhysicsTuning.getTrackSpringMultiplier() : BntPhysicsTuning.getCogwheelSpringMultiplier();
        double dampingMult = contact.isTrackModel ? BntPhysicsTuning.getTrackDampingMultiplier() : BntPhysicsTuning.getCogwheelDampingMultiplier();
        double springStrength = suspensionGain * BntPhysicsTuning.getSpringScale() * springMult;
        double dampingStrength = suspensionGain * BntPhysicsTuning.getDampingScale() * dampingMult;
        double relVelY = contact.localVelocity.y;
        double dampingImpulse = -relVelY * dampingStrength * timeStep;
        double springImpulse = (contact.suspensionRest - contact.springLength) * springStrength * timeStep;
        double denom = 1.0 + (springStrength * timeStep * timeStep + dampingStrength * timeStep) / normalMassShare;
        double rawSpringForce = (springImpulse + dampingImpulse - springStrength * timeStep * timeStep * relVelY) / denom;
        double maxImpulseMult = contact.isTrackModel
            ? BntPhysicsTuning.getTrackMaxImpulseMultiplier()
            : BntPhysicsTuning.getCogwheelMaxImpulseMultiplier();
        double bumpStopScale = contact.springLength < 0.0 ? BntPhysicsTuning.getBumpStopScale() : 1.0;
        double maxImpulseVal = maxImpulseMult * suspensionGain * BntPhysicsTuning.getImpulseScale() * timeStep * bumpStopScale;
        double speedLimitImpulse = normalMassShare * (BntPhysicsTuning.getMaxSuspensionSpeed() + Math.abs(relVelY));
        double impulseCeiling = Math.min(maxImpulseVal, speedLimitImpulse);
        double springForce = Mth.clamp(rawSpringForce, -impulseCeiling, impulseCeiling);
        Vec3i rayHitNormal = contact.extResult.normal.getNormal();
        Vec3 localForce = new Vec3(springForce * rayHitNormal.getX(), springForce * rayHitNormal.getY(), springForce * rayHitNormal.getZ());
        if (contact.extResult.subLevel != null) {
            localForce = contact.extResult.subLevel.logicalPose().transformNormal(localForce);
        }

        localForce = contact.pose.transformNormalInverse(localForce);
        queuedForce.set(localForce.x, localForce.y, localForce.z);

        queuedForce.fma(contact.longitudinalImpulse, contact.normalD);
        queuedForce.fma(contact.lateralImpulse, contact.sideD);

        ForceTotal forceTotal = contact.mixin.bnt$getForceTotal();
        forceTotal.applyImpulseAtPoint(contact.subLevel, contact.forcePoint, queuedForce);
        contact.mixin.bnt$markQueuedForForceApplication();
    }

    private static void solveTraction(ServerSubLevel subLevel, List<BntPhysicsEvents.WheelContact> contacts, double timeStep) {
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

        double loadShare = massData.getMass() / contacts.size();
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
            double beltSpeed = contact.chainRadius * contact.kineticSpeed * BLOCKS_PER_SECOND_PER_RPM_RADIUS;
            double targetSpeed = contact.isConnected ? beltSpeed * (1.0 - contact.brakeStrength) : 0.0;
            double driveTraction = contact.isConnected
                ? BntPhysicsTuning.getDriveTraction() + contact.brakeStrength * BntPhysicsTuning.getBrakeTraction()
                : BntPhysicsTuning.getRollingResistance();
            double longitudinalSpeed = contact.localVelocity.dot(contact.normalD);
            contact.targetSpeed = targetSpeed;
            contact.goalLongitudinal = longitudinalSpeed + response * (targetSpeed - longitudinalSpeed);
            contact.limitLongitudinal = driveTraction * grip * loadShare * timeStep;
            contact.limitLateral = BntPhysicsTuning.getLateralTraction() * grip * loadShare * timeStep;
        }

        if (!anyTraction) {
            return;
        }

        double commandedYawRate = pivotScrub > 0.0 ? solveCommandedYawRate(contacts) : 0.0;

        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (contact.hasTraction) {
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

        for (BntPhysicsEvents.WheelContact contact : contacts) {
            if (!contact.hasTraction || !contact.isConnected) {
                continue;
            }

            driven++;
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

        if (driven < 2) {
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
        BlockState state = kbe.getBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return CogwheelSizeHelper.getSuspensionRest(state.getBlock());
        } else {
            double wheelRadius = CogwheelSizeHelper.getRadius(state.getBlock());
            double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
            Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
            Pose3dc pose = subLevel.logicalPose();
            BntPhysicsEvents.TerrainCastResult extensionToTerrain = computeMaxExtensionToTerrain(kbe, getTravelDirection(axis), pose, subLevel);
            double unclampedExtension = extensionToTerrain.maxExtension - wheelRadius;
            mixin.bnt$setLiftedUp(unclampedExtension > suspensionRest);
            return Mth.clamp(unclampedExtension, -suspensionRest * 3.0, suspensionRest);
        }
    }

    private static double computeRenderExtensionForPose(KineticBlockEntity kbe, Pose3dc pose, SubLevel subLevel) {
        BlockState state = kbe.getBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return CogwheelSizeHelper.getSuspensionRest(state.getBlock());
        } else {
            double wheelRadius = CogwheelSizeHelper.getRadius(state.getBlock());
            double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
            Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
            BntPhysicsEvents.TerrainCastResult extensionToTerrain = computeMaxExtensionToTerrain(kbe, getTravelDirection(axis), pose, subLevel);
            return Mth.clamp(extensionToTerrain.maxExtension - wheelRadius, -suspensionRest * 3.0, suspensionRest);
        }
    }

    private static BntPhysicsEvents.TerrainCastResult computeMaxExtensionToTerrain(
        KineticBlockEntity kbe, Vector3dc normalD, Pose3dc pose, SubLevel containingSubLevel
    ) {
        BlockState state = kbe.getBlockState();
        Vec3 wheelPosCenter = getWheelCenter(kbe, state);
        double wheelRadius = CogwheelSizeHelper.getRadius(state.getBlock());
        double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
        Vec3 sampleAxis = JOMLConversion.toMojang(normalD).normalize();
        double maxCastHeight = wheelRadius + suspensionRest + 1.5;
        double minExtension = 5.0;
        Direction minNormal = Direction.UP;
        SubLevel minHitSubLevel = null;
        BlockPos minInteractingBlock = null;

        for (double sampleOffset : getTerrainSampleOffsets(wheelRadius)) {
            Vec3 localPosO = wheelPosCenter.add(sampleAxis.scale(sampleOffset));
            Vec3 localRayStart = localPosO.add(0.0, maxCastHeight, 0.0);
            Vec3 localRayEnd = localPosO.subtract(0.0, 5.0, 0.0);
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
                Vec3 localHitPos = pose.transformPositionInverse(
                    hitSubLevel == null ? clipResult.getLocation() : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation())
                );
                if (localHitPos.y > wheelPosCenter.y + suspensionRest + 0.5) {
                    if (hitSubLevel == null || ignoredSubLevels.contains(hitSubLevel)) {
                        break;
                    }

                    ignoredSubLevels.add(hitSubLevel);
                } else if (hitSubLevel != null && hitSubLevel != containingSubLevel && localHitPos.y > wheelPosCenter.y - wheelRadius * 0.25) {
                    if (ignoredSubLevels.contains(hitSubLevel)) {
                        break;
                    }

                    ignoredSubLevels.add(hitSubLevel);
                } else {
                    if (localHitPos.y < wheelPosCenter.y - suspensionRest * 3.0) {
                        break;
                    }

                    Direction dir = clipResult.getDirection();
                    Vector3d hitNormal = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                    if (hitSubLevel != null) {
                        hitSubLevel.logicalPose().transformNormal(hitNormal);
                    }

                    if (!(hitNormal.dot(0.0, 1.0, 0.0) < 0.5)) {
                        double dist = wheelPosCenter.y - localHitPos.y;
                        pose.transformNormalInverse(hitNormal);
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

        return new BntPhysicsEvents.TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
    }

    private static double[] getTerrainSampleOffsets(double wheelRadius) {
        double inner = wheelRadius * 0.5;
        return new double[]{-wheelRadius, -inner, 0.0, inner, wheelRadius};
    }

    private static Vec3 getWheelCenter(KineticBlockEntity kbe, BlockState state) {
        Vec3 center = kbe.getBlockPos().getCenter().add(0.0, CogwheelSizeHelper.getVerticalOffset(state.getBlock()), 0.0);
        if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
            center = center.add(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        }

        return center;
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
        Vector3d lever;
        Vector3dc normalD;
        Vector3dc sideD;
        Vector3d localVelocity;
        BntPhysicsEvents.TerrainCastResult extResult;
        double suspensionRest;
        double springLength;
        double chainRadius;
        double kineticSpeed;
        double targetSpeed;
        double touchingFriction;
        double normalMass;
        double brakeStrength;
        double goalLongitudinal;
        double goalLateral;
        double limitLongitudinal;
        double limitLateral;
        double longitudinalImpulse;
        double lateralImpulse;
        boolean hasTraction;
        boolean isConnected;
        boolean isTrackModel;
        boolean loaded;
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
}
