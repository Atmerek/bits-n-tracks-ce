package dev.qwxon.bitsntracks.content;

import com.simibubi.create.AllShapes;
import java.util.EnumMap;
import java.util.Map;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BntCogwheelShapes {
    private static final Map<CogwheelSize, VoxelShaper> GEARS = new EnumMap<>(CogwheelSize.class);

    static {
        for (CogwheelSize size : CogwheelSize.values()) {
            double radius = size.shapeRadius();
            GEARS.put(size, new AllShapes.Builder(Block.box(8.0 - radius, 4.0, 8.0 - radius, 8.0 + radius, 12.0, 8.0 + radius))
                .add(AllShapes.SIX_VOXEL_POLE.get(Axis.Y))
                .forAxis());
        }
    }

    private BntCogwheelShapes() {
    }

    public static VoxelShape get(CogwheelSize size, Axis axis) {
        return GEARS.get(size).get(axis);
    }
}
