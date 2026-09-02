package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment.SegmentType;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class BntChainEdit {
    private BntChainEdit() {
    }

    public static int nodeIndexForSegment(CogwheelChain chain, CogwheelChainSegment segment) {
        List<CogwheelChainSegment> segments = chain.getSegments();
        int[] edges = edgeStarts(chain);
        if (edges.length != segments.size()) {
            return -1;
        }
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).equals(segment)) {
                return edges[i];
            }
        }
        return -1;
    }

    public static CogwheelChainSegment segmentForNodeIndex(CogwheelChain chain, int nodeIndex, float chainPosition) {
        List<CogwheelChainSegment> segments = chain.getSegments();
        int[] edges = edgeStarts(chain);
        if (nodeIndex < 0 || edges.length != segments.size()) {
            return null;
        }

        CogwheelChainSegment fallback = null;
        for (int i = 0; i < segments.size(); i++) {
            CogwheelChainSegment segment = segments.get(i);
            if (segment.type() != SegmentType.BETWEEN_NODES || edges[i] != nodeIndex) {
                continue;
            }
            if (chainPosition + 0.001F >= segment.startDist() && chainPosition - 0.001F <= segment.endDist()) {
                return segment;
            }
            if (fallback == null) {
                fallback = segment;
            }
        }
        if (fallback == null) {
            return null;
        }
        return new CogwheelChainSegment(
            fallback.fromPosition(), fallback.toPosition(), SegmentType.BETWEEN_NODES,
            Math.min(fallback.startDist(), chainPosition), Math.max(fallback.endDist(), chainPosition));
    }

    private static int[] edgeStarts(CogwheelChain chain) {
        List<RenderedChainPathNode> rendered = chain.getChainPathNodes();
        List<PathedCogwheelNode> nodes = chain.getChainPathCogwheelNodes();
        int count = rendered.size();
        int size = nodes.size();
        int[] edges = new int[count];
        if (count == 0 || size == 0) {
            return edges;
        }

        int[] cogs = new int[count];
        for (int i = 0; i < count; i++) {
            cogs[i] = -1;
            BlockPos localPos = rendered.get(i).relativePos();
            for (int node = 0; node < size; node++) {
                if (nodes.get(node).localPos().equals(localPos)) {
                    cogs[i] = node;
                    break;
                }
            }
        }

        int start = cogs[0];
        if (start < 0) {
            return edges;
        }

        int pointer = start;
        int base = start;
        for (int i = 0; i < count; i++) {
            int cog = cogs[i];
            if (cog >= 0 && cyclicDistance(start, cog, size) > cyclicDistance(start, pointer, size)) {
                pointer = cog;
                base = cog;
            }
            edges[i] = base;
        }
        return edges;
    }

    private static int cyclicDistance(int from, int to, int size) {
        return (to - from + size) % size;
    }
}
