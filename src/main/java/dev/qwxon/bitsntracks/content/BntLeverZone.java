package dev.qwxon.bitsntracks.content;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public final class BntLeverZone {
    private BntLeverZone() {
    }

    public static Direction of(Axis blockAxis, double dx, double dy, double dz, double radius) {
        double centerHalf = 0.5 * radius;
        double centerThresh = centerHalf * centerHalf;
        return switch (blockAxis) {
            case Z -> pick(dx, dy, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, centerThresh);
            case X -> pick(dz, dy, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN, centerThresh);
            case Y -> pick(dx, dz, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, centerThresh);
        };
    }

    private static Direction pick(double first, double second, Direction firstPositive, Direction firstNegative,
                                  Direction secondPositive, Direction secondNegative, double centerThresh) {
        if (first * first + second * second < centerThresh) {
            return null;
        }
        if (Math.abs(first) > Math.abs(second)) {
            return first > 0.0 ? firstPositive : firstNegative;
        }
        return second > 0.0 ? secondPositive : secondNegative;
    }
}
