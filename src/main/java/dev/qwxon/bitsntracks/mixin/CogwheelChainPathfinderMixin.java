package dev.qwxon.bitsntracks.mixin;

import com.google.common.collect.ImmutableList;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainPathfinder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.ICogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.ChainInteractionFailedException;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltSolver;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntLegacyChainPath;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(
    value = {CogwheelChainPathfinder.class},
    remap = false
)
public class CogwheelChainPathfinderMixin {
    @Overwrite
    public static List<PathedCogwheelNode> buildChainPath(PlacingCogwheelChain worldSpaceChain) throws ChainInteractionFailedException {
        PlacingCogwheelChain chain = worldSpaceChain.toLocalSpaceChain();
        List<PlacingCogwheelNode> nodes = chain.getNodes();
        int count = nodes.size();
        if (count < 2) {
            return null;
        }

        Axis axis = BntChainGeometry.sharedAxis(nodes);
        if (axis == null) {
            return bnt$legacyBuildChainPath(chain);
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        BntChainGeometry.fill(nodes, axis, xs, ys, radii);

        boolean[] touched = BntBeltSolver.contacts(xs, ys, radii);
        int[] sides = BntBeltSolver.sides(xs, ys, radii, touched);
        if (sides == null) {
            return null;
        }

        List<PathedCogwheelNode> path = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            path.add(new PathedCogwheelNode(nodes.get(i), sides[i]));
        }
        return path;
    }

    private static List<PathedCogwheelNode> bnt$legacyBuildChainPath(PlacingCogwheelChain chain) throws ChainInteractionFailedException {
        List<PlacingCogwheelNode> nodes = chain.getNodes();
        int count = nodes.size();
        BntLegacyChainPath leftPath = new BntLegacyChainPath(ImmutableList.of(new PathedCogwheelNode(nodes.getFirst(), 1)), 0.0, 0);
        BntLegacyChainPath rightPath = new BntLegacyChainPath(ImmutableList.of(new PathedCogwheelNode(nodes.getFirst(), -1)), 0.0, 0);
        PlacingCogwheelNode previousNode = nodes.get(0);

        for (int i = 1; i < count * 2; i++) {
            PlacingCogwheelNode nextNode = nodes.get(i % count);
            PlacingCogwheelNode nextNextNode = nodes.get((i + 1) % count);
            BntLegacyChainPath nextLeftPath = null;
            BntLegacyChainPath nextRightPath = null;

            for (int fromSide = 1; fromSide >= -1; fromSide -= 2) {
                BntLegacyChainPath fromPath = fromSide == 1 ? leftPath : rightPath;
                if (fromPath == null) {
                    continue;
                }

                for (int toSide = 1; toSide >= -1; toSide -= 2) {
                    if (!CogwheelChainPathfinder.isValidPathStep(previousNode, fromSide, nextNode, toSide)) {
                        continue;
                    }

                    BntLegacyChainPath extended = bnt$legacyExtend(previousNode, nextNode, fromSide, toSide, fromPath, nextNextNode);
                    if (toSide == 1) {
                        nextLeftPath = nextLeftPath == null ? extended : nextLeftPath.compare(extended);
                    } else {
                        nextRightPath = nextRightPath == null ? extended : nextRightPath.compare(extended);
                    }
                }
            }

            leftPath = nextLeftPath;
            rightPath = nextRightPath;
            if (nextLeftPath == null && nextRightPath == null) {
                throw new ChainInteractionFailedException("pathfinding_failed_at_node");
            }

            previousNode = nextNode;
        }

        BntLegacyChainPath finalPath = leftPath != null && rightPath != null
            ? leftPath.compare(rightPath)
            : (leftPath != null ? leftPath : rightPath);
        if (finalPath == null) {
            return null;
        }

        ArrayList<PathedCogwheelNode> traversed = new ArrayList<>(finalPath.traversed());
        traversed.removeLast();
        for (int i = 0; i < count - 1; i++) {
            traversed.removeFirst();
        }
        return traversed;
    }

