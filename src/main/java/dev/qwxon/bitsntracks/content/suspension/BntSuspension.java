package dev.qwxon.bitsntracks.content.suspension;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class BntSuspension {
    public static final int BOGIE = 2;
    public static final int WIDE_BOGIE = 3;
    public static final double PIVOT = 0.5;
    private static final double BOGIE_REACH = 0.5;
    private static final double BOGIE_RISE = 0.5 * Math.tan(Math.PI / 8.0);
    private static final double SAG = 0.1;
    private static final double LARGE_DRAWN_RADIUS = Math.hypot(16.0, 6.6) / 16.0;
    private static final double MEDIUM_DRAWN_RADIUS = 9.0 * Math.sqrt(2.0) / 16.0;
    private static final double SMALL_DRAWN_RADIUS = 9.0 / 16.0;
    private static final double TINY_DRAWN_RADIUS = 4.0 * Math.sqrt(2.0) / 16.0;

    private BntSuspension() {
    }

    public static boolean supports(BlockState state) {
        return (state.is((Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())
                || state.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get())
                || state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())
                || state.is((Block)BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL.get()))
            && state.hasProperty(BlockStateProperties.AXIS)
            && state.getValue(BlockStateProperties.AXIS) != Axis.Y;
    }

    public static boolean supportsBogie(BlockState state) {
        return pairable(state) && !BntCogwheelPairing.isWide(state);
    }

    public static boolean supportsWideBogie(BlockState state) {
        return pairable(state) && BntCogwheelPairing.isWide(state);
    }

    private static boolean pairable(BlockState state) {
        return supports(state)
            && (state.is((Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())
                || state.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get()));
    }

    public static boolean hasPiece(BlockEntity be) {
        return be instanceof KineticBlockEntityPhysicsAccess access
            && access.bnt$isPhysicsEnabled()
            && access.bnt$getSuspensionSide() != 0
            && supports(be.getBlockState());
    }

    public static boolean isBogie(BlockEntity be) {
        return hasPiece(be) && Math.abs(((KineticBlockEntityPhysicsAccess)be).bnt$getSuspensionSide()) >= BOGIE;
    }

    public static boolean isWideBogie(BlockEntity be) {
        return hasPiece(be) && Math.abs(((KineticBlockEntityPhysicsAccess)be).bnt$getSuspensionSide()) == WIDE_BOGIE;
    }

    public static boolean canCarry(BlockEntity be) {
        return be instanceof KineticBlockEntityPhysicsAccess access
            && access.bnt$isPhysicsEnabled()
            && access.bnt$getSuspensionSide() == 0
            && supports(be.getBlockState());
    }

    public static boolean canPair(BlockEntity be) {
        return canCarry(be) && pairable(be.getBlockState());
    }

    public static Axis axis(BlockState state) {
        return state.getValue(BlockStateProperties.AXIS);
    }

    public static Vec3 across(Axis axis) {
        return axis == Axis.X ? new Vec3(0.0, 0.0, -1.0) : new Vec3(1.0, 0.0, 0.0);
    }

    public static Vec3 along(Axis axis) {
        return axis == Axis.X ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 0.0, 1.0);
    }

    private static BlockPos rest(BlockPos cell, Axis axis, int facing) {
        return cell.relative(Direction.get(facing > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE, axis));
    }

    public static int facingFor(Level level, BlockPos pos, BlockState state, int side) {
        if (side == 0 || !supports(state)) {
            return 0;
        }
        Axis axis = axis(state);
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        if (isBogie(level.getBlockEntity(pos.offset(step(axis, side))))
            || partnerPos != null && isBogie(level.getBlockEntity(partnerPos.offset(step(axis, side))))) {
            return 0;
        }
        for (int facing : new int[]{1, -1}) {
            if (mounted(level, armEnd(level, pos, axis, facing), axis, side, facing)) {
                return facing;
            }
        }
        return 0;
    }

    public static BlockPos armEnd(Level level, BlockPos pos, Axis axis, int facing) {
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        return partnerPos != null && partnerPos.equals(rest(pos, axis, facing)) ? partnerPos : pos;
    }

    private static boolean mounted(Level level, BlockPos pos, Axis axis, int side, int facing) {
        BlockPos over = pos.offset(step(axis, side));
        for (BlockPos cell : new BlockPos[]{pos, over, pos.above(), over.above()}) {
            if (holds(level, cell, axis, facing)) {
                return true;
            }
        }
        return false;
    }

    public static BlockPos neighbour(BlockPos pos, BlockState state, int toward) {
        return pos.offset(step(axis(state), toward));
    }

    public static int towards(Level level, BlockPos from, BlockPos to) {
        BlockState state = level.getBlockState(from);
        BlockState other = level.getBlockState(to);
        boolean wide = supportsWideBogie(state) && supportsWideBogie(other);
        if (!wide && !(supportsBogie(state) && supportsBogie(other)) || axis(state) != axis(other)) {
            return 0;
        }
        Axis axis = axis(state);
        int toward = to.equals(from.offset(step(axis, 1))) ? 1 : to.equals(from.offset(step(axis, -1))) ? -1 : 0;
        if (toward != 0 && wide) {
            BlockPos fromHalf = BntCogwheelPairing.partnerPos(level, from);
            BlockPos toHalf = BntCogwheelPairing.partnerPos(level, to);
            if (fromHalf == null || toHalf == null || !toHalf.equals(fromHalf.offset(step(axis, toward)))) {
                return 0;
            }
        }
        return toward;
    }

    public static int bogieFacing(Level level, BlockPos pos, int toward) {
        BlockEntity be = level.getBlockEntity(pos);
        if (toward == 0 || !canPair(be)) {
            return 0;
        }
        Axis axis = axis(be.getBlockState());
        BlockPos partnerPos = pos.offset(step(axis, toward));
        BlockEntity partner = level.getBlockEntity(partnerPos);
        if (!canPair(partner) || towards(level, pos, partnerPos) != toward
            || CogwheelSizeHelper.isTiny(be.getBlockState().getBlock()) != CogwheelSizeHelper.isTiny(partner.getBlockState().getBlock())) {
            return 0;
        }
        for (BlockPos half : station(level, pos)) {
            if (!canCarry(level.getBlockEntity(half)) || facingPiece(level, half.offset(step(axis, -toward)), axis, toward)) {
                return 0;
            }
        }
        for (BlockPos half : station(level, partnerPos)) {
            if (!canCarry(level.getBlockEntity(half)) || facingPiece(level, half.offset(step(axis, toward)), axis, -toward)) {
                return 0;
            }
        }
        for (int facing : new int[]{1, -1}) {
            if (heldAtEnd(level, pos, partnerPos, axis, facing)) {
                return facing;
            }
        }
        return hangsOver(level, pos, partnerPos) ? 1 : 0;
    }

    private static boolean bogieMounted(Level level, BlockPos pos, BlockPos partnerPos, Axis axis, int facing) {
        return heldAtEnd(level, pos, partnerPos, axis, facing) || hangsOver(level, pos, partnerPos);
    }

    private static boolean heldAtEnd(Level level, BlockPos pos, BlockPos partnerPos, Axis axis, int facing) {
        return holds(level, armEnd(level, pos, axis, facing).above(), axis, facing)
            || holds(level, armEnd(level, partnerPos, axis, facing).above(), axis, facing);
    }

    private static boolean hangsOver(Level level, BlockPos pos, BlockPos partnerPos) {
        for (BlockPos station : new BlockPos[]{pos, partnerPos}) {
            for (BlockPos half : station(level, station)) {
                if (hangs(level, half.above())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<BlockPos> station(Level level, BlockPos pos) {
        BlockPos half = BntCogwheelPairing.partnerPos(level, pos);
        return half == null ? List.of(pos) : List.of(pos, half);
    }

    public static float bogieDrop(BlockEntity be, BlockEntity partner) {
        float drop = 0.0F;
        for (BlockEntity cog : new BlockEntity[]{be, partner}) {
            if (cog instanceof KineticBlockEntityPhysicsAccess access) {
                drop = Math.min(drop, access.bnt$getAlignmentOffsetY());
            }
        }
        return drop;
    }

    public static boolean stillHolds(Level level, BlockPos pos, BlockEntity be) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access) || !supports(be.getBlockState())) {
            return false;
        }
        Axis axis = axis(be.getBlockState());
        int side = access.bnt$getSuspensionSide();
        int facing = access.bnt$getSuspensionFacing();
        if (Math.abs(side) >= BOGIE) {
            KineticBlockEntity partner = partner(level, pos, be);
            boolean whole = Math.abs(side) == WIDE_BOGIE
                ? supportsWideBogie(be.getBlockState()) && partner != null
                    && sameStation(level, pos, side) && sameStation(level, partner.getBlockPos(), -side)
                : supportsBogie(be.getBlockState()) && partner != null;
            return whole && bogieMounted(level, pos, pos.offset(step(axis, side)), axis, facing);
        }
        return mounted(level, armEnd(level, pos, axis, facing), axis, side, facing);
    }

    private static boolean sameStation(Level level, BlockPos pos, int side) {
        BlockPos half = BntCogwheelPairing.partnerPos(level, pos);
        return half != null && level.getBlockEntity(half) instanceof KineticBlockEntityPhysicsAccess access
            && access.bnt$getSuspensionSide() == side;
    }

    public static boolean sharesPivot(Level level, BlockPos pos, Axis axis, int side) {
        return facingPiece(level, pos.offset(step(axis, side)), axis, -side);
    }

    public static KineticBlockEntity partner(Level level, BlockPos pos, BlockEntity be) {
        if (!isBogie(be)) {
            return null;
        }
        int side = ((KineticBlockEntityPhysicsAccess)be).bnt$getSuspensionSide();
        BlockEntity other = level.getBlockEntity(pos.offset(step(axis(be.getBlockState()), side)));
        return isBogie(other) && ((KineticBlockEntityPhysicsAccess)other).bnt$getSuspensionSide() == -side && other instanceof KineticBlockEntity kinetic
            ? kinetic
            : null;
    }

    private static boolean facingPiece(Level level, BlockPos pos, Axis axis, int side) {
        BlockEntity be = level.getBlockEntity(pos);
        return hasPiece(be) && axis(be.getBlockState()) == axis && ((KineticBlockEntityPhysicsAccess)be).bnt$getSuspensionSide() == side;
    }

    private static BlockPos step(Axis axis, int side) {
        Vec3 across = across(axis).scale(Integer.signum(side));
        return new BlockPos((int)Math.round(across.x), 0, (int)Math.round(across.z));
    }

    private static boolean hangs(Level level, BlockPos cell) {
        return level.getBlockState(cell).isFaceSturdy(level, cell, Direction.DOWN);
    }

    private static boolean holds(Level level, BlockPos cell, Axis axis, int facing) {
        BlockPos rest = rest(cell, axis, facing);
        Direction toward = Direction.get(facing > 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE, axis);
        return level.getBlockState(rest).isFaceSturdy(level, rest, toward);
    }

    public static void attach(KineticBlockEntity kinetic, int side, int facing) {
        for (KineticBlockEntity cog : withWidePartner(kinetic)) {
            if (cog instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setSuspension(side, facing);
                cog.setChanged();
                cog.sendData();
            }
        }
    }

    public static boolean heldByWidePartner(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !(be instanceof KineticBlockEntityPhysicsAccess access) || Math.abs(access.bnt$getSuspensionSide()) == BOGIE) {
            return false;
        }
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, be.getBlockPos());
        return partnerPos != null && level.getBlockEntity(partnerPos) instanceof KineticBlockEntityPhysicsAccess partner
            && partner.bnt$getSuspensionSide() != 0;
    }

    private static List<KineticBlockEntity> withWidePartner(KineticBlockEntity kinetic) {
        Level level = kinetic.getLevel();
        BlockPos partnerPos = level == null ? null : BntCogwheelPairing.partnerPos(level, kinetic.getBlockPos());
        return partnerPos != null && level.getBlockEntity(partnerPos) instanceof KineticBlockEntity partner
            ? List.of(kinetic, partner)
            : List.of(kinetic);
    }

    public static void attachBogie(KineticBlockEntity kinetic, KineticBlockEntity partner, int toward, int facing) {
        int type = BntCogwheelPairing.isWide(kinetic.getBlockState()) ? WIDE_BOGIE : BOGIE;
        List<KineticBlockEntity> lead = withWidePartner(kinetic);
        List<KineticBlockEntity> cogs = new ArrayList<>(lead);
        cogs.addAll(withWidePartner(partner));
        float drop = 0.0F;
        for (KineticBlockEntity cog : cogs) {
            drop = Math.min(drop, ((KineticBlockEntityPhysicsAccess)cog).bnt$getAlignmentOffsetY());
        }
        for (KineticBlockEntity cog : cogs) {
            if (cog instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setAlignmentOffsetX(0.0F);
                access.bnt$setAlignmentOffsetY(drop);
                access.bnt$setAlignmentOffsetZ(0.0F);
                access.bnt$setSuspension((lead.contains(cog) ? toward : -toward) * type, facing);
                cog.setChanged();
                cog.sendData();
            }
        }
    }

    public static List<KineticBlockEntity> bogieCogs(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic)) {
            return List.of();
        }
        KineticBlockEntity partner = partner(level, pos, kinetic);
        if (partner == null) {
            return List.of(kinetic);
        }
        List<KineticBlockEntity> cogs = new ArrayList<>(isWideBogie(kinetic) ? withWidePartner(kinetic) : List.of(kinetic));
        cogs.addAll(isWideBogie(kinetic) ? withWidePartner(partner) : List.of(partner));
        return cogs;
    }

    public static int detach(Level level, BlockPos pos, KineticBlockEntity kinetic, boolean drop) {
        if (!(kinetic instanceof KineticBlockEntityPhysicsAccess access) || access.bnt$getSuspensionSide() == 0) {
            return 0;
        }
        int side = Math.abs(access.bnt$getSuspensionSide());
        int pieces = 1;
        List<KineticBlockEntity> cogs = new ArrayList<>(side == BOGIE ? List.of(kinetic) : withWidePartner(kinetic));
        KineticBlockEntity partner = partner(level, pos, kinetic);
        if (partner != null) {
            pieces++;
            cogs.addAll(side == WIDE_BOGIE ? withWidePartner(partner) : List.of(partner));
        }
        for (KineticBlockEntity cog : cogs) {
            if (cog != kinetic && ((KineticBlockEntityPhysicsAccess)cog).bnt$getSuspensionSide() != 0) {
                clear(cog);
            }
        }
        clear(kinetic);
        if (drop) {
            Block.popResource(level, pos, new ItemStack(BitsNTracksItems.SUSPENSION_PIECE.get(), pieces));
        }
        return pieces;
    }

    private static void clear(KineticBlockEntity kinetic) {
        ((KineticBlockEntityPhysicsAccess)kinetic).bnt$setSuspension(0, 0);
        kinetic.setChanged();
        kinetic.sendData();
    }

    public record Arm(Vec3 across, double side, double reachAcross, double reachUp, double length, double upCap, double downCap, double ride) {
        public double upTravel(double travel) {
            return Math.min(travel, this.upCap);
        }

        public double downTravel(double travel) {
            return Math.min(travel, this.downCap);
        }

        public Vec3 displacement(double rise) {
            double up = Math.max(-this.downCap, Math.min(this.upCap, rise));
            double height = this.reachUp + up;
            double sign = Math.abs(this.reachAcross) < 1.0E-3 ? -this.side : Math.signum(this.reachAcross);
            double reach = sign * Math.sqrt(Math.max(0.0, this.length * this.length - height * height));
            return this.across.scale(reach - this.reachAcross).add(0.0, up, 0.0);
        }
    }

    public static Arm arm(BlockEntity be) {
        if (!hasPiece(be)) {
            return null;
        }
        int side = ((KineticBlockEntityPhysicsAccess)be).bnt$getSuspensionSide();
        return Math.abs(side) >= BOGIE ? bogieArm(be, side) : arm(be, side);
    }

    public static Arm arm(BlockEntity be, int side) {
        Vec3 across = across(axis(be.getBlockState()));
        KineticBlockEntityPhysicsAccess access = (KineticBlockEntityPhysicsAccess)be;
        Vec3 alignment = new Vec3(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
        double reachAcross = alignment.dot(across) - side * PIVOT;
        double reachUp = alignment.y - PIVOT;
        double length = Math.hypot(reachAcross, reachUp);
        double downCap = length + reachUp;
        double ride = 0.0;
        double halfway = halfwayToFacing(be, side);
        if (halfway > 0.0) {
            double approach = Math.max(0.0, halfway - drawnRadius(be.getBlockState()));
            double spread = Math.max(0.0, Math.abs(reachAcross) - approach);
            downCap = Math.min(downCap, Math.sqrt(Math.max(0.0, length * length - spread * spread)) + reachUp);
            ride = Math.max(0.0, SAG - downCap);
        }
        return new Arm(across, side, reachAcross, reachUp, length, -reachUp, downCap, ride);
    }

    private static double halfwayToFacing(BlockEntity be, int side) {
        Level level = be.getLevel();
        if (level == null) {
            return 0.0;
        }
        Axis axis = axis(be.getBlockState());
        BlockPos end = armEnd(level, be.getBlockPos(), axis, ((KineticBlockEntityPhysicsAccess)be).bnt$getSuspensionFacing());
        for (int cells = 1; cells <= 2; cells++) {
            if (facingPiece(level, end.offset(step(axis, side).multiply(cells)), axis, -side)) {
                return cells * 0.5;
            }
        }
        return 0.0;
    }

    private static Arm bogieArm(BlockEntity be, int side) {
        int toward = Integer.signum(side);
        double reach = BOGIE_REACH + bogieSpread(be);
        double length = Math.hypot(reach, BOGIE_RISE);
        double radius = drawnRadius(be.getBlockState());
        double droop = Math.max(0.0, Math.sqrt(Math.max(0.0, length * length - radius * radius)) - BOGIE_RISE);
        return new Arm(across(axis(be.getBlockState())), toward, -toward * reach, -BOGIE_RISE, length, 2.0 * BOGIE_RISE, droop, SAG);
    }

    private static double drawnRadius(BlockState state) {
        return CogwheelSizeHelper.isTiny(state.getBlock()) ? TINY_DRAWN_RADIUS
            : CogwheelSizeHelper.isLarge(state.getBlock()) ? LARGE_DRAWN_RADIUS
            : CogwheelSizeHelper.isMedium(state.getBlock()) ? MEDIUM_DRAWN_RADIUS
            : SMALL_DRAWN_RADIUS;
    }

    public static double bogieSpread(BlockEntity be) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access) || !supports(be.getBlockState())) {
            return 0.0;
        }
        Vec3 across = across(axis(be.getBlockState()));
        int toward = Integer.signum(access.bnt$getSuspensionSide());
        return -toward * (across.x * access.bnt$getAlignmentOffsetX() + across.z * access.bnt$getAlignmentOffsetZ());
    }

    public static double closestBogieSpread(BlockState state) {
        return -Math.max(0.0, BOGIE_REACH - drawnRadius(state));
    }

    public static void spreadBogie(KineticBlockEntity kinetic, KineticBlockEntity partner, double spread) {
        List<KineticBlockEntity> cogs = new ArrayList<>(withWidePartner(kinetic));
        cogs.addAll(withWidePartner(partner));
        for (KineticBlockEntity cog : cogs) {
            if (cog instanceof KineticBlockEntityPhysicsAccess access) {
                Vec3 across = across(axis(cog.getBlockState()));
                double offset = -Integer.signum(access.bnt$getSuspensionSide()) * spread;
                access.bnt$setAlignmentOffsetX((float)(across.x * offset));
                access.bnt$setAlignmentOffsetZ((float)(across.z * offset));
                cog.setChanged();
                cog.sendData();
            }
        }
    }

    public static double travel(BlockEntity be) {
        return CogwheelSizeHelper.getSuspensionRest(be.getBlockState().getBlock(), be);
    }

    public static Vec3 displacement(BlockEntity be, double drop) {
        Arm arm = arm(be);
        return arm == null ? new Vec3(0.0, -drop, 0.0) : arm.displacement(-drop);
    }

    public static Vec3 restDisplacement(BlockEntity be) {
        Arm arm = arm(be);
        return arm == null ? Vec3.ZERO : arm.displacement(arm.ride());
    }
}
