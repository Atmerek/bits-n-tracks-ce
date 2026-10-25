package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

public final class BntCogwheelTransform {
    private BntCogwheelTransform() {
    }

    public static void apply(KineticBlockEntity cogwheel, StructureTransform transform) {
        if (!(cogwheel instanceof KineticBlockEntityPhysicsAccess access)) {
            return;
        }
        Vec3 offset = moved(transform, new Vec3(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ()));
        access.bnt$setAlignmentOffsetX((float)offset.x);
        access.bnt$setAlignmentOffsetY((float)offset.y);
        access.bnt$setAlignmentOffsetZ((float)offset.z);

        int route = access.bnt$getTrackRouteSide();
        if (route >= 0 && route < Direction.values().length) {
            access.bnt$setTrackRouteSide(moved(transform, Direction.values()[route]).ordinal());
        }

        int side = access.bnt$getSuspensionSide();
        if (side != 0) {
            Axis after = BntSuspension.axis(cogwheel.getBlockState());
            Axis before = before(transform, after);
            access.bnt$setSuspension(
                turned(transform, BntSuspension.across(before), BntSuspension.across(after), side),
                turned(transform, BntSuspension.along(before), BntSuspension.along(after), access.bnt$getSuspensionFacing())
            );
        }
    }

    public static void afterSubLevelMove(AssemblyTransform transform, Iterable<BlockPos> blocks) {
        Rotation rotation = transform.getRotation();
        if (rotation == Rotation.NONE) {
            return;
        }
        ServerLevel level = transform.getLevel();
        StructureTransform structure = new StructureTransform(BlockPos.ZERO, Axis.Y, rotation, Mirror.NONE);
        for (BlockPos pos : blocks) {
            if (level.getBlockEntity(transform.apply(pos)) instanceof BntFlangedCogwheelBlockEntity cogwheel) {
                structure.apply(cogwheel);
                cogwheel.setChanged();
                cogwheel.sendData();
            }
        }
    }

    private static Direction moved(StructureTransform transform, Direction direction) {
        return transform.rotateFacing(transform.mirrorFacing(direction));
    }

    private static Vec3 moved(StructureTransform transform, Vec3 vector) {
        Vec3 result = Vec3.ZERO;
        for (Axis axis : Axis.values()) {
            double component = vector.get(axis);
            if (component != 0.0) {
                Direction direction = moved(transform, Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE));
                result = result.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(component));
            }
        }
        return result;
    }

    private static Axis before(StructureTransform transform, Axis after) {
        for (Axis axis : Axis.values()) {
            if (moved(transform, Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE)).getAxis() == after) {
                return axis;
            }
        }
        return after;
    }

    private static int turned(StructureTransform transform, Vec3 before, Vec3 after, int value) {
        if (value == 0) {
            return 0;
        }
        Direction moved = moved(transform, Direction.getNearest(before.scale(Integer.signum(value))));
        Direction expected = Direction.getNearest(after);
        return moved == expected ? Math.abs(value) : moved == expected.getOpposite() ? -Math.abs(value) : value;
    }
}
