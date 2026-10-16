package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HiddenCogwheelCompat {
    private static final ThreadLocal<Integer> bnt$chainSwapDepth = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Level> bnt$placementLevel = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<Boolean> RESTING = ThreadLocal.withInitial(() -> false);

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

    public static boolean isOwnCogwheel(BlockState state) {
        return state.getBlock() instanceof BntFlangedCogwheelBlock;
    }

    public static boolean isOwnCogwheel(BlockGetter level, BlockPos pos) {
        return level != null && isOwnCogwheel(level.getBlockState(pos));
    }

    public static boolean isOwnChain(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null
            || !(level.getBlockEntity(controllerPos) instanceof SmartBlockEntity smart)
            || !(smart.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour)
            || behaviour.getControlledChain() == null) {
            return false;
        }
        for (PathedCogwheelNode node : behaviour.getControlledChain().getChainPathCogwheelNodes()) {
            BlockPos pos = controllerPos.offset(node.localPos());
            if (level.isLoaded(pos) && !isOwnCogwheel(level, pos)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isIndustrial(BlockState state) {
        return state.getBlock() instanceof BntFlangedCogwheelBlock cogwheel && cogwheel.material() == CogwheelMaterial.INDUSTRIAL;
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

    public static boolean isPhysicsEnabled(BlockEntity be) {
        return be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled();
    }

    public static double getVisualDrop(BlockEntity be, float partialTick) {
        return be instanceof KineticBlockEntity kinetic && isPhysicsEnabled(be)
            ? BntPhysicsEvents.getClientRenderExtension(kinetic, partialTick)
            : 0.0;
    }

    /** Drop a wheel is drawn at, with the belt holding it up. */
    public static double getHeldVisualDrop(BlockEntity be, float partialTick) {
        if (!(be instanceof KineticBlockEntity kinetic) || !isPhysicsEnabled(be)) {
            return 0.0;
        }
        if (RESTING.get()) {
            BntSuspension.Arm arm = BntSuspension.arm(be);
            return arm == null ? 0.0 : -arm.ride();
        }
        return BntPhysicsEvents.getHeldRenderExtension(kinetic, partialTick);
    }

    public static void drawResting(boolean resting) {
        RESTING.set(resting);
    }

    public static Vec3 getModelTranslation(BlockEntity be, float partialTick) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access)) {
            return Vec3.ZERO;
        }

        Vec3 translation = new Vec3(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        if (BntFlangedCogwheelBlock.physics(be.getBlockState())) {
            translation = translation.add(BntSuspension.displacement(be, getHeldVisualDrop(be, partialTick)));
        }

        return translation;
    }

    public static VoxelShape offsetShapeForSuspension(VoxelShape shape, BlockGetter getter, BlockPos pos) {
        return getter instanceof Level ? offsetShapeForSuspension(shape, getter.getBlockEntity(pos)) : shape;
    }

    public static VoxelShape offsetShapeForSuspension(VoxelShape shape, BlockEntity be) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide) {
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

                    return false;
                }
            }
        }
    }

    public static boolean shouldForceDynamicRenderer(CogwheelChainBehaviour behaviour) {
        if (behaviour == null || behaviour.blockEntity == null) {
            return false;
        }
        if (BntVisualized.ownType(behaviour.blockEntity)) {
            return true;
        }

        Level level = behaviour.blockEntity.getLevel();
        if (level != null && level.isClientSide() && Sable.HELPER.getContainingClient(behaviour.blockEntity) != null) {
            return true;
        }
        return hasPhysicsEnabledNode(behaviour);
    }

    private static boolean bnt$checkChainPhysics(Level level, BlockPos controllerPos, CogwheelChain chain) {
        if (level != null && chain != null) {
            for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
                BlockPos nodePos = controllerPos.offset(node.localPos());
                BlockEntity nodeBe = level.getBlockEntity(nodePos);
                if (nodeBe != null && isPhysicsEnabled(nodeBe)) {
                    return true;
                }

                if (BntFlangedCogwheelBlock.physics(level.getBlockState(nodePos))) {
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
                    Object subLevel = Sable.HELPER.getContainingClient(be);
                    if (subLevel instanceof SubLevel containing) {
                        return containing.getLevel();
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
