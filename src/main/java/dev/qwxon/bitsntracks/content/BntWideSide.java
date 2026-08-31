package dev.qwxon.bitsntracks.content;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.StringRepresentable;

public enum BntWideSide implements StringRepresentable {
    NONE("none"),
    NEGATIVE("negative"),
    POSITIVE("positive");

    private final String name;

    BntWideSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public BntWideSide opposite() {
        return switch (this) {
            case NEGATIVE -> POSITIVE;
            case POSITIVE -> NEGATIVE;
            case NONE -> NONE;
        };
    }

    public Direction toDirection(Axis axis) {
        return switch (this) {
            case NEGATIVE -> Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE);
            case POSITIVE -> Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
            case NONE -> null;
        };
    }

    public static BntWideSide of(Direction direction) {
        return direction.getAxisDirection() == AxisDirection.POSITIVE ? POSITIVE : NEGATIVE;
    }
}
