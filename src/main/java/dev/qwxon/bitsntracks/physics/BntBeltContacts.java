package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltDrape;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltLinks;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltPath;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltSlack;
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
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
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

    /** A stretch of track hanging off the wheels, carrying its own weight. */
    public static final class BntBeltWeight {
        private KineticBlockEntityPhysicsAccess carrier;
        private ServerSubLevel subLevel;
        private Vector3d forcePoint;
        private double mass;
    }

    /** What a loop puts on the hull this tick. */
    public record BntBeltLoads(List<BntBeltContact> contacts, List<BntBeltWeight> weights) {
        public boolean isEmpty() {
            return this.contacts.isEmpty() && this.weights.isEmpty();
        }
    }

    public static BntBeltLoads build(
        ServerSubLevel subLevel, List<KineticBlockEntity> wheels, Level level
    ) {
        List<BntBeltContact> contacts = new ArrayList<>();
        List<BntBeltWeight> weights = new ArrayList<>();
        BntBeltLoads loads = new BntBeltLoads(contacts, weights);
        if (!BntPhysicsTuning.isBeltCollisionEnabled() || wheels.isEmpty()) {
            return loads;
        }

        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return loads;
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

            List<PathedCogwheelNode> nodes = chain instanceof BntChainGeometryRefresh refreshable
                ? refreshable.bnt$latchedBeltOrder()
                : List.of();
            if (nodes.size() < 2) {
                continue;
            }

            collectSpans(loads, level, subLevel, pose, massData, controllerPos, nodes, wheel.getSpeed());
        }
        return loads;
    }

    /** Walks a loop under the radius context the chain lengths are read from. */
    private static void collectSpans(
        BntBeltLoads loads, Level level, ServerSubLevel subLevel, Pose3d pose,
        MassData massData, BlockPos controllerPos, List<PathedCogwheelNode> nodes, float speed
    ) {
        Level heldLevel = BntRadiusProvider.level();
        BlockPos heldOrigin = BntRadiusProvider.origin();
        try {
            BntRadiusProvider.setLevel(level);
            BntRadiusProvider.setOrigin(controllerPos);
            spans(loads, level, subLevel, pose, massData, controllerPos, nodes, speed);
        } finally {
            BntRadiusProvider.setLevel(heldLevel);
            BntRadiusProvider.setOrigin(heldOrigin);
        }
    }

    private static void spans(
        BntBeltLoads loads, Level level, ServerSubLevel subLevel, Pose3d pose,
        MassData massData, BlockPos controllerPos, List<PathedCogwheelNode> nodes, float speed
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
        float tension = BntBeltTension.at(level, controllerPos);
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
            }
            planarU[i] = BntChainGeometry.planarX(centre, axis);
            planarV[i] = BntChainGeometry.planarY(centre, axis);
            alongAxis[i] = BntBeltPath.axisCoord(centre, axis);
            radii[i] = CogwheelSizeHelper.getRadius(state.getBlock());
            sides[i] = node.side();
            averageY += centre.y;
        }
        averageY /= count;

        double[][] runs = new double[count][];
        double[] runLengths = new double[count];
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            if (next == i) {
                continue;
            }
            runs[i] = BntBeltPath.tangent(
                planarU[i], planarV[i], sides[i] * radii[i],
                planarU[next], planarV[next], sides[next] * radii[next]);
            runLengths[i] = runs[i] == null ? 0.0 : runs[i][0];
        }

        int links = BntBeltLinks.resolve(level, controllerPos, tension);
        double surplus = BntBeltLinks.surplus(links, tension,
            BntBeltLinks.drawnTautLength(level, controllerPos, nodes));
        double[] slack = BntBeltSlack.distribute(
            runLengths, surplus, BntBeltSlack.tightRun(nodes, speed), speed);
        double massPerBlock = BntPhysicsTuning.getBeltMassPerBlock();

        for (int i = 0; i < count; i++) {
            double[] run = runs[i];
            if (run == null || run[0] < 1.0E-4) {
                continue;
            }
            int next = (i + 1) % count;

            double startU = planarU[i] + run[1];
            double startV = planarV[i] + run[2];
            double endU = planarU[next] + run[3];
            double endV = planarV[next] + run[4];

            Vec3 midpoint = BntBeltPath.fromPlanar(
                (startU + endU) * 0.5, (startV + endV) * 0.5, (alongAxis[i] + alongAxis[next]) * 0.5, axis);
            boolean underside = midpoint.y <= averageY;

            int samples = BntBeltDrape.probeCount(run[0]);
            double sag = BntBeltTension.sagFromSurplus(run[0], slack[i]);
            double segmentMass = massPerBlock * run[0] / samples;
            for (int sample = 0; sample < samples; sample++) {
                double along = (sample + 0.5) / samples;
                Vec3 chord = BntBeltPath.fromPlanar(
                    Mth.lerp(along, startU, endU),
                    Mth.lerp(along, startV, endV),
                    Mth.lerp(along, alongAxis[i], alongAxis[next]),
                    axis);
                double droop = BntBeltTension.droopAt(along, sag);
                Vec3 point = chord.subtract(0.0, droop, 0.0);

                BntBeltContact contact = underside
                    ? sample(level, subLevel, pose, massData, chord, droop, tension)
                    : null;
                if (contact != null) {
                    loads.contacts().add(contact);
                } else if (segmentMass > 0.0) {
                    BntBeltWeight weight = new BntBeltWeight();
                    weight.subLevel = subLevel;
                    weight.forcePoint = new Vector3d(point.x, point.y, point.z);
                    weight.mass = segmentMass;
                    loads.weights().add(weight);
                }
            }
        }
    }

    /** Contact for one belt sample, where the run hangs by droop below the taut chord. */
    private static BntBeltContact sample(
        Level level, ServerSubLevel subLevel, Pose3d pose, MassData massData,
        Vec3 chord, double droop, float tension
    ) {
        Vec3 localStart = chord.add(0.0, CAST_HEADROOM, 0.0);
        Vec3 localEnd = chord.subtract(0.0, droop + CAST_DROP, 0.0);
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

        double ground = localHit.y;
        double reach = ground - (chord.y - droop);
        if (reach < -CONTACT_REACH) {
            return null;
        }

        double penetration = ground - chord.y;
        if (penetration > CAST_HEADROOM) {
            return null;
        }

        Vec3 restPoint = new Vec3(chord.x, Mth.clamp(ground, chord.y - droop, chord.y), chord.z);
        Vector3d forcePoint = new Vector3d(restPoint.x, restPoint.y, restPoint.z);
        double inverseNormalMass = massData.getInverseNormalMass(forcePoint, OrientedBoundingBox3d.UP);
        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.0) {
            return null;
        }

        Vector3d velocity = Sable.HELPER.getVelocity(level, JOMLConversion.toJOML(restPoint));

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

    public static void assignCarrier(BntBeltWeight weight, KineticBlockEntityPhysicsAccess carrier) {
        weight.carrier = carrier;
    }

    /** Hangs a stretch of track off the hull under gravity. */
    public static void applyWeight(BntBeltWeight weight, Level level, double timeStep) {
        if (weight.carrier == null || weight.mass <= 0.0) {
            return;
        }

        Vector3d gravity = DimensionPhysicsData.getGravity(level);
        if (gravity == null || gravity.lengthSquared() < 1.0E-12) {
            return;
        }

        Vec3 worldImpulse = new Vec3(gravity.x, gravity.y, gravity.z).scale(weight.mass * timeStep);
        Vec3 localImpulse = weight.subLevel.logicalPose().transformNormalInverse(worldImpulse);

        ForceTotal forceTotal = weight.carrier.bnt$getForceTotal();
        forceTotal.applyImpulseAtPoint(
            weight.subLevel, weight.forcePoint,
            new Vector3d(localImpulse.x, localImpulse.y, localImpulse.z));
        weight.carrier.bnt$markQueuedForForceApplication();
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
