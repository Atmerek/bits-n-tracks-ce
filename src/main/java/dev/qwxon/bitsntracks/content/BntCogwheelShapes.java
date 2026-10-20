package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BntCogwheelShapes {
    private static final double GEAR_LOW = 4.0;
    private static final double GEAR_HIGH = 12.0;
    private static final double STUB = 2.0;
    private static final double POLE_LOW = 5.0;
    private static final double POLE_HIGH = 11.0;
    private static final int TRIMS = 4;
    private static final VoxelShape[][][][] SHAPES =
        new VoxelShape[CogwheelSize.values().length][Axis.values().length][BntWideSide.values().length][TRIMS];

    static {
        for (CogwheelSize size : CogwheelSize.values()) {
            for (Axis axis : Axis.values()) {
                for (BntWideSide side : BntWideSide.values()) {
                    for (int trim = 0; trim < TRIMS; trim++) {
                        SHAPES[size.ordinal()][axis.ordinal()][side.ordinal()][trim] = build(size, axis, side, trim);
                    }
                }
            }
        }
    }

    private BntCogwheelShapes() {
    }

    public static VoxelShape get(CogwheelSize size, Axis axis) {
        return SHAPES[size.ordinal()][axis.ordinal()][BntWideSide.NONE.ordinal()][0];
    }

    public static VoxelShape get(CogwheelSize size, BlockState state) {
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        return SHAPES[size.ordinal()][axis.ordinal()][BntCogwheelPairing.sideOf(state).ordinal()][BntCogwheelWidth.trim(state)];
    }

    private static VoxelShape build(CogwheelSize size, Axis axis, BntWideSide side, int trim) {
        boolean wide = side != BntWideSide.NONE;
        double outer = Math.max(GEAR_LOW, STUB + BntCogwheelWidth.flangeShift(wide, trim));
        double inner = !wide ? 16.0 - outer : trim > 0 ? 16.0 : GEAR_HIGH;
        double low = side == BntWideSide.NEGATIVE ? 16.0 - inner : outer;
        double high = side == BntWideSide.NEGATIVE ? 16.0 - outer : inner;
        double radius = size.shapeRadius();
        return Shapes.or(
            box(axis, 8.0 - radius, 8.0 + radius, low, high),
            box(axis, POLE_LOW, POLE_HIGH, 0.0, 16.0));
    }

    private static VoxelShape box(Axis axis, double acrossLow, double acrossHigh, double alongLow, double alongHigh) {
        return switch (axis) {
            case X -> Block.box(alongLow, acrossLow, acrossLow, alongHigh, acrossHigh, acrossHigh);
            case Y -> Block.box(acrossLow, alongLow, acrossLow, acrossHigh, alongHigh, acrossHigh);
            case Z -> Block.box(acrossLow, acrossLow, alongLow, acrossHigh, acrossHigh, alongHigh);
        };
    }
}
