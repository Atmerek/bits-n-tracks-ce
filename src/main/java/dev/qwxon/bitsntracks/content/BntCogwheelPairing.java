package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;

public final class BntCogwheelPairing {
    public static final EnumProperty<BntWideSide> WIDE = EnumProperty.create("wide", BntWideSide.class);

    private BntCogwheelPairing() {
    }

    public static boolean supports(BlockState state) {
        return state.hasProperty(WIDE) && state.hasProperty(RotatedPillarKineticBlock.AXIS);
    }

    public static BntWideSide sideOf(BlockState state) {
        return supports(state) ? (BntWideSide)state.getValue(WIDE) : BntWideSide.NONE;
    }

    public static boolean isWide(BlockState state) {
        return sideOf(state) != BntWideSide.NONE;
    }

    public static Direction partnerDirection(BlockState state) {
        BntWideSide side = sideOf(state);
        return side == BntWideSide.NONE ? null : side.toDirection((Axis)state.getValue(RotatedPillarKineticBlock.AXIS));
    }

    public static BlockPos partnerPos(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Direction direction = partnerDirection(state);
        if (direction == null) {
            return null;
        }

        BlockPos partnerPos = pos.relative(direction);
        BlockState partner = level.getBlockState(partnerPos);
        return canPair(state, partner) && sideOf(partner) == BntWideSide.of(direction.getOpposite()) ? partnerPos : null;
    }

    public static void copySettings(KineticBlockEntityPhysicsAccess from, KineticBlockEntityPhysicsAccess to) {
        to.bnt$setAlignmentOffsetX(from.bnt$getAlignmentOffsetX());
        to.bnt$setAlignmentOffsetY(from.bnt$getAlignmentOffsetY());
        to.bnt$setAlignmentOffsetZ(from.bnt$getAlignmentOffsetZ());
        to.bnt$setHiddenByLever(from.bnt$isHiddenByLever());
    }

    public static void adoptPartnerSettings(Level level, BlockPos pos) {
        BlockPos partnerPos = partnerPos(level, pos);
        if (partnerPos == null || level.isClientSide) {
            return;
        }

        BlockEntity selfBe = level.getBlockEntity(pos);
        BlockEntity partnerBe = level.getBlockEntity(partnerPos);
        if (!(selfBe instanceof KineticBlockEntityPhysicsAccess self) || !(partnerBe instanceof KineticBlockEntityPhysicsAccess partner)) {
            return;
        }

        copySettings(partner, self);
        selfBe.setChanged();
        if (selfBe instanceof KineticBlockEntity kinetic) {
            kinetic.sendData();
        }

        if (partner.bnt$isPhysicsEnabled() && !self.bnt$isPhysicsEnabled()) {
            self.bnt$setPhysicsEnabled(true);
        }
    }

    public static void pushSettingsToPartner(Level level, BlockPos pos) {
        BlockPos partnerPos = partnerPos(level, pos);
        if (partnerPos == null) {
            return;
        }

        BlockEntity selfBe = level.getBlockEntity(pos);
        BlockEntity partnerBe = level.getBlockEntity(partnerPos);
        if (selfBe instanceof KineticBlockEntityPhysicsAccess self && partnerBe instanceof KineticBlockEntityPhysicsAccess partner) {
            copySettings(self, partner);
            partnerBe.setChanged();
            if (partnerBe instanceof KineticBlockEntity kinetic) {
                kinetic.sendData();
            }
        }
    }

    public static Vec3 seamOffset(BlockState state) {
        Direction direction = partnerDirection(state);
        return direction == null
            ? Vec3.ZERO
            : new Vec3(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5);
    }

    public static BlockState pairOnPlacement(BlockState state, BlockPlaceContext context) {
        if (state == null || !supports(state) || context.isSecondaryUseActive()) {
            return state;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Axis axis = (Axis)state.getValue(RotatedPillarKineticBlock.AXIS);

        for (AxisDirection axisDirection : AxisDirection.values()) {
            Direction direction = Direction.fromAxisAndDirection(axis, axisDirection);
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (canJoin(level, pos, state, neighbourPos, neighbour) && sideOf(neighbour) == BntWideSide.NONE) {
                return (BlockState)state.setValue(WIDE, BntWideSide.of(direction));
            }
        }

        return state;
    }

    public static void linkPartner(Level level, BlockPos pos, BlockState state) {
        Direction direction = partnerDirection(state);
        if (direction == null) {
            return;
        }

        BlockPos partnerPos = pos.relative(direction);
        BlockState partner = level.getBlockState(partnerPos);
        if (!canJoin(level, pos, state, partnerPos, partner)) {
            return;
        }

        BntWideSide expected = BntWideSide.of(direction.getOpposite());
        if (sideOf(partner) != expected) {
            level.setBlock(partnerPos, (BlockState)partner.setValue(WIDE, expected), 3);
        }
    }

    public static BlockState unlinkBrokenPair(BlockState state, Direction direction, BlockState neighbour) {
        Direction partnerDirection = partnerDirection(state);
        if (partnerDirection == null || partnerDirection != direction) {
            return state;
        }

        return canPair(state, neighbour) && sideOf(neighbour) == BntWideSide.of(direction.getOpposite())
            ? state
            : (BlockState)state.setValue(WIDE, BntWideSide.NONE);
    }

    private static boolean canJoin(BlockGetter level, BlockPos pos, BlockState state, BlockPos neighbourPos, BlockState neighbour) {
        return canPair(state, neighbour) && isIndustrial(level, pos, state) == isIndustrial(level, neighbourPos, neighbour);
    }

    private static boolean isIndustrial(BlockGetter level, BlockPos pos, BlockState state) {
        if (HiddenCogwheelCompat.isHiddenCogwheel(state) && level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
            String originalBlock = access.bnt$getOriginalBlock();
            if (originalBlock != null && !originalBlock.isEmpty()) {
                return originalBlock.contains("industrial");
            }
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getPath().contains("industrial");
    }

    private static boolean canPair(BlockState state, BlockState neighbour) {
        return supports(neighbour)
            && neighbour.getValue(RotatedPillarKineticBlock.AXIS) == state.getValue(RotatedPillarKineticBlock.AXIS)
            && isSameSize(state.getBlock(), neighbour.getBlock());
    }

    private static boolean isSameSize(Block block, Block other) {
        return CogwheelSizeHelper.isLarge(block) == CogwheelSizeHelper.isLarge(other)
            && CogwheelSizeHelper.isMedium(block) == CogwheelSizeHelper.isMedium(other)
            && CogwheelSizeHelper.isTiny(block) == CogwheelSizeHelper.isTiny(other);
    }
}
