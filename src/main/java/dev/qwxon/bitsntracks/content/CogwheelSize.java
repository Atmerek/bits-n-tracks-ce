package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.world.level.block.Block;

public enum CogwheelSize {
    TINY,
    INTERMEDIATE,
    SMALL,
    MEDIUM,
    LARGE;

    public static CogwheelSize of(Block block) {
        if (block instanceof BntFlangedCogwheelBlock cogwheel) {
            return cogwheel.size();
        }
        if (block instanceof HiddenCogwheelBlock hidden) {
            return hidden.size();
        }
        return block instanceof ICogWheel cog && cog.isLargeCog() ? LARGE : SMALL;
    }
}
