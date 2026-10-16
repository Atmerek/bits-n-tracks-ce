package dev.qwxon.bitsntracks.physics;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;

public final class BntStressValues {
    private static boolean registered;

    private BntStressValues() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            BlockStressValues.IMPACTS
                .registerProvider(
                    block -> block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock
                        ? () -> BntPhysicsTuning.getStressImpact(block)
                        : null
                );
        }
    }
}
