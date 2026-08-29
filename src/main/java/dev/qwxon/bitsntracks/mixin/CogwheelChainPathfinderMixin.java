package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainPathfinder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.ICogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(
    value = {CogwheelChainPathfinder.class},
    remap = false
)
public class CogwheelChainPathfinderMixin {
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
            double radius = bnt$getTrackRadius(currentNode);
            return angle * radius;
        }
    }

    @Overwrite
    public static Vec3 getPathingTangentOnCog(ICogwheelNode from, ICogwheelNode to, int toSide) {
        return bnt$getPathingTangentOnCog(from.center(), from.rotationAxisVec(), to.center(), bnt$getTrackRadius(to), to.rotationAxisVec(), toSide);
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

    private static double bnt$getTrackRadius(ICogwheelNode node) {
        if (node instanceof PathedCogwheelNode pathedNode) {
            return bnt$getTrackRadius(pathedNode);
        } else {
            double fallback = node.isLarge()
                ? (node.hasSmallCogwheelOffset() ? BntPhysicsTuning.getMediumTrackRadius() : BntPhysicsTuning.getLargeTrackRadius())
                : (node.hasSmallCogwheelOffset() ? BntPhysicsTuning.getSmallTrackRadius() : BntPhysicsTuning.getTinyTrackRadius());
            if (node instanceof PlacingCogwheelNode placingNode) {
                double radius = BntRadiusProvider.getTrackRadius(placingNode.pos(), placingNode.isLarge(), fallback);
                if (radius > 0.0) {
                    return radius;
                }
            }

            return fallback;
        }
    }

    private static double bnt$getTrackRadius(PathedCogwheelNode node) {
        double fallback = node.isLarge()
            ? (node.hasSmallCogwheelOffset() ? BntPhysicsTuning.getMediumTrackRadius() : BntPhysicsTuning.getLargeTrackRadius())
            : (node.hasSmallCogwheelOffset() ? BntPhysicsTuning.getSmallTrackRadius() : BntPhysicsTuning.getTinyTrackRadius());
        double radius = BntRadiusProvider.getTrackRadius(node.pos(), node.isLarge(), fallback);
        return radius > 0.0 ? radius : fallback;
    }
}
