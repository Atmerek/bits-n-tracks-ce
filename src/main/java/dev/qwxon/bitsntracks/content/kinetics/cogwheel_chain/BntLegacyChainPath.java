package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.google.common.collect.ImmutableList;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import java.util.ArrayList;

public record BntLegacyChainPath(ImmutableList<PathedCogwheelNode> traversed, double distance, int chainIntersections) {
    public BntLegacyChainPath compare(BntLegacyChainPath other) {
        if (this.chainIntersections != other.chainIntersections) {
            return this.chainIntersections < other.chainIntersections ? this : other;
        }
        return this.distance > other.distance ? this : other;
    }

    public BntLegacyChainPath extend(PathedCogwheelNode nextNode, double additionalDistance, int additionalSelfIntersections) {
        ArrayList<PathedCogwheelNode> extended = new ArrayList<>(this.traversed);
        extended.add(nextNode);
        return new BntLegacyChainPath(
            ImmutableList.copyOf(extended),
            this.distance + additionalDistance,
            this.chainIntersections + additionalSelfIntersections
        );
    }
}
