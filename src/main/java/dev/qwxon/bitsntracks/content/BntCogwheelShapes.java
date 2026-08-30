package dev.qwxon.bitsntracks.content;

import com.simibubi.create.AllShapes;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BntCogwheelShapes {
    private static final VoxelShaper TINY_GEAR = gear(4.0);
    private static final VoxelShaper SMALL_GEAR = gear(6.0);
    private static final VoxelShaper MEDIUM_GEAR = gear(7.0);
    private static final VoxelShaper LARGE_GEAR = gear(8.0);

    private BntCogwheelShapes() {
    }

    public static VoxelShape get(CogwheelSize size, Axis axis) {
        return switch (size) {
            case TINY -> TINY_GEAR.get(axis);
            case MEDIUM -> MEDIUM_GEAR.get(axis);
            case LARGE -> LARGE_GEAR.get(axis);
            case SMALL -> SMALL_GEAR.get(axis);
        };
    }

    private static VoxelShaper gear(double radius) {
        return new AllShapes.Builder(Block.box(8.0 - radius, 4.0, 8.0 - radius, 8.0 + radius, 12.0, 8.0 + radius))
            .add(AllShapes.SIX_VOXEL_POLE.get(Axis.Y))
            .forAxis();
    }
}
