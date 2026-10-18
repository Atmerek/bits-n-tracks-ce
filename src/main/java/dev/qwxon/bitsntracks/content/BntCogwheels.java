package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BntCogwheels {
    private static final ThreadLocal<Integer> CHAIN_SWAP_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Level> PLACEMENT_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> RESTING = ThreadLocal.withInitial(() -> false);

    private BntCogwheels() {
    }

    public static Level getPlacementLevel() {
        return PLACEMENT_LEVEL.get();
    }

    public static void setPlacementLevel(Level level) {
        if (level == null) {
            PLACEMENT_LEVEL.remove();
        } else {
            PLACEMENT_LEVEL.set(level);
        }
    }

    public static boolean isOwnCogwheel(BlockState state) {
        return state.getBlock() instanceof BntFlangedCogwheelBlock;
    }

    public static boolean isOwnCogwheel(BlockGetter level, BlockPos pos) {
        return level != null && isOwnCogwheel(level.getBlockState(pos));
    }

    public static boolean isOwnOrLegacyCogwheel(Block block) {
        return block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock;
    }

    public static boolean isOwnChain(Level level, BlockPos controllerPos) {
        CogwheelChain chain = level == null ? null : BntChains.chainAt(level, controllerPos);
        if (chain == null) {
            return false;
        }
        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
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

    public static boolean isPhysicsEnabled(BlockEntity be) {
        return be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled();
    }

    public static double getVisualDrop(BlockEntity be, float partialTick) {
        return be instanceof KineticBlockEntity kinetic && isPhysicsEnabled(be)
            ? BntPhysicsEvents.getRawRenderExtension(kinetic, partialTick)
            : 0.0;
    }

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
        return BntFlangedCogwheelBlock.physics(be.getBlockState())
            ? translation.add(BntSuspension.displacement(be, getHeldVisualDrop(be, partialTick)))
            : translation;
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
        return CHAIN_SWAP_DEPTH.get() > 0;
    }

    public static void replaceBlockForPhysicsSwap(Level level, BlockPos pos, BlockState newState) {
        int depth = CHAIN_SWAP_DEPTH.get();
        CHAIN_SWAP_DEPTH.set(depth + 1);
        try {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        } finally {
            if (depth == 0) {
                CHAIN_SWAP_DEPTH.remove();
            } else {
                CHAIN_SWAP_DEPTH.set(depth);
            }
        }
    }

    public static boolean shouldForceDynamicRenderer(CogwheelChainBehaviour behaviour) {
        BlockEntity be = behaviour == null ? null : behaviour.getBlockEntity();
        if (be == null) {
            return false;
        }
        if (BntVisualized.ownType(be)) {
            return true;
        }
        Level level = be.getLevel();
        if (level != null && level.isClientSide() && Sable.HELPER.getContainingClient(be) != null) {
            return true;
        }
        return hasPhysicsEnabledNode(behaviour, level);
    }

    private static boolean hasPhysicsEnabledNode(CogwheelChainBehaviour behaviour, Level level) {
        BlockPos controllerPos = BntChains.controllerPos(behaviour);
        CogwheelChain chain = BntChains.chainOf(behaviour);
        if (level == null || controllerPos == null || chain == null) {
            return false;
        }
        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
            if (BntFlangedCogwheelBlock.physics(level.getBlockState(controllerPos.offset(node.localPos())))) {
                return true;
            }
        }
        return false;
    }
}
