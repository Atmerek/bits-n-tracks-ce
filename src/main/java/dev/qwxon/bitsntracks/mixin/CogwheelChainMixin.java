package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.registry.core.BnbTags.BnbBlockTags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltDrape;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainMotion;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import dev.qwxon.bitsntracks.physics.BntPonderPhysics;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
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
    private CogwheelChainType type;

    @Shadow
    private void updateInsideOutsideFlip() {
        throw new AssertionError();
    }

    @Unique
    private boolean bnt$own;

    @Override
    public boolean bnt$isOwn() {
        return this.bnt$own || BntCogwheelChainTypes.isOwn(this.type);
    }

    @Unique
    private boolean bnt$claim(Level level, BlockPos controllerPos) {
        if (!this.bnt$own && (BntCogwheelChainTypes.isOwn(this.type) || HiddenCogwheelCompat.isOwnChain(level, controllerPos))) {
            this.bnt$own = true;
            this.bnt$builtDisplacements = null;
        }
        return this.bnt$own;
    }

    @Unique
    private List<RenderedChainPathNode> bnt$build(List<PathedCogwheelNode> nodes, boolean own, Operation<List<RenderedChainPathNode>> original) {
        boolean previous = BntChainGeometry.beginOwn(own);
        try {
            return original.call(nodes);
        } finally {
            BntChainGeometry.endOwn(previous);
        }
    }

    @WrapOperation(
        method = {"<init>(Ljava/util/List;Lcom/kipti/bnb/content/kinetics/cogwheel_chain/types/CogwheelChainType;Lnet/minecraft/world/item/Item;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChainGeometryBuilder;buildFullChainFromPathNodes(Ljava/util/List;)Ljava/util/List;"
        )
    )
    private List<RenderedChainPathNode> bnt$buildNewChain(List<PathedCogwheelNode> nodes, Operation<List<RenderedChainPathNode>> original,
                                                          @Local(argsOnly = true) CogwheelChainType chainType) {
        this.bnt$own = BntChainGeometry.ownGeometry() || BntCogwheelChainTypes.isOwn(chainType);
        return this.bnt$build(nodes, this.bnt$own, original);
    }

    @WrapOperation(
        method = {"read", "transform"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChainGeometryBuilder;buildFullChainFromPathNodes(Ljava/util/List;)Ljava/util/List;"
        )
    )
    private List<RenderedChainPathNode> bnt$rebuildChain(List<PathedCogwheelNode> nodes, Operation<List<RenderedChainPathNode>> original) {
        return this.bnt$build(nodes, this.bnt$isOwn(), original);
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
    private List<PathedCogwheelNode> bnt$preReadNodes = List.of();

    @Unique
    private BntChainGeometry.Layout bnt$preReadLatch;

    @Unique
    private List<RenderedChainPathNode> bnt$preReadRendered = List.of();

    @Unique
    private BntChainGeometry.Layout bnt$latchedLayout(Level level, BlockPos controllerPos, List<PathedCogwheelNode> nodes) {
        double[] signature = BntChainEngagement.signature(level, controllerPos, nodes);
        BntChainGeometry.Layout restored = this.bnt$restoredLayout;
        if (restored != null) {
            this.bnt$restoredLayout = null;
            if (BntChainEngagement.tangled(level, controllerPos, nodes, restored)) {
                restored = null;
            } else if (BntChainEngagement.stillHolds(level, controllerPos, nodes, restored)
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
        if (!BntChainEngagement.layoutFits(nodes, previous)) {
            previous = null;
        }
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
        if (level == null || controllerPos == null || nodes == null || nodes.size() < 2 || !this.bnt$claim(level, controllerPos)) {
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
        boolean claimed = this.bnt$own;
        if (!this.bnt$claim(level, controllerPos)) {
            return;
        }
        if (!claimed) {
            this.bnt$refreshWithLevel(level, controllerPos);
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
            || !BntChainEngagement.beltFits(nodes)
            || BntChainEngagement.hasSeveredDrive(level, controllerPos, nodes, engaged)
            || BntChainEngagement.hasSelfDrive(level, controllerPos, nodes);
        if (!changed && BntChainEngagement.drivesTogether(level, controllerPos, nodes, engaged, layout.sides())) {
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
        at = {@At("HEAD")}
    )
    private void bnt$rememberLatchBeforeRead(CompoundTag tag, CallbackInfo ci) {
        this.bnt$own = tag.getBoolean("BNT_Own");
        this.bnt$preReadNodes = this.cogwheelNodes == null ? List.of() : List.copyOf(this.cogwheelNodes);
        this.bnt$preReadLatch = this.bnt$latched;
        this.bnt$preReadRendered = this.renderedNodes == null ? List.of() : List.copyOf(this.renderedNodes);
    }

    @Inject(
        method = {"read"},
        at = {@At("TAIL")}
    )
    private void bnt$forgetBuiltGeometry(CompoundTag tag, CallbackInfo ci) {
        BntChainGeometry.Layout kept = this.bnt$preReadLatch != null && bnt$sameCogwheels(this.bnt$preReadNodes, this.cogwheelNodes)
            ? this.bnt$preReadLatch
            : null;
        if (kept != null && !this.bnt$preReadRendered.isEmpty()) {
            this.renderedNodes.clear();
            this.renderedNodes.addAll(this.bnt$preReadRendered);
        }
        this.bnt$preReadNodes = List.of();
        this.bnt$preReadLatch = null;
        this.bnt$preReadRendered = List.of();
        this.bnt$builtDisplacements = null;
        this.bnt$latched = null;
        this.bnt$latchedAt = Long.MIN_VALUE;
        this.bnt$holdsCheckedAt = Long.MIN_VALUE;
        this.bnt$sidesPending = false;
        this.bnt$appliedLayout = null;
        this.bnt$engagedDisplacements = null;
        this.bnt$restoredLayout = kept != null ? kept : bnt$readLayout(tag, this.cogwheelNodes.size());
    }

    @Unique
    private static boolean bnt$sameCogwheels(List<PathedCogwheelNode> before, List<PathedCogwheelNode> after) {
        if (before.size() != after.size()) {
            return false;
        }
        for (int i = 0; i < before.size(); i++) {
            PathedCogwheelNode a = before.get(i);
            PathedCogwheelNode b = after.get(i);
            if (!a.localPos().equals(b.localPos()) || a.isLarge() != b.isLarge() || a.rotationAxis() != b.rotationAxis()) {
                return false;
            }
        }
        return true;
    }

    /** Bits 'n' Bobs saves its small flanged cogwheel as tiny. */
    @Inject(
        method = {"checkIntegrity"},
        at = {@At("HEAD")}
    )
    private void bnt$adoptSmallFlangedSize(Level level, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        if (!this.bnt$claim(level, origin)) {
            return;
        }
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
        boolean previous = BntChainGeometry.beginOwn(true);
        try {
            this.renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(updated);
        } finally {
            BntChainGeometry.endOwn(previous);
        }
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

    @Inject(
        method = {"write"},
        at = {@At("TAIL")}
    )
    private void bnt$writeOwn(CompoundTag tag, CallbackInfo ci) {
        if (this.bnt$own) {
            tag.putBoolean("BNT_Own", true);
        }
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
        if (nodes == null || nodes.size() < 2 || !this.bnt$claim(level, controllerPos)) {
            return;
        }

        double[] displacements = BntChainMotion.displacementSignature(nodes);
        double[] surface = BntBeltDrape.surfaceKey(level, controllerPos);
        double[] signature = Arrays.copyOf(displacements, displacements.length + 2 + surface.length);
        signature[displacements.length] = BntBeltTension.at(level, controllerPos);
        BntPonderPhysics.Stage stage = BntPonderPhysics.stage(level);
        signature[displacements.length + 1] = stage == null ? 0.0 : stage.epoch();
        System.arraycopy(surface, 0, signature, displacements.length + 2, surface.length);
        if (Arrays.equals(signature, this.bnt$builtDisplacements)) {
            return;
        }

        this.bnt$builtDisplacements = signature;
        BntChainGeometry.Layout layout = this.bnt$latchedLayout(level, controllerPos, nodes);
        BntChainGeometry.Layout previous = BntChainMotion.swapLayout(layout);
        boolean wasOwn = BntChainGeometry.beginOwn(true);

        try {
            this.renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(this.cogwheelNodes);
        } finally {
            BntChainGeometry.endOwn(wasOwn);
            BntChainMotion.swapLayout(previous);
        }

        this.cachedSegments = null;
    }

    @Unique
    private void bnt$refreshWithLevel(Level level, BlockPos controllerPos) {
        Level outerLevel = BntRadiusProvider.level();
        BlockPos outerOrigin = BntRadiusProvider.origin();
        BntRadiusProvider.setLevel(level);
        BntRadiusProvider.setOrigin(controllerPos);
        try {
            this.bnt$refreshChainGeometry(level, controllerPos);
        } finally {
            BntRadiusProvider.setLevel(outerLevel);
            BntRadiusProvider.setOrigin(outerOrigin);
        }
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
