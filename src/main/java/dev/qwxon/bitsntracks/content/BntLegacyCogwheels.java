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
    private static final String PHYSICS = "BntPhysicsEnabled";
    private static final String ORIGINAL = "BntOriginalBlock";

    private BntLegacyCogwheels() {
    }

    public static boolean isLegacy(KineticBlockEntity be, @Nullable CompoundTag tag) {
        return be.getBlockState().getBlock() instanceof HiddenCogwheelBlock
            || tag != null && (tag.getBoolean(PHYSICS) || tag.contains(ORIGINAL));
    }

    public static void convert(KineticBlockEntity be, @Nullable CompoundTag legacy) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        boolean legacyPhysics = legacy != null && legacy.getBoolean(PHYSICS);
        if (state.getBlock() instanceof BntFlangedCogwheelBlock) {
            if (legacyPhysics) {
                BntFlangedCogwheelBlock.setPhysics(level, pos, true);
            }
            return;
        }
        if (!(state.getBlock() instanceof HiddenCogwheelBlock) && !legacyPhysics) {
            return;
        }
        Block target = successor(state.getBlock(), legacy != null && legacy.contains(ORIGINAL) ? legacy.getString(ORIGINAL) : null);
        if (target == null) {
            return;
        }

        BlockState next = target.defaultBlockState()
            .setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS))
            .setValue(BntCogwheelPairing.WIDE, BntCogwheelPairing.sideOf(state))
            .setValue(BntFlangedCogwheelBlock.PHYSICS, true);
        CompoundTag tag = (legacy == null ? new CompoundTag() : legacy.copy()).merge(be.saveWithoutMetadata(level.registryAccess()));
        BntCogwheels.replaceBlockForPhysicsSwap(level, pos, next);
        BlockEntity fresh = level.getBlockEntity(pos);
        if (fresh != null) {
            fresh.loadWithComponents(tag, level.registryAccess());
            fresh.setChanged();
            if (fresh instanceof KineticBlockEntity kinetic) {
                kinetic.sendData();
            }
        }
        BntChainEngagement.rebuild(level, pos);
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
            case INTERMEDIATE -> BitsNTracksBlocks.INTERMEDIATE_FLANGED_COGWHEEL.get();
            case SMALL -> BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get();
            case MEDIUM -> BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get();
            case MEDIUM_LARGE -> BitsNTracksBlocks.MEDIUM_LARGE_FLANGED_COGWHEEL.get();
            case LARGE -> BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get();
        };
    }
}
