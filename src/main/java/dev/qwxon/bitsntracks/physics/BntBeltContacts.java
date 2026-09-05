package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltDrape;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltPath;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;

public final class BntBeltContacts {
    private static final double CAST_HEADROOM = 0.75;
    private static final double CAST_DROP = 2.0;
    private static final double CONTACT_REACH = 1.0 / 16.0;

    private BntBeltContacts() {
    }

    /** One point where a run meets the ground. */
    public static final class BntBeltContact {
        private KineticBlockEntityPhysicsAccess carrier;
        private ServerSubLevel subLevel;
        private Vector3d forcePoint;
        private double penetration;
        private Vector3d velocity;
        private double normalMass;
        private double friction;
        private float tension;
    }

    public static List<BntBeltContact> build(
        ServerSubLevel subLevel, List<KineticBlockEntity> wheels, Level level
    ) {
        List<BntBeltContact> contacts = new ArrayList<>();
        if (!BntPhysicsTuning.isBeltCollisionEnabled() || wheels.isEmpty()) {
            return contacts;
        }

        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return contacts;
        }

        Pose3d pose = subLevel.logicalPose();
        Set<BlockPos> visited = new HashSet<>();

        for (KineticBlockEntity wheel : wheels) {
            BlockPos controllerPos = controllerPos(wheel);
            if (controllerPos == null || !visited.add(controllerPos)) {
                continue;
            }

            CogwheelChain chain = chainAt(level, controllerPos);
            if (chain == null) {
                continue;
            }

            List<PathedCogwheelNode> nodes = chain.getChainPathCogwheelNodes();
            if (nodes.size() < 2) {
                continue;
            }

            collectSpans(contacts, level, subLevel, pose, massData, controllerPos, nodes);
        }
        return contacts;
    }

    private static void collectSpans(
        List<BntBeltContact> contacts, Level level, ServerSubLevel subLevel, Pose3d pose,
        MassData massData, BlockPos controllerPos, List<PathedCogwheelNode> nodes
    ) {
        Axis axis = BntChainGeometry.sharedAxis(nodes);
        if (axis == null || axis == Axis.Y) {
            return;
        }

        int count = nodes.size();
        double[] planarU = new double[count];
        double[] planarV = new double[count];
        double[] alongAxis = new double[count];
        double[] radii = new double[count];
        int[] sides = new int[count];
        float tension = BntBeltTension.DEFAULT;
        double averageY = 0.0;

        for (int i = 0; i < count; i++) {
            PathedCogwheelNode node = nodes.get(i);
            BlockPos nodePos = controllerPos.offset(node.localPos());
            BlockState state = level.getBlockState(nodePos);
            Vec3 centre = nodePos.getCenter()
                .add(0.0, CogwheelSizeHelper.getVerticalOffset(state.getBlock()), 0.0)
                .add(BntCogwheelPairing.seamOffset(state));
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess access) {
                centre = centre.add(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
                tension = BntBeltTension.clamp(access.bnt$getBeltTension());
            }
            planarU[i] = BntChainGeometry.planarX(centre, axis);
            planarV[i] = BntChainGeometry.planarY(centre, axis);
            alongAxis[i] = BntBeltPath.axisCoord(centre, axis);
            radii[i] = CogwheelSizeHelper.getChainRadius(state.getBlock());
            sides[i] = node.side();
            averageY += centre.y;
        }
        averageY /= count;

        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            if (next == i) {
                continue;
            }

            double[] run = BntBeltPath.tangent(
                planarU[i], planarV[i], sides[i] * radii[i],
                planarU[next], planarV[next], sides[next] * radii[next]);
            if (run == null || run[0] < 1.0E-4) {
                continue;
            }

            double startU = planarU[i] + run[1];
            double startV = planarV[i] + run[2];
            double endU = planarU[next] + run[3];
            double endV = planarV[next] + run[4];

            Vec3 midpoint = BntBeltPath.fromPlanar(
                (startU + endU) * 0.5, (startV + endV) * 0.5, (alongAxis[i] + alongAxis[next]) * 0.5, axis);
            if (midpoint.y > averageY) {
                continue;
            }

            int samples = BntBeltDrape.probeCount(run[0]);
            double sag = BntBeltTension.sagDepth(run[0], tension);
            for (int sample = 0; sample < samples; sample++) {
                double along = (sample + 0.5) / samples;
                Vec3 point = BntBeltPath.fromPlanar(
                    Mth.lerp(along, startU, endU),
                    Mth.lerp(along, startV, endV),
                    Mth.lerp(along, alongAxis[i], alongAxis[next]),
                    axis);
                point = point.subtract(0.0, BntBeltTension.droopAt(along, sag), 0.0);

                BntBeltContact contact = sample(level, subLevel, pose, massData, point, tension);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
        }
    }

    private static BntBeltContact sample(
        Level level, ServerSubLevel subLevel, Pose3d pose, MassData massData, Vec3 localPoint, float tension
    ) {
        Vec3 localStart = localPoint.add(0.0, CAST_HEADROOM, 0.0);
        Vec3 localEnd = localPoint.subtract(0.0, CAST_DROP, 0.0);
        ClipContext clipContext = new ClipContext(
            pose.transformPosition(localStart), pose.transformPosition(localEnd),
            ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
        ((ClipContextExtension)clipContext).sable$setSubLevelIgnoring(other -> other == subLevel);

        BlockHitResult hit = level.clip(clipContext);
        if (hit.getType() == Type.MISS) {
            return null;
        }
        if (hit.getDirection().getStepY() <= 0) {
            return null;
        }

        SubLevel hitSubLevel = Sable.HELPER.getContaining(level, hit.getLocation());
        Vec3 localHit = pose.transformPositionInverse(
            hitSubLevel == null ? hit.getLocation() : hitSubLevel.logicalPose().transformPosition(hit.getLocation()));

        double penetration = localHit.y - localPoint.y;
        if (penetration < -CONTACT_REACH || penetration > CAST_HEADROOM) {
            return null;
        }

        Vector3d forcePoint = new Vector3d(localPoint.x, localPoint.y, localPoint.z);
        double inverseNormalMass = massData.getInverseNormalMass(forcePoint, OrientedBoundingBox3d.UP);
        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.0) {
            return null;
        }

        Vector3d velocity = Sable.HELPER.getVelocity(level, JOMLConversion.toJOML(localPoint));

        BntBeltContact contact = new BntBeltContact();
        contact.subLevel = subLevel;
        contact.forcePoint = forcePoint;
        contact.penetration = Math.max(penetration, 0.0);
        contact.velocity = velocity;
        contact.normalMass = 1.0 / inverseNormalMass;
        contact.friction = PhysicsBlockPropertyHelper.getFriction(level.getBlockState(hit.getBlockPos()));
        contact.tension = tension;
        return contact;
    }

    /** Applies support and drag. */
    public static void apply(BntBeltContact contact, int shareCount, double timeStep) {
        if (contact.carrier == null) {
            return;
        }

        double normalMassShare = contact.normalMass / Math.max(shareCount, 1);
        double support = BntBeltTension.supportScale(contact.tension);
        double springStrength = BntPhysicsTuning.getBaseSuspensionStrength() * normalMassShare
            * BntPhysicsTuning.getSpringScale() * support;
        double dampingStrength = BntPhysicsTuning.getBaseSuspensionStrength() * normalMassShare
            * BntPhysicsTuning.getDampingScale() * support;

        double approachSpeed = contact.velocity.y;
        double springImpulse = contact.penetration * springStrength * timeStep;
        double dampingImpulse = -approachSpeed * dampingStrength * timeStep;
        double denom = 1.0 + (springStrength * timeStep * timeStep + dampingStrength * timeStep) / normalMassShare;
        double raw = (springImpulse + dampingImpulse) / denom;

        double ceiling = normalMassShare * (BntPhysicsTuning.getMaxSuspensionSpeed() + Math.abs(approachSpeed));
        double normalImpulse = Mth.clamp(raw, 0.0, ceiling);
        if (normalImpulse <= 0.0) {
            return;
        }

        double grip = BntBeltTension.gripScale(contact.tension) * Math.max(contact.friction, 0.0);
        double dragLimit = normalImpulse * grip;
        Vec3 worldImpulse = new Vec3(
            Mth.clamp(-contact.velocity.x * normalMassShare, -dragLimit, dragLimit),
            normalImpulse,
            Mth.clamp(-contact.velocity.z * normalMassShare, -dragLimit, dragLimit));

        Vec3 localImpulse = contact.subLevel.logicalPose().transformNormalInverse(worldImpulse);

        ForceTotal forceTotal = contact.carrier.bnt$getForceTotal();
        forceTotal.applyImpulseAtPoint(
            contact.subLevel, contact.forcePoint,
            new Vector3d(localImpulse.x, localImpulse.y, localImpulse.z));
        contact.carrier.bnt$markQueuedForForceApplication();
    }

    public static void assignCarrier(BntBeltContact contact, KineticBlockEntityPhysicsAccess carrier) {
        contact.carrier = carrier;
    }

    private static BlockPos controllerPos(KineticBlockEntity wheel) {
        CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)wheel.getBehaviour(CogwheelChainBehaviour.TYPE);
        if (behaviour == null || !behaviour.isPartOfChain()) {
            return null;
        }
        if (behaviour.getControlledChain() != null) {
            return wheel.getBlockPos();
        }
        return behaviour.getControllerOffset() == null ? null : wheel.getBlockPos().offset(behaviour.getControllerOffset());
    }

    private static CogwheelChain chainAt(Level level, BlockPos controllerPos) {
        return level.getBlockEntity(controllerPos) instanceof KineticBlockEntity kinetic
            && kinetic.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour
            ? behaviour.getControlledChain()
            : null;
    }
}
