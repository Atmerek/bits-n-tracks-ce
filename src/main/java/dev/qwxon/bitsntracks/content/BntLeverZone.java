package dev.qwxon.bitsntracks.content;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;

public final class BntLeverZone {
    private static final double CENTRE_SHARE = 0.4;

    private BntLeverZone() {
    }

    public static Direction of(Axis blockAxis, double dx, double dy, double dz, Block block) {
        double centre = CENTRE_SHARE * CogwheelSize.of(block).shapeRadius() / 16.0;
        double centreThresh = centre * centre;
        return switch (blockAxis) {
            case Z -> pick(dx, dy, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, centreThresh);
            case X -> pick(dz, dy, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN, centreThresh);
            case Y -> pick(dx, dz, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, centreThresh);
        };
    }

    private static Direction pick(double first, double second, Direction firstPositive, Direction firstNegative,
                                  Direction secondPositive, Direction secondNegative, double centreThresh) {
        if (first * first + second * second < centreThresh) {
            return null;
        }
        if (Math.abs(first) > Math.abs(second)) {
            return first > 0.0 ? firstPositive : firstNegative;
        }
        return second > 0.0 ? secondPositive : secondNegative;
    }
}
