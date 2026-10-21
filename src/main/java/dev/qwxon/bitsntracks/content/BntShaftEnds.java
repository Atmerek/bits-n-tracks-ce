package dev.qwxon.bitsntracks.content;

import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.StringRepresentable;

public enum BntShaftEnds implements StringRepresentable {
    BOTH("both", true, true),
    NEGATIVE("negative", true, false),
    POSITIVE("positive", false, true),
    NONE("none", false, false);

    private final String name;
    private final boolean negative;
    private final boolean positive;

    BntShaftEnds(String name, boolean negative, boolean positive) {
        this.name = name;
        this.negative = negative;
        this.positive = positive;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean keeps(AxisDirection end) {
        return end == AxisDirection.POSITIVE ? this.positive : this.negative;
    }

    public BntShaftEnds with(AxisDirection end, boolean kept) {
        return end == AxisDirection.POSITIVE ? of(this.negative, kept) : of(kept, this.positive);
    }

    public BntShaftEnds flipped() {
        return of(this.positive, this.negative);
    }

    public static BntShaftEnds of(boolean negative, boolean positive) {
        return negative ? positive ? BOTH : NEGATIVE : positive ? POSITIVE : NONE;
    }
}
