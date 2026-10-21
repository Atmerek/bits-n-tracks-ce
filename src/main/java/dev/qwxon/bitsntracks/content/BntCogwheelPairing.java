package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
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
        return side == BntWideSide.NONE ? null : side.toDirection(state.getValue(RotatedPillarKineticBlock.AXIS));
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

    public static int countWheels(Level level, Set<BlockPos> positions) {
        int wheels = 0;
        for (BlockPos pos : positions) {
            BlockPos partnerPos = partnerPos(level, pos);
            if (partnerPos == null || !positions.contains(partnerPos) || pos.asLong() < partnerPos.asLong()) {
                wheels++;
            }
        }
        return wheels;
    }

    public static KineticBlockEntity beltTwin(KineticBlockEntity wheel) {
        Level level = wheel.getLevel();
        if (level == null || onBelt(wheel)) {
            return wheel;
        }

        BlockPos partnerPos = partnerPos(level, wheel.getBlockPos());
        return partnerPos != null && level.getBlockEntity(partnerPos) instanceof KineticBlockEntity partner && onBelt(partner)
            ? partner
            : wheel;
    }

    private static boolean onBelt(KineticBlockEntity wheel) {
        CogwheelChainBehaviour behaviour = wheel.getBehaviour(CogwheelChainBehaviour.TYPE);
        return behaviour != null && behaviour.isPartOfChain();
    }

    public static void copySettings(KineticBlockEntityPhysicsAccess from, KineticBlockEntityPhysicsAccess to) {
        to.bnt$setAlignmentOffsetX(from.bnt$getAlignmentOffsetX());
        to.bnt$setAlignmentOffsetY(from.bnt$getAlignmentOffsetY());
        to.bnt$setAlignmentOffsetZ(from.bnt$getAlignmentOffsetZ());
        to.bnt$setHiddenByLever(from.bnt$isHiddenByLever());
        to.bnt$setTrackRouteSide(from.bnt$getTrackRouteSide());
        to.bnt$setExtension(from.bnt$getExtension());
        to.bnt$setLiftedUp(from.bnt$isLiftedUp());
        to.bnt$setMaxAirExtension(from.bnt$getMaxAirExtension());
        for (BntTuning setting : BntTuning.values()) {
            to.bnt$setTuning(setting, from.bnt$getTuning(setting));
        }
        if (Math.abs(from.bnt$getSuspensionSide()) != BntSuspension.BOGIE && Math.abs(to.bnt$getSuspensionSide()) != BntSuspension.BOGIE) {
            to.bnt$setSuspension(from.bnt$getSuspensionSide(), from.bnt$getSuspensionFacing());
        }
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
            BntFlangedCogwheelBlock.setPhysics(level, pos, true);
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
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);

        for (AxisDirection axisDirection : AxisDirection.values()) {
            Direction direction = Direction.fromAxisAndDirection(axis, axisDirection);
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (canPair(state, neighbour) && sideOf(neighbour) == BntWideSide.NONE) {
                return BntCogwheelShafts.keepSeam(
                    BntCogwheelWidth.withTrim(state.setValue(WIDE, BntWideSide.of(direction)), BntCogwheelWidth.trim(neighbour)));
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
        if (!canPair(state, partner)) {
            return;
        }

        BntWideSide expected = BntWideSide.of(direction.getOpposite());
        BlockState linked = BntCogwheelShafts.keepSeam(BntCogwheelWidth.withTrim(partner.setValue(WIDE, expected), BntCogwheelWidth.trim(state)));
        if (linked != partner) {
            level.setBlock(partnerPos, linked, Block.UPDATE_ALL);
        }
    }

    public static BlockState unlinkBrokenPair(BlockState state, Direction direction, BlockState neighbour) {
        Direction partnerDirection = partnerDirection(state);
        if (partnerDirection == null || partnerDirection != direction) {
            return state;
        }

        return canPair(state, neighbour) && sideOf(neighbour) == BntWideSide.of(direction.getOpposite())
            ? state
            : BntCogwheelWidth.withTrim(state.setValue(WIDE, BntWideSide.NONE), 0);
    }

    private static boolean canPair(BlockState state, BlockState neighbour) {
        return supports(neighbour)
            && neighbour.getValue(RotatedPillarKineticBlock.AXIS) == state.getValue(RotatedPillarKineticBlock.AXIS)
            && (neighbour.getBlock() == state.getBlock() || legacyPair(state, neighbour));
    }

    private static boolean legacyPair(BlockState state, BlockState neighbour) {
        return (state.getBlock() instanceof HiddenCogwheelBlock || neighbour.getBlock() instanceof HiddenCogwheelBlock)
            && CogwheelSize.of(state.getBlock()) == CogwheelSize.of(neighbour.getBlock());
    }
}