    private static BntLegacyChainPath bnt$legacyExtend(PlacingCogwheelNode previousNode, PlacingCogwheelNode nextNode,
                                                       int fromSide, int toSide, BntLegacyChainPath fromPath,
                                                       PlacingCogwheelNode nextNextNode) {
        Vec3 fromPos = previousNode.center().add(getPathingTangentOnCog(nextNode, previousNode, -fromSide));
        Vec3 toPos = nextNode.center().add(getPathingTangentOnCog(previousNode, nextNode, toSide));
        ImmutableList<PathedCogwheelNode> traversed = fromPath.traversed();
        int traversedSize = traversed.size();
        int continuationSide = CogwheelChainPathfinder.isValidPathStep(nextNode, toSide, nextNextNode, toSide) ? toSide : -toSide;
        double distance = fromPos.distanceTo(toPos)
            + getArcDistanceOnCog(
                new PathedCogwheelNode(previousNode, fromSide),
                new PathedCogwheelNode(nextNode, toSide),
                new PathedCogwheelNode(nextNextNode, continuationSide)
            ) * 10.0;
        int selfIntersections = nextNextNode == previousNode
            ? (toSide != fromSide ? 1 : 0)
            : (traversedSize < 2
                ? 0
                : CogwheelChainPathfinder.getSelfIntersection(
                    traversed.get(traversedSize - 2), traversed.get(traversedSize - 1), nextNode, toSide));
        return fromPath.extend(new PathedCogwheelNode(nextNode, toSide), distance, selfIntersections);
    }

    @Overwrite
    private static double getArcDistanceOnCog(PathedCogwheelNode prevNode, PathedCogwheelNode currentNode, PathedCogwheelNode nextNode) {
        Vec3 fromTangent = CogwheelChainGeometryBuilder.getTangentPointOnCircle(prevNode, currentNode, true);
        Vec3 toTangent = CogwheelChainGeometryBuilder.getTangentPointOnCircle(nextNode, currentNode, false);
        Vec3 incomingDiff = currentNode.center().subtract(prevNode.center());
        if (toTangent.distanceToSqr(fromTangent) < 1.0E-4) {
            return 0.0;
        } else if (incomingDiff.normalize().dot(toTangent.subtract(fromTangent)) < 0.0) {
            return 0.0;
        } else {
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, fromTangent.normalize().dot(toTangent.normalize()))));
            return angle * BntChainGeometry.trackRadius(currentNode);
        }
    }

    @Overwrite
    public static Vec3 getPathingTangentOnCog(ICogwheelNode from, ICogwheelNode to, int toSide) {
        return bnt$getPathingTangentOnCog(
            from.center(), from.rotationAxisVec(), to.center(), BntChainGeometry.trackRadius(to), to.rotationAxisVec(), toSide);
    }

    @Overwrite
    public static Vec3 getPathingTangentOnCog(Vec3 fromCenter, Vec3 fromRotationAxis, Vec3 toCenter, boolean toLarge, Vec3 toRotationAxis, int toSide) {
        double toRadius = toLarge ? BntPhysicsTuning.getLargeTrackRadius() : BntPhysicsTuning.getSmallTrackRadius();
        return bnt$getPathingTangentOnCog(fromCenter, fromRotationAxis, toCenter, toRadius, toRotationAxis, toSide);
    }

    private static Vec3 bnt$getPathingTangentOnCog(Vec3 fromCenter, Vec3 fromRotationAxis, Vec3 toCenter, double toRadius, Vec3 toRotationAxis, int toSide) {
        Vec3 differenceTo = toCenter.subtract(fromCenter);
        if (!fromRotationAxis.equals(toRotationAxis)) {
            differenceTo = CogwheelChainPathfinder.projectDirToAxisPlane(
                CogwheelChainPathfinder.projectDirToAxisPlane(differenceTo, toRotationAxis), fromRotationAxis
            );
        }

        return toRotationAxis.cross(differenceTo).normalize().scale(toSide * toRadius);
    }
}
