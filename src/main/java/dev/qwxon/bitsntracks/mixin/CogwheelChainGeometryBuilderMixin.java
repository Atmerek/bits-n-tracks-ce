package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(
    value = {CogwheelChainGeometryBuilder.class},
    remap = false
)
public class CogwheelChainGeometryBuilderMixin {
    @Overwrite
    public static Vec3 getTangentPointOnCircle(PathedCogwheelNode previousNode, PathedCogwheelNode currentNode, boolean isIncoming) {
        Vec3 axis = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(currentNode.rotationAxis(), AxisDirection.POSITIVE).getNormal());
        Vec3 incomingPoint = currentNode.center().subtract(previousNode.center());
        if (previousNode.rotationAxis() != currentNode.rotationAxis()) {
            Vec3 prevAxis = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(previousNode.rotationAxis(), AxisDirection.POSITIVE).getNormal());
            incomingPoint = incomingPoint.subtract(prevAxis.scale(incomingPoint.dot(prevAxis)));
        }

        Vec3 incoming = incomingPoint.subtract(axis.scale(axis.dot(incomingPoint)));
        if (!isIncoming) {
            incoming = incoming.scale(-1.0);
        }

        double previousRadius = bnt$getTrackRadius(previousNode);
        double currentRadius = bnt$getTrackRadius(currentNode);
        if (previousNode.rotationAxis() != currentNode.rotationAxis()) {
            Vec3 prevAxis = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(previousNode.rotationAxis(), AxisDirection.POSITIVE).getNormal());
            return prevAxis.scale(previousNode.localPos().subtract(currentNode.localPos()).get(previousNode.rotationAxis()));
        } else if (previousNode.side() == currentNode.side()) {
            return incoming.normalize().cross(axis).scale(-currentRadius * currentNode.side());
        } else {
            double factor = previousRadius / (previousRadius + currentRadius);
            Vec3 tangentOrigin = incoming.scale(factor);
            double distance = (isIncoming ? 1 : -1) * tangentOrigin.length();
            double sineRatio = previousRadius / distance;
            double cosRatio = Math.sqrt(1.0 - sineRatio * sineRatio);
            double perpendicularHeight = cosRatio * currentRadius;
            double lengthAlongIncoming = sineRatio * currentRadius;
            return incoming.normalize().cross(axis).scale(-perpendicularHeight * currentNode.side()).add(incoming.normalize().scale(-lengthAlongIncoming));
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
