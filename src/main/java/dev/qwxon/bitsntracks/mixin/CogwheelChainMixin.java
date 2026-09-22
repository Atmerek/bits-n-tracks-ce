package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.kipti.bnb.registry.core.BnbTags.BnbBlockTags;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainMotion;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import dev.qwxon.bitsntracks.physics.BntPonderPhysics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    private double[] bnt$engagedDisplacements;

    @Unique
    private BntChainGeometry.Layout bnt$latched;

    /** Set while one fault is still standing, so a repair runs once for it and not every lazy tick. */
    @Unique
    private boolean bnt$repairAttempted;

    @Unique
    private BntChainGeometry.Layout bnt$restoredLayout;

    @Unique
    private boolean bnt$sidesPending;

    @Unique
    private BntChainGeometry.Layout bnt$appliedLayout;

    @Unique
    private long bnt$latchedAt = Long.MIN_VALUE;

    @Unique
    private long bnt$holdsCheckedAt = Long.MIN_VALUE;

    @Unique
    private boolean bnt$holds;

    @Unique
    private BntChainGeometry.Layout bnt$latchedLayout(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        double[] signature = BntChainEngagement.signature(level, controllerPos, nodes);
        BntChainGeometry.Layout restored = this.bnt$restoredLayout;
        if (restored != null) {
            this.bnt$restoredLayout = null;
            if (BntChainEngagement.stillHolds(level, controllerPos, nodes, restored)
                && BntChainEngagement.agreesWithSolver(level, controllerPos, nodes, restored)) {
                this.bnt$engagedDisplacements = signature;
                this.bnt$repairAttempted = false;
                this.bnt$latch(level, restored);
                this.bnt$applySides(level, nodes, restored.sides());
                return restored;
            }
        }

        if (this.bnt$latched != null
            && this.bnt$latched.sides().length == nodes.size()
            && Arrays.equals(signature, this.bnt$engagedDisplacements)
            && (this.bnt$withinDwell(level) || this.bnt$latchedStillHolds(level, controllerPos, nodes))) {
            return this.bnt$latched;
        }

        this.bnt$engagedDisplacements = signature;
        this.bnt$repairAttempted = false;
        BntChainGeometry.Layout previous = this.bnt$latched != null ? this.bnt$latched : restored;
        this.bnt$latch(level, BntChainEngagement.layout(level, controllerPos, nodes, previous));
        if (BntDebugLog.enabled() && !bnt$sameLayout(previous, this.bnt$latched)) {
            BntDebugLog.LOG.info("{} chain {} relatched from {} to {}", BntDebugLog.side(level), controllerPos,
                BntDebugLog.layout(nodes, previous), BntDebugLog.layout(nodes, this.bnt$latched));
        }
        if (this.bnt$latched != null) {
            this.bnt$applySides(level, nodes, this.bnt$latched.sides());
        }
        return this.bnt$latched;
    }

    @Unique
    private static boolean bnt$sameLayout(BntChainGeometry.Layout first, BntChainGeometry.Layout second) {
        return first == second || first != null && second != null
            && Arrays.equals(first.sequence(), second.sequence()) && Arrays.equals(first.sides(), second.sides());
    }

    @Unique
    private void bnt$latch(Level level, BntChainGeometry.Layout layout) {
        this.bnt$latched = layout;
        this.bnt$latchedAt = level == null ? Long.MIN_VALUE : level.getGameTime();
        this.bnt$holdsCheckedAt = Long.MIN_VALUE;
    }

    /** Every input to the check is held for the tick, so it only has to run once per tick. */
    @Unique
    private boolean bnt$latchedStillHolds(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        long now = level.getGameTime();
        if (this.bnt$holdsCheckedAt != now) {
            this.bnt$holds = BntChainEngagement.stillHolds(level, controllerPos, nodes, this.bnt$latched);
            this.bnt$holdsCheckedAt = now;
        }
        return this.bnt$holds;
    }

    @Unique
    private boolean bnt$withinDwell(Level level) {
        return level != null
            && this.bnt$latchedAt != Long.MIN_VALUE
            && level.getGameTime() - this.bnt$latchedAt < BntChainEngagement.LATCH_DWELL_TICKS;
    }

    /**
     * A side is the direction Create drives that cogwheel in, so on the server it may only change while
     * the chain is stopped. The client has no network to damage and takes it straight away.
     */
    @Unique
    private void bnt$applySides(Level level, List<PathedCogwheelNode> nodes, int[] sides) {
        if (!bnt$sidesDiffer(nodes, sides)) {
            return;
        }

        if (level == null || level.isClientSide) {
            this.bnt$adoptSides(nodes, sides);
        } else {
            this.bnt$sidesPending = true;
        }
    }

    @Unique
    private static boolean bnt$sidesDiffer(List<PathedCogwheelNode> nodes, int[] sides) {
        if (sides.length != nodes.size()) {
            return false;
        }

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).side() != sides[i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<PathedCogwheelNode> bnt$latchedBeltOrder() {
        List<PathedCogwheelNode> nodes = this.cogwheelNodes;
        BntChainGeometry.Layout layout = this.bnt$latched;
        if (nodes == null || layout == null || layout.sides().length != nodes.size()) {
            return List.of();
        }
        return BntChainGeometry.applyLayout(nodes, layout);
    }

    @Override
    public boolean bnt$isNodeEngaged(Level level, BlockPos controllerPos, BlockPos nodeLocalPos) {
        List<PathedCogwheelNode> nodes = this.cogwheelNodes;
        if (level == null || controllerPos == null || nodes == null || nodes.size() < 2) {
            return true;
        }

        this.bnt$latchedLayout(level, controllerPos, nodes);
        nodes = this.cogwheelNodes;

        boolean[] engagement = this.bnt$appliedEngagement(nodes.size());
        if (engagement == null) {
            return true;
        }

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).localPos().equals(nodeLocalPos)) {
                return engagement[i];
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

        boolean[] applied = this.bnt$appliedEngagement(nodes.size());
        if (applied != null) {
            BntChainEngagement.parkDisengaged(level, controllerPos, nodes, applied);
        }

        BntChainGeometry.Layout layout = this.bnt$latchedLayout(level, controllerPos, nodes);
        nodes = this.cogwheelNodes;
        if (layout == null || layout.sides().length != nodes.size()) {
            return;
        }

        boolean[] engaged = BntChainEngagement.engagement(layout, nodes.size());
        boolean changed = this.bnt$sidesPending
            || applied == null
            || !Arrays.equals(applied, engaged)
            || BntChainEngagement.hasSeveredDrive(level, controllerPos, nodes, engaged)
            || BntChainEngagement.hasSelfDrive(level, controllerPos, nodes);
        if (!changed && BntChainEngagement.drivesTogether(level, controllerPos, nodes, engaged)) {
            this.bnt$repairAttempted = false;
            return;
        }

        if (this.bnt$repairAttempted) {
            return;
        }

        this.bnt$repairAttempted = true;
        Set<BlockPos> stopped = BntChainEngagement.detach(level, BntChainEngagement.positionsOf(controllerPos, nodes));
        this.bnt$sidesPending = false;
        this.bnt$adoptSides(nodes, layout.sides());
        this.bnt$appliedLayout = layout;
        BntChainEngagement.restore(level, stopped);
        if (BntDebugLog.enabled()) {
            BntDebugLog.LOG.info("server chain {} rebuilt its drive for {}", controllerPos, BntDebugLog.layout(nodes, layout));
        }
    }

    /** The engagement the kinetic network was actually built with, which is what a stale speed belongs to. */
    @Unique
    private boolean[] bnt$appliedEngagement(int count) {
        BntChainGeometry.Layout applied = this.bnt$appliedLayout;
        return applied == null || applied.sides().length != count
            ? null
            : BntChainEngagement.engagement(applied, count);
    }

    @Inject(
        method = {"read"},
        at = {@At("TAIL")}
    )
    private void bnt$forgetBuiltGeometry(CompoundTag tag, CallbackInfo ci) {
        this.bnt$builtDisplacements = null;
        this.bnt$latched = null;
        this.bnt$latchedAt = Long.MIN_VALUE;
        this.bnt$holdsCheckedAt = Long.MIN_VALUE;
        this.bnt$sidesPending = false;
        this.bnt$appliedLayout = null;
        this.bnt$engagedDisplacements = null;
        this.bnt$restoredLayout = bnt$readLayout(tag, this.cogwheelNodes.size());
    }

    /** Bits 'n' Bobs saves its small flanged cogwheel as tiny. */
    @Inject(
        method = {"checkIntegrity"},
        at = {@At("HEAD")}
    )
    private void bnt$adoptSmallFlangedSize(Level level, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        List<PathedCogwheelNode> nodes = this.cogwheelNodes;
        List<PathedCogwheelNode> updated = null;
        for (int i = 0; i < nodes.size(); i++) {
            PathedCogwheelNode node = nodes.get(i);
            BlockPos pos = node.localPos().offset(origin);
            if (node.isLarge() || node.hasSmallCogwheelOffset() || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!BnbBlockTags.SMALL_FLANGED_COGWHEEL.matches(state) && !state.is(BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())) {
                continue;
            }
            if (updated == null) {
                updated = new ArrayList<>(nodes);
            }
            updated.set(i, new PathedCogwheelNode(node.side(), false, node.rotationAxis(), node.localPos(), true));
        }
        if (updated == null) {
            return;
        }

        this.cogwheelNodes = updated;
        this.renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(updated);
        this.cachedSegments = null;
        this.bnt$builtDisplacements = null;
        if (!level.isClientSide && level.getBlockEntity(origin) instanceof SmartBlockEntity controller) {
            controller.setChanged();
            controller.sendData();
        }
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
        double[] signature = Arrays.copyOf(displacements, displacements.length + 2);
        signature[displacements.length] = BntBeltTension.at(level, controllerPos);
        BntPonderPhysics.Stage stage = BntPonderPhysics.stage(level);
        signature[displacements.length + 1] = stage == null ? 0.0 : stage.epoch();
        if (Arrays.equals(signature, this.bnt$builtDisplacements)) {
            return;
        }

        this.bnt$builtDisplacements = signature;
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
        this.bnt$holdsCheckedAt = Long.MIN_VALUE;
        this.updateInsideOutsideFlip();
    }
}
