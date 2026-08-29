package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainPathfinder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.ChainInteractionFailedException;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({PlacingCogwheelChain.class})
public abstract class PlacingCogwheelChainMixin {
    @Shadow(
        remap = false
    )
    public abstract List<PlacingCogwheelNode> getVisitedNodes();

    @Shadow(
        remap = false
    )
    public abstract PlacingCogwheelNode getLastNode();

    @Shadow(
        remap = false
    )
    public abstract int getSize();

    @Shadow(
        remap = false
    )
    public abstract boolean exceedsMaxBounds(PlacingCogwheelNode var1);

    @Shadow(
        remap = false
    )
    private static boolean isValidLargeCogAxisConnection(PlacingCogwheelNode previousNode, BlockPos pos, Axis axis, boolean isLarge) {
        throw new AssertionError();
    }

    @Overwrite(
        remap = false
    )
    public boolean tryAddNode(BlockPos pos, BlockState state, CogwheelChainType type) throws ChainInteractionFailedException {
        PlacingCogwheelNode previousNode = this.getLastNode();
        CogwheelChainCandidate candidate = CogwheelChainCandidate.getForBlock(state);
        if (candidate == null) {
            return false;
        } else {
            Level level = HiddenCogwheelCompat.getPlacementLevel();

            try {
                List<PlacingCogwheelNode> visitedNodes = this.getVisitedNodes();
                if (level != null && !visitedNodes.isEmpty()) {
                    BntRadiusProvider.setLevel(level);
                    BntRadiusProvider.setOrigin(visitedNodes.get(0).pos());
                }

                boolean isValidCogwheel = type.getCogwheelPredicate().test(state.getBlock());
                if (!isValidCogwheel && HiddenCogwheelCompat.isHiddenFlangedCogwheel(state)) {
                    BlockEntity be = level != null ? level.getBlockEntity(pos) : null;
                    BlockState visibleState = HiddenCogwheelCompat.toVisibleCogwheelState(state, be);
                    if (visibleState != null && type.getCogwheelPredicate().test(visibleState.getBlock())) {
                        isValidCogwheel = true;
                    }
                }

                if (!isValidCogwheel) {
                    throw new ChainInteractionFailedException("invalid_cogwheel_type." + type.getTranslationKey());
                } else {
                    for (int i = 1; i < this.getVisitedNodes().size(); i++) {
                        if (this.getVisitedNodes().get(i).pos().equals(pos)) {
                            throw new ChainInteractionFailedException("cannot_revisit_node");
                        }
                    }

                    Axis axis = candidate.axis();
                    boolean isLarge = candidate.isLarge();
                    boolean hasSmallOffset = candidate.hasSmallCogwheelOffset();
                    PlacingCogwheelNode nextNode = new PlacingCogwheelNode(pos, axis, isLarge, hasSmallOffset);
                    if (level != null) {
                        SubLevel subLevelA = Sable.HELPER.getContaining(level, previousNode.pos());
                        SubLevel subLevelB = Sable.HELPER.getContaining(level, pos);
                        if (subLevelA != subLevelB) {
                            Vec3 worldPosA = previousNode.pos().getCenter();
                            if (subLevelA != null) {
                                worldPosA = subLevelA.logicalPose().transformPosition(worldPosA);
                            }

                            Vec3 worldPosB = pos.getCenter();
                            if (subLevelB != null) {
                                worldPosB = subLevelB.logicalPose().transformPosition(worldPosB);
                            }

                            double distance = worldPosA.distanceTo(worldPosB);
                            if (distance > 32.0) {
                                throw new ChainInteractionFailedException("out_of_bounds");
                            }

                            this.getVisitedNodes().add(nextNode);
                            return true;
                        }
                    }

                    if (this.exceedsMaxBounds(nextNode)) {
                        throw new ChainInteractionFailedException("out_of_bounds");
                    } else {
                        int sameAxisOffset = Math.abs(pos.get(axis) - previousNode.pos().get(axis));
                        PlacingCogwheelNode prePreviousNode = this.getSize() >= 2 ? this.getVisitedNodes().get(this.getVisitedNodes().size() - 2) : null;
                        boolean sameAxisPlane = sameAxisOffset == 0;
                        boolean sameRotationAxis = axis == previousNode.rotationAxis();
                        double thisRadius;
                        if (CogwheelSizeHelper.isLarge(state.getBlock())) {
                            thisRadius = BntPhysicsTuning.getLargeTrackRadius();
                        } else if (CogwheelSizeHelper.isMedium(state.getBlock())) {
                            thisRadius = BntPhysicsTuning.getMediumTrackRadius();
                        } else if (CogwheelSizeHelper.isTiny(state.getBlock())) {
                            thisRadius = BntPhysicsTuning.getTinyTrackRadius();
                        } else {
                            thisRadius = BntPhysicsTuning.getSmallTrackRadius();
                        }

                        double prevRadius = previousNode.isLarge() ? BntPhysicsTuning.getLargeTrackRadius() : BntPhysicsTuning.getSmallTrackRadius();
                        if (level != null) {
                            BlockState prevState = level.getBlockState(previousNode.pos());
                            if (CogwheelSizeHelper.isLarge(prevState.getBlock())) {
                                prevRadius = BntPhysicsTuning.getLargeTrackRadius();
                            } else if (CogwheelSizeHelper.isMedium(prevState.getBlock())) {
                                prevRadius = BntPhysicsTuning.getMediumTrackRadius();
                            } else if (CogwheelSizeHelper.isTiny(prevState.getBlock())) {
                                prevRadius = BntPhysicsTuning.getTinyTrackRadius();
                            } else {
                                prevRadius = BntPhysicsTuning.getSmallTrackRadius();
                            }
                        }

                        double radiusSum = thisRadius + prevRadius;
                        boolean touching = sameAxisPlane && pos.distSqr(previousNode.pos()) <= radiusSum * radiusSum;
                        if (sameRotationAxis && sameAxisPlane && touching && bnt$isDirectNeighbor(pos, previousNode.pos())) {
                            boolean var45 = true;
                        } else {
                            boolean var10000 = false;
                        }

                        boolean validFlatConnection = sameRotationAxis && sameAxisPlane;
                        boolean validLargeConnection = isValidLargeCogAxisConnection(previousNode, pos, axis, isLarge);
                        boolean validConnection = validFlatConnection || validLargeConnection;
                        if (validConnection) {
                            List<Integer> validPathSteps = CogwheelChainPathfinder.getValidPathSteps(previousNode, nextNode);
                            if (validPathSteps.isEmpty()) {
                                throw new ChainInteractionFailedException("no_cogwheel_connection");
                            } else {
                                if (prePreviousNode != null) {
                                    boolean anyPathValid = false;

                                    for (Integer validPathStep : validPathSteps) {
                                        if (anyPathValid) {
                                            break;
                                        }

                                        anyPathValid = CogwheelChainPathfinder.isValidPathStep(prePreviousNode, 1, previousNode, validPathStep)
                                            || CogwheelChainPathfinder.isValidPathStep(prePreviousNode, -1, previousNode, validPathStep);
                                    }

                                    if (!anyPathValid) {
                                        throw new ChainInteractionFailedException("no_path_to_cogwheel");
                                    }
                                }

                                this.getVisitedNodes().add(nextNode);
                                return true;
                            }
                        } else if (!sameRotationAxis) {
                            throw new ChainInteractionFailedException("not_valid_axis_change");
                        } else {
                            throw new ChainInteractionFailedException("not_flat_connection");
                        }
                    }
                }
            } finally {
                BntRadiusProvider.clearLevel();
            }
        }
    }

    private static boolean bnt$isDirectNeighbor(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return dx + dy + dz == 1;
    }

    @ModifyVariable(
        method = {"validateConnection"},
        at = @At("STORE"),
        ordinal = 2,
        remap = false
    )
    private static boolean bnt$modifyIsAdjacent(boolean original) {
        return false;
    }
}
