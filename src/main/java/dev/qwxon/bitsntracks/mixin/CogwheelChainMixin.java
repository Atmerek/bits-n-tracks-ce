package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainMotion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {CogwheelChain.class},
    remap = false
)
public abstract class CogwheelChainMixin implements BntChainGeometryRefresh {
    @Shadow
    private List<PathedCogwheelNode> cogwheelNodes;

    @Shadow
    private List<RenderedChainPathNode> renderedNodes;

    @Shadow
    private List<CogwheelChainSegment> cachedSegments;

    @Shadow
    private void updateInsideOutsideFlip() {
        throw new AssertionError();
    }

    @Unique
    private double[] bnt$builtDisplacements;

    @Unique
    private boolean[] bnt$engagedNodes;

    @Unique
    private double[] bnt$engagedDisplacements;

    @Unique
    private BntChainGeometry.Layout bnt$latched;

    @Unique
    private boolean bnt$repairAttempted;

    @Unique
    private BntChainGeometry.Layout bnt$restoredLayout;

    @Unique
    private BntChainGeometry.Layout bnt$latchedLayout(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        double[] signature = BntChainEngagement.signature(level, controllerPos, nodes);
        BntChainGeometry.Layout restored = this.bnt$restoredLayout;
        if (restored != null) {
            this.bnt$restoredLayout = null;
            if (BntChainEngagement.stillHolds(level, controllerPos, nodes, restored)) {
                this.bnt$engagedDisplacements = signature;
                this.bnt$repairAttempted = false;
                this.bnt$latched = restored;
                this.bnt$adoptSides(nodes, restored.sides());
                return restored;
            }
        }

        if (this.bnt$latched != null
            && this.bnt$latched.sides().length == nodes.size()
            && Arrays.equals(signature, this.bnt$engagedDisplacements)
            && BntChainEngagement.stillHolds(level, controllerPos, nodes, this.bnt$latched)) {
            return this.bnt$latched;
        }

        this.bnt$engagedDisplacements = signature;
        this.bnt$repairAttempted = false;
        this.bnt$latched = BntChainEngagement.layout(level, controllerPos, nodes);
        if (this.bnt$latched != null) {
            this.bnt$adoptSides(nodes, this.bnt$latched.sides());
        }
        return this.bnt$latched;
    }

    @Override
    public boolean bnt$isNodeEngaged(Level level, BlockPos controllerPos, BlockPos nodeLocalPos) {
        List<PathedCogwheelNode> nodes = this.cogwheelNodes;
        if (level == null || controllerPos == null || nodes == null || nodes.size() < 2) {
            return true;
        }

        BntChainGeometry.Layout layout = this.bnt$latchedLayout(level, controllerPos, nodes);
        this.bnt$engagedNodes = BntChainEngagement.engagement(layout, nodes.size());

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).localPos().equals(nodeLocalPos)) {
                return this.bnt$engagedNodes[i];
            }
        }
        return true;
    }

    @Override
    public void bnt$verifyKinetics(Level level, BlockPos controllerPos) {
        List<PathedCogwheelNode> nodes = this.cogwheelNodes;
        if (level == null || controllerPos == null || nodes == null || nodes.size() < 2) {
            return;
        }

        BntChainGeometry.Layout layout = this.bnt$latchedLayout(level, controllerPos, nodes);
        if (this.bnt$repairAttempted) {
            return;
        }

        nodes = this.cogwheelNodes;
        boolean[] engaged = BntChainEngagement.engagement(layout, nodes.size());
        if (BntChainEngagement.drivesTogether(level, controllerPos, nodes, engaged)) {
            return;
        }

        this.bnt$repairAttempted = true;
        BntChainEngagement.rebuild(level, controllerPos, nodes);
    }

    @Inject(
        method = {"read"},
        at = {@At("TAIL")}
    )
    private void bnt$forgetBuiltGeometry(CompoundTag tag, CallbackInfo ci) {
        this.bnt$builtDisplacements = null;
        this.bnt$latched = null;
        this.bnt$engagedDisplacements = null;
        this.bnt$restoredLayout = bnt$readLayout(tag, this.cogwheelNodes.size());
    }

    @Inject(
        method = {"write"},
        at = {@At("TAIL")}
    )
    private void bnt$writeLayout(CompoundTag tag, CallbackInfo ci) {
        BntChainGeometry.Layout layout = this.bnt$latched != null ? this.bnt$latched : this.bnt$restoredLayout;
        if (layout == null || layout.sides().length != this.cogwheelNodes.size()) {
            return;
        }

        tag.putIntArray("BNT_PathOrder", layout.sequence());
        tag.putIntArray("BNT_PathSides", layout.sides());
    }

    @Unique
    private static BntChainGeometry.Layout bnt$readLayout(CompoundTag tag, int count) {
        if (!tag.contains("BNT_PathOrder") || !tag.contains("BNT_PathSides")) {
            return null;
        }

        int[] sequence = tag.getIntArray("BNT_PathOrder");
        int[] sides = tag.getIntArray("BNT_PathSides");
        if (sides.length != count || sequence.length < 2) {
            return null;
        }
        for (int index : sequence) {
            if (index < 0 || index >= count) {
                return null;
            }
        }
        return new BntChainGeometry.Layout(sequence, sides);
    }

    @Override
    public void bnt$refreshChainGeometry(Level level, BlockPos controllerPos) {
        List<PathedCogwheelNode> nodes = this.cogwheelNodes;
        if (nodes == null || nodes.size() < 2) {
            return;
        }

        double[] displacements = BntChainMotion.displacementSignature(nodes);
        if (Arrays.equals(displacements, this.bnt$builtDisplacements)) {
            return;
        }

        this.bnt$builtDisplacements = displacements;
        BntChainGeometry.Layout layout = this.bnt$latchedLayout(level, controllerPos, nodes);
        BntChainGeometry.Layout previous = BntChainMotion.swapLayout(layout);

        try {
            this.renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(this.cogwheelNodes);
        } finally {
            BntChainMotion.swapLayout(previous);
        }

        this.cachedSegments = null;
    }

    @Unique
    private void bnt$adoptSides(List<PathedCogwheelNode> nodes, int[] sides) {
        if (sides.length != nodes.size()) {
            return;
        }

        boolean changed = false;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).side() != sides[i]) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }

        List<PathedCogwheelNode> updated = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            PathedCogwheelNode node = nodes.get(i);
            updated.add(new PathedCogwheelNode(
                sides[i], node.isLarge(), node.rotationAxis(), node.localPos(), node.hasSmallCogwheelOffset()));
        }
        this.cogwheelNodes = updated;
        this.updateInsideOutsideFlip();
    }
}
