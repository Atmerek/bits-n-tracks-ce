package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.registry.content.blocks.BnbKineticBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BntLegacyCogwheels {
    private BntLegacyCogwheels() {
    }

    public static boolean convert(KineticBlockEntity be, @Nullable String originalId, boolean legacyPhysics) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof BntFlangedCogwheelBlock) {
            if (legacyPhysics) {
                BntFlangedCogwheelBlock.setPhysics(level, pos, true);
            }
            return false;
        }
        if (!(state.getBlock() instanceof HiddenCogwheelBlock) && !legacyPhysics) {
            return false;
        }
        Block target = successor(state.getBlock(), originalId);
        if (target == null) {
            return false;
        }

        BlockState next = target.defaultBlockState()
            .setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS))
            .setValue(BntCogwheelPairing.WIDE, BntCogwheelPairing.sideOf(state))
            .setValue(BntFlangedCogwheelBlock.PHYSICS, true);
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        HiddenCogwheelCompat.replaceBlockForPhysicsSwap(level, pos, next);
        BlockEntity fresh = level.getBlockEntity(pos);
        if (fresh != null) {
            fresh.loadWithComponents(tag, level.registryAccess());
            fresh.setChanged();
            if (fresh instanceof KineticBlockEntity kinetic) {
                kinetic.sendData();
            }
        }
        BntChainEngagement.rebuild(level, pos);
        return true;
    }

    @Nullable
    private static Block successor(Block block, @Nullable String originalId) {
        if (block instanceof HiddenCogwheelBlock hidden) {
            ResourceLocation id = originalId == null ? null : ResourceLocation.tryParse(originalId);
            Block original = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            return original instanceof BntFlangedCogwheelBlock ? original : wooden(hidden.size());
        }
        if (block == BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get()) {
            return wooden(CogwheelSize.SMALL);
        }
        if (block == BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get()) {
            return wooden(CogwheelSize.LARGE);
        }
        return null;
    }

    private static Block wooden(CogwheelSize size) {
        return switch (size) {
            case TINY -> BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get();
            case INTERMEDIATE -> BitsNTracksBlocks.INTERMEDIATE_INDUSTRIAL_FLANGED_COGWHEEL.get();
            case SMALL -> BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get();
            case MEDIUM -> BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get();
            case LARGE -> BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get();
        };
    }
}
