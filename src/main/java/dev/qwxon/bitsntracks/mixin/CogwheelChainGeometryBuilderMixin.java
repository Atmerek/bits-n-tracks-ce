package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainMotion;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(
    value = {CogwheelChainGeometryBuilder.class},
    remap = false
)
public class CogwheelChainGeometryBuilderMixin {
    @Shadow
    private static List<RenderedChainPathNode> wrappedArcBetweenPoints(
        PathedCogwheelNode currentNode,
        Vec3 outPreviousPositionWorld,
        Vec3 inCurrentOffsetWorld,
        Vec3 outCurrentOffsetWorld,
        Vec3 inNextPositionWorld
    ) {
        throw new AssertionError();
    }

    @Overwrite
    public static List<RenderedChainPathNode> buildFullChainFromPathNodes(List<PathedCogwheelNode> pathNodes) {
        List<PathedCogwheelNode> nodes = BntChainGeometry.effectiveSequence(pathNodes);
        List<RenderedChainPathNode> resultNodes = new ArrayList<>();
        List<Pair<Vec3, Vec3>> offsetsAtNodes = new ArrayList<>();
        int n = nodes.size();
        if (n == 0) {
            return resultNodes;
        }

        for (int i = 0; i < n; i++) {
            offsetsAtNodes.add(CogwheelChainGeometryBuilder.calculateOffsets(
                nodes.get((n + i - 1) % n), nodes.get(i), nodes.get((i + 1) % n)));
        }

        int size = pathNodes.size();
        int[] listIndex = new int[n];
        boolean[] present = new boolean[size];
        boolean[] emitted = new boolean[size];
        for (int i = 0; i < n; i++) {
            listIndex[i] = -1;
            for (int node = 0; node < size; node++) {
                if (pathNodes.get(node).localPos().equals(nodes.get(i).localPos())) {
                    listIndex[i] = node;
                    present[node] = true;
                    break;
                }
            }
        }

        boolean[] grazed = BntChainGeometry.grazing(nodes);
        for (int i = 0; i < n; i++) {
            if (!grazed[i]) {
                continue;
            }
            Pair<Vec3, Vec3> offsets = offsetsAtNodes.get(i);
            offsetsAtNodes.set(i, Pair.of(offsets.getSecond(), offsets.getSecond()));
        }

        for (int i = 0; i < n; i++) {
            PathedCogwheelNode previousNode = nodes.get((n + i - 1) % n);
            PathedCogwheelNode currentNode = nodes.get(i);
            PathedCogwheelNode nextNode = nodes.get((i + 1) % n);
            Pair<Vec3, Vec3> previousOffsets = offsetsAtNodes.get((i - 1 + n) % n);
            Pair<Vec3, Vec3> currentOffsets = offsetsAtNodes.get(i);
            Pair<Vec3, Vec3> nextOffsets = offsetsAtNodes.get((i + 1) % n);
            resultNodes.add(new RenderedChainPathNode(
                currentNode.localPos(), currentOffsets.getFirst(), currentNode.rotationAxisVec()));
            Vec3 currentDisplacement = BntChainMotion.displacement(currentNode.pos());
            resultNodes.addAll(wrappedArcBetweenPoints(
                currentNode,
                previousOffsets.getSecond().add(BntChainMotion.liveCenter(previousNode)).subtract(currentDisplacement),
                currentOffsets.getFirst().add(currentNode.center()),
                currentOffsets.getSecond().add(currentNode.center()),
                nextOffsets.getFirst().add(BntChainMotion.liveCenter(nextNode)).subtract(currentDisplacement)
            ));
            resultNodes.add(new RenderedChainPathNode(
                currentNode.localPos(), currentOffsets.getSecond(), currentNode.rotationAxisVec()));
            bnt$passThroughSkippedNodes(resultNodes, pathNodes, present, emitted,
                listIndex[i], listIndex[(i + 1) % n],
                BntChainMotion.liveCenter(currentNode).add(currentOffsets.getSecond()),
                BntChainMotion.liveCenter(nextNode).add(nextOffsets.getFirst()));
        }

        return resultNodes;
    }

    @Overwrite
    public static Vec3 getTangentPointOnCircle(PathedCogwheelNode previousNode, PathedCogwheelNode currentNode, boolean isIncoming) {
        if (previousNode.rotationAxis() != currentNode.rotationAxis()) {
            Vec3 previousAxis = bnt$axisVector(previousNode);
            return previousAxis.scale(previousNode.localPos().subtract(currentNode.localPos()).get(previousNode.rotationAxis()));
        }

        Vec3 axis = bnt$axisVector(currentNode);
        Vec3 currentCenter = BntChainMotion.liveCenter(currentNode);
        Vec3 previousCenter = BntChainMotion.liveCenter(previousNode);
        Vec3 travel = isIncoming
            ? currentCenter.subtract(previousCenter)
            : previousCenter.subtract(currentCenter);
        travel = travel.subtract(axis.scale(axis.dot(travel)));

        double currentRadius = BntChainGeometry.trackRadius(currentNode);
        double signedCurrent = currentRadius * currentNode.side();
        double distance = travel.length();
        if (distance < 1.0E-9) {
            return axis.cross(travel).scale(signedCurrent);
        }

        Vec3 forward = travel.scale(1.0 / distance);
        Vec3 outward = axis.cross(forward);
        double signedOther = BntChainGeometry.trackRadius(previousNode) * previousNode.side();
        double delta = isIncoming ? signedCurrent - signedOther : signedOther - signedCurrent;
        double cosine = delta / distance;
        if (Math.abs(cosine) >= 1.0) {
            return outward.scale(signedCurrent);
        }

        double sine = Math.sqrt(1.0 - cosine * cosine);
        return forward.scale(-cosine).add(outward.scale(sine)).scale(signedCurrent);
    }

    private static void bnt$passThroughSkippedNodes(
        List<RenderedChainPathNode> resultNodes,
        List<PathedCogwheelNode> pathNodes,
        boolean[] present,
        boolean[] emitted,
        int from,
        int to,
        Vec3 runStart,
        Vec3 runEnd
    ) {
        int size = pathNodes.size();
        if (from < 0 || to < 0) {
            return;
        }

        Vec3 along = runEnd.subtract(runStart);
        double lengthSquared = along.lengthSqr();
        double lower = 0.0;

        for (int step = 1; step < size; step++) {
            int candidate = (from + step) % size;
            if (candidate == to || present[candidate] || emitted[candidate]) {
                return;
            }

            emitted[candidate] = true;
            PathedCogwheelNode skipped = pathNodes.get(candidate);
            Vec3 center = BntChainMotion.liveCenter(skipped);
            double at = lengthSquared < 1.0E-12 ? 0.0 : along.dot(center.subtract(runStart)) / lengthSquared;
            at = Math.min(1.0, Math.max(lower, at));
            lower = at;
            resultNodes.add(new RenderedChainPathNode(
                skipped.localPos(), runStart.add(along.scale(at)).subtract(center), skipped.rotationAxisVec()));
        }
    }

    private static Vec3 bnt$axisVector(PathedCogwheelNode node) {
        return Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(node.rotationAxis(), AxisDirection.POSITIVE).getNormal());
    }
}
