package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainPathfinder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.ChainInteractionFailedException;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.llamalad7.mixinextras.sugar.Local;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlacingCogwheelChain.class, remap = false)
public abstract class PlacingCogwheelChainMixin {
    @Unique
    private static final double CROSS_STRUCTURE_REACH = 32.0;

    @Shadow
    public abstract List<PlacingCogwheelNode> getVisitedNodes();

    @Shadow
    public abstract PlacingCogwheelNode getLastNode();

    @Shadow
    public abstract int getSize();

    @Shadow
    public abstract boolean exceedsMaxBounds(PlacingCogwheelNode node);

    @Shadow
    private static boolean isValidLargeCogAxisConnection(PlacingCogwheelNode previousNode, BlockPos pos, Axis axis, boolean isLarge) {
        throw new AssertionError();
    }

    @Inject(method = "tryAddNode", at = @At("HEAD"), cancellable = true)
    private void bnt$addOwnNode(BlockPos pos, BlockState state, CogwheelChainType type, CallbackInfoReturnable<Boolean> cir)
        throws ChainInteractionFailedException {
        if (BntChainGeometry.ownGeometry() || BntCogwheelChainTypes.isOwn(type) || BntCogwheels.isOwnCogwheel(state)) {
            boolean previous = BntChainGeometry.beginOwn(true);
            try {
                cir.setReturnValue(this.bnt$tryAddNode(pos, state, type));
            } finally {
                BntChainGeometry.endOwn(previous);
            }
        }
    }

    @Unique
    private boolean bnt$tryAddNode(BlockPos pos, BlockState state, CogwheelChainType type) throws ChainInteractionFailedException {
        CogwheelChainCandidate candidate = CogwheelChainCandidate.getForBlock(state);
        if (candidate == null) {
            return false;
        }
        Level level = BntCogwheels.getPlacementLevel();
        List<PlacingCogwheelNode> visited = this.getVisitedNodes();
        try (BntRadiusProvider.Scope ignored = level != null && !visited.isEmpty()
            ? BntRadiusProvider.at(level, visited.get(0).pos())
            : BntRadiusProvider.keep()) {
            if (!type.getCogwheelPredicate().test(state.getBlock())) {
                throw new ChainInteractionFailedException("invalid_cogwheel_type." + type.getTranslationKey());
            }
            for (int i = 1; i < visited.size(); i++) {
                if (visited.get(i).pos().equals(pos)) {
                    throw new ChainInteractionFailedException("cannot_revisit_node");
                }
            }

            PlacingCogwheelNode previousNode = this.getLastNode();
            Axis axis = candidate.axis();
            PlacingCogwheelNode nextNode = new PlacingCogwheelNode(pos, axis, candidate.isLarge(), candidate.hasSmallCogwheelOffset());
            if (level != null && bnt$acrossStructures(level, previousNode.pos(), pos)) {
                visited.add(nextNode);
                return true;
            }
            if (this.exceedsMaxBounds(nextNode)) {
                throw new ChainInteractionFailedException("out_of_bounds");
            }

            boolean sameRotationAxis = axis == previousNode.rotationAxis();
            boolean flat = sameRotationAxis && pos.get(axis) == previousNode.pos().get(axis);
            if (!flat && !isValidLargeCogAxisConnection(previousNode, pos, axis, candidate.isLarge())) {
                throw new ChainInteractionFailedException(sameRotationAxis ? "not_flat_connection" : "not_valid_axis_change");
            }

            List<Integer> steps = CogwheelChainPathfinder.getValidPathSteps(previousNode, nextNode);
            if (steps.isEmpty()) {
                throw new ChainInteractionFailedException("no_cogwheel_connection");
            }
            if (this.getSize() >= 2 && !bnt$anyStepContinues(visited.get(visited.size() - 2), previousNode, steps)) {
                throw new ChainInteractionFailedException("no_path_to_cogwheel");
            }
            visited.add(nextNode);
            return true;
        }
    }

    @Unique
    private static boolean bnt$acrossStructures(Level level, BlockPos from, BlockPos to) throws ChainInteractionFailedException {
        SubLevel fromSubLevel = Sable.HELPER.getContaining(level, from);
        SubLevel toSubLevel = Sable.HELPER.getContaining(level, to);
        if (fromSubLevel == toSubLevel) {
            return false;
        }
        Vec3 fromWorld = fromSubLevel == null ? from.getCenter() : fromSubLevel.logicalPose().transformPosition(from.getCenter());
        Vec3 toWorld = toSubLevel == null ? to.getCenter() : toSubLevel.logicalPose().transformPosition(to.getCenter());
        if (fromWorld.distanceTo(toWorld) > CROSS_STRUCTURE_REACH) {
            throw new ChainInteractionFailedException("out_of_bounds");
        }
        return true;
    }

    @Unique
    private static boolean bnt$anyStepContinues(PlacingCogwheelNode prePrevious, PlacingCogwheelNode previous, List<Integer> steps) {
        for (int step : steps) {
            if (CogwheelChainPathfinder.isValidPathStep(prePrevious, 1, previous, step)
                || CogwheelChainPathfinder.isValidPathStep(prePrevious, -1, previous, step)) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "checkMissingNodesInLevel", at = @At("HEAD"), cancellable = true)
    private void bnt$refuseNodes(Level level, CogwheelChainType type, CallbackInfoReturnable<Boolean> cir) {
        for (PlacingCogwheelNode node : this.getVisitedNodes()) {
            if (BntCogwheelChainTypes.refusal(type, level, node.pos(), level.getBlockState(node.pos())) != null) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @ModifyVariable(method = "validateConnection", at = @At("STORE"), ordinal = 2)
    private static boolean bnt$modifyIsAdjacent(boolean original, @Local(argsOnly = true) CogwheelChainType type) {
        return original && !BntChainGeometry.ownGeometry() && !BntCogwheelChainTypes.isOwn(type);
    }
}
