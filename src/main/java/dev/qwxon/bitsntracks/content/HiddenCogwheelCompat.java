package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.kipti.bnb.registry.content.blocks.BnbKineticBlocks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HiddenCogwheelCompat {
    private static final ThreadLocal<Integer> bnt$chainSwapDepth = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Level> bnt$placementLevel = ThreadLocal.withInitial(() -> null);

    private HiddenCogwheelCompat() {
    }

    public static Level getPlacementLevel() {
        return bnt$placementLevel.get();
    }

    public static void setPlacementLevel(Level level) {
        if (level == null) {
            bnt$placementLevel.remove();
        } else {
            bnt$placementLevel.set(level);
        }
    }

    public static boolean isHiddenCogwheel(BlockState state) {
        return state.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL.get());
    }

    public static boolean isTinyHiddenCogwheel(BlockState state) {
        return state.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get());
    }

    public static boolean isHiddenChain(BlockState state) {
        return isHiddenCogwheel(state);
    }

    public static boolean isHiddenFlangedCogwheel(BlockState state) {
        return isHiddenCogwheel(state);
    }

    public static boolean isLargeHiddenCogwheel(BlockState state) {
        return state.is((Block)BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL.get());
    }

    public static boolean isMediumHiddenCogwheel(BlockState state) {
        return state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get());
    }

    public static BlockState toHiddenCogwheelState(BlockState oldState) {
        Block replacement;
        if (oldState.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
            || oldState.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())
            || oldState.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())
            || BuiltInRegistries.BLOCK.getKey(oldState.getBlock()).toString().equals("dndecor:medium_industrial_cogwheel")) {
            replacement = (Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get();
        } else if (oldState.is((Block)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get())
            || oldState.is((Block)BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get())
            || oldState.is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())
            || oldState.is((Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())
            || BuiltInRegistries.BLOCK.getKey(oldState.getBlock()).toString().equals("dndecor:industrial_cogwheel")) {
            replacement = (Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get();
        } else if (!oldState.is((Block)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get())
            && !oldState.is((Block)BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get())
            && !oldState.is((Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get())
            && !oldState.is((Block)BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL.get())
            && !BuiltInRegistries.BLOCK.getKey(oldState.getBlock()).toString().equals("dndecor:large_industrial_cogwheel")) {
            if (!oldState.is((Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get())
                && !oldState.is((Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get())
                && !oldState.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get())) {
                return null;
            }

            replacement = (Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get();
        } else {
            replacement = (Block)BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL.get();
        }

        return copyState(oldState, replacement);
    }

    public static BlockState toVisibleCogwheelState(BlockState oldState) {
        return toVisibleCogwheelState(oldState, null);
    }

    public static BlockState toVisibleCogwheelState(BlockState oldState, BlockEntity be) {
        Block replacement = null;
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            String originalBlockId = access.bnt$getOriginalBlock();
            if (originalBlockId != null) {
                replacement = (Block)BuiltInRegistries.BLOCK.get(ResourceLocation.parse(originalBlockId));
            }
        }

        if (replacement == null || replacement == Blocks.AIR) {
            if (!oldState.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())
                && !oldState.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
                && !oldState.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())) {
                if (!oldState.is((Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())
                    && !oldState.is((Block)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get())
                    && !oldState.is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())) {
                    if (!oldState.is((Block)BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL.get())
                        && !oldState.is((Block)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get())
                        && !oldState.is((Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get())) {
                        if (!oldState.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get())
                            && !oldState.is((Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get())
                            && !oldState.is((Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get())) {
                            return null;
                        }

                        replacement = isIndustrialBlockEntity(be)
                            ? (Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get()
                            : (Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get();
                    } else {
                        replacement = isIndustrialBlockEntity(be)
                            ? (Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get()
                            : (Block)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get();
                    }
                } else {
                    replacement = isIndustrialBlockEntity(be)
                        ? (Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get()
                        : (Block)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get();
                }
            } else {
                replacement = isIndustrialBlockEntity(be)
                    ? (Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get()
                    : (Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get();
            }
        }

        return copyState(oldState, replacement);
    }

    public static void transformControlledChain(BlockEntity be, StructureTransform transform) {
        if (be instanceof SmartBlockEntity smartBe) {
            CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE);
            if (behaviour != null) {
                CogwheelChain chain = behaviour.getControlledChain();
                if (chain != null) {
                    chain.transform(transform);
                }
            }
        }
    }

    public static BlockState toVisibleRenderState(BlockState oldState) {
        return toVisibleRenderState(oldState, null);
    }

    public static BlockState toVisibleRenderState(BlockState oldState, BlockEntity be) {
        if (isHiddenCogwheel(oldState)) {
            Block replacement = null;
            if (be instanceof KineticBlockEntityPhysicsAccess access) {
                String originalBlock = access.bnt$getOriginalBlock();
                if (originalBlock != null && !originalBlock.isEmpty()) {
                    ResourceLocation res = ResourceLocation.parse(originalBlock);
                    replacement = (Block)BuiltInRegistries.BLOCK.get(res);
                }
            }

            if (replacement == null || replacement == Blocks.AIR) {
                if (shouldUseCustomFlangedModel(oldState, be)) {
                    if (isMediumHiddenCogwheel(oldState)) {
                        replacement = isIndustrialBlockEntity(be)
                            ? (Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get()
                            : (Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get();
                    } else if (isLargeHiddenCogwheel(oldState)) {
                        replacement = isIndustrialBlockEntity(be)
                            ? (Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get()
                            : (Block)BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get();
                    } else if (isTinyHiddenCogwheel(oldState)) {
                        replacement = isIndustrialBlockEntity(be)
                            ? (Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get()
                            : (Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get();
                    } else {
                        replacement = isIndustrialBlockEntity(be)
                            ? (Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get()
                            : (Block)BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get();
                    }
                } else {
                    replacement = isLargeHiddenCogwheel(oldState)
                        ? (Block)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get()
                        : (
                            isTinyHiddenCogwheel(oldState)
                                ? (Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get()
                                : (Block)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get()
                        );
                }
            }

            return copyState(oldState, replacement);
        } else {
            return oldState;
        }
    }

    public static boolean shouldUseCustomFlangedModel(BlockState state, BlockEntity be) {
        if (state.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())) {
            return true;
        } else if (be instanceof KineticBlockEntityPhysicsAccess access) {
            String originalBlock = access.bnt$getOriginalBlock();
            return originalBlock != null && !originalBlock.isEmpty()
                ? isBitsNTracksId(originalBlock)
                : state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get());
        } else {
            return false;
        }
    }

    public static boolean isBitsNTracksId(String blockId) {
        return blockId != null && (blockId.startsWith("bits_n_tracks:") || blockId.startsWith("bitsntracks:"));
    }

    public static boolean isIndustrialBlockEntity(BlockEntity be) {
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            String originalBlock = access.bnt$getOriginalBlock();
            if (originalBlock != null) {
                return originalBlock.contains("industrial");
            }
        }

        return false;
    }

    public static boolean isPhysicsEnabled(BlockEntity be) {
        return be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled();
    }

    public static double getVisualDrop(BlockEntity be, float partialTick) {
        return be instanceof KineticBlockEntity kinetic && isPhysicsEnabled(be)
            ? BntPhysicsEvents.getClientRenderExtension(kinetic, partialTick)
            : 0.0;
    }

    public static double getVisualVerticalTranslation(BlockEntity be, float partialTick) {
        return be == null ? 0.0 : getManualVisualVerticalOffset(be) - getVisualDrop(be, partialTick);
    }

    public static Vec3 getModelTranslation(BlockEntity be, float partialTick) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access)) {
            return Vec3.ZERO;
        }

        double y = access.bnt$getAlignmentOffsetY();
        if (access.bnt$isPhysicsEnabled()) {
            y += getVisualVerticalTranslation(be, partialTick);
        }

        return new Vec3(access.bnt$getAlignmentOffsetX(), y, access.bnt$getAlignmentOffsetZ());
    }

    public static VoxelShape offsetShapeForSuspension(VoxelShape shape, BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof Level level) || !level.isClientSide) {
            return shape;
        }

        BlockEntity be = getter.getBlockEntity(pos);
        if (!isPhysicsEnabled(be)) {
            return shape;
        }

        Vec3 translation = getModelTranslation(be, BntClientCompat.getPartialTick());
        return translation.equals(Vec3.ZERO) ? shape : shape.move(translation.x, translation.y, translation.z);
    }

    public static boolean isSuppressingChainDestroy() {
        return bnt$chainSwapDepth.get() > 0;
    }

    public static void runWithChainDestroySuppressed(Runnable action) {
        int depth = bnt$chainSwapDepth.get();
        bnt$chainSwapDepth.set(depth + 1);

        try {
            action.run();
        } finally {
            if (depth == 0) {
                bnt$chainSwapDepth.remove();
            } else {
                bnt$chainSwapDepth.set(depth);
            }
        }
    }

    public static void replaceBlockForPhysicsSwap(Level level, BlockPos pos, BlockState newState) {
        Runnable swap = () -> level.setBlock(pos, newState, 3);
        runWithChainDestroySuppressed(swap);
    }

    public static void restoreBlockEntity(Level level, BlockPos pos, CompoundTag tag, boolean enablePhysics) {
        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe != null) {
            newBe.loadWithComponents(tag, level.registryAccess());
            if (newBe instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setPhysicsEnabled(enablePhysics);
            }

            newBe.setChanged();
            if (newBe instanceof KineticBlockEntity kinetic) {
                kinetic.sendData();
            }
        }
    }

    public static double getManualVisualVerticalOffset(BlockEntity be) {
        if (CogwheelSizeHelper.isLarge(be.getBlockState().getBlock())) {
            return BntPhysicsTuning.getLargeVisualVerticalOffset();
        } else if (CogwheelSizeHelper.isMedium(be.getBlockState().getBlock())) {
            return BntPhysicsTuning.getMediumVisualVerticalOffset();
        } else {
            return CogwheelSizeHelper.isTiny(be.getBlockState().getBlock())
                ? BntPhysicsTuning.getTinyVisualVerticalOffset()
                : BntPhysicsTuning.getSmallVisualVerticalOffset();
        }
    }

    private static BlockState copyState(BlockState oldState, Block replacement) {
        BlockState state = replacement.defaultBlockState();

        for (Property<?> property : oldState.getProperties()) {
            if (state.hasProperty(property)) {
                state = copyProperty(oldState, state, property);
            }
        }

        return state;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState oldState, BlockState newState, Property<T> property) {
        return (BlockState)newState.setValue(property, oldState.getValue(property));
    }

    public static Vec3 getTransformedPosition(BlockEntity controllerBe, Vec3 localPos, BlockPos relativePos) {
        return BntClientCompat.getTransformedPosition(controllerBe, localPos, relativePos);
    }

    public static List<ChainSegment> transformChainSegments(List<ChainSegment> segments, CogwheelChain chain, KineticBlockEntity be) {
        return BntClientCompat.transformChainSegments(segments, chain, be);
    }

    public static boolean hasPhysicsEnabledNode(CogwheelChainBehaviour behaviour) {
        if (behaviour == null) {
            return false;
        } else {
            BlockEntity selfBe = behaviour.blockEntity;
            if (selfBe != null && isPhysicsEnabled(selfBe)) {
                return true;
            } else {
                Level level = getActualLevel(selfBe);
                if (level == null) {
                    return false;
                } else {
                    try {
                        CogwheelChain chain = behaviour.getControlledChain();
                        if (chain != null) {
                            return bnt$checkChainPhysics(level, selfBe.getBlockPos(), chain);
                        }

                        Vec3i offset = behaviour.getControllerOffset();
                        if (offset != null) {
                            BlockPos controllerPos = selfBe.getBlockPos().offset(offset);
                            BlockEntity controllerBe = level.getBlockEntity(controllerPos);
                            if (controllerBe != null) {
                                if (isPhysicsEnabled(controllerBe)) {
                                    return true;
                                }

                                if (controllerBe instanceof SmartBlockEntity smartBe) {
                                    CogwheelChainBehaviour controllerChainBehaviour = (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE);
                                    if (controllerChainBehaviour != null) {
                                        CogwheelChain controllerChain = controllerChainBehaviour.getControlledChain();
                                        if (controllerChain != null) {
                                            return bnt$checkChainPhysics(level, controllerPos, controllerChain);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable var10) {
                    }

                    return false;
                }
            }
        }
    }

    public static boolean shouldForceDynamicRenderer(CogwheelChainBehaviour behaviour) {
        if (behaviour != null && behaviour.blockEntity != null) {
            BlockState state = behaviour.blockEntity.getBlockState();
            if (!state.is((Block)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get())
                && !state.is((Block)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get())
                && !state.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
                && !isHiddenCogwheel(state)) {
                Level level = behaviour.blockEntity.getLevel();
                if (level != null && level.isClientSide()) {
                    try {
                        Object subLevel = Sable.HELPER.getContainingClient(behaviour.blockEntity);
                        if (subLevel != null) {
                            return true;
                        }
                    } catch (Throwable var4) {
                    }
                }

                return hasPhysicsEnabledNode(behaviour);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean bnt$checkChainPhysics(Level level, BlockPos controllerPos, CogwheelChain chain) {
        if (level != null && chain != null) {
            for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
                BlockPos nodePos = controllerPos.offset(node.localPos());
                BlockEntity nodeBe = level.getBlockEntity(nodePos);
                if (nodeBe != null && isPhysicsEnabled(nodeBe)) {
                    return true;
                }

                BlockState nodeState = level.getBlockState(nodePos);
                if (isHiddenFlangedCogwheel(nodeState)
                    || nodeState.is((Block)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get())
                    || nodeState.is((Block)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get())
                    || nodeState.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static Level getActualLevel(BlockEntity be) {
        if (be == null) {
            return null;
        } else {
            Level level = be.getLevel();
            if (level == null) {
                return null;
            } else {
                if (level.isClientSide) {
                    try {
                        Object subLevel = Sable.HELPER.getContainingClient(be);
                        if (subLevel != null) {
                            return (Level)subLevel.getClass().getMethod("getLevel").invoke(subLevel);
                        }
                    } catch (Exception var3) {
                    }
                } else {
                    ServerSubLevel subLevel = (ServerSubLevel)Sable.HELPER.getContaining(be);
                    if (subLevel != null) {
                        return subLevel.getLevel();
                    }
                }

                return level;
            }
        }
    }
}
