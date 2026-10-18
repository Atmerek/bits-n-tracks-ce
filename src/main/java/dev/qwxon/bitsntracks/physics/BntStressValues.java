package dev.qwxon.bitsntracks.physics;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;

public final class BntStressValues {
    private static boolean registered;

    private BntStressValues() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            BlockStressValues.IMPACTS
                .registerProvider(
                    block -> HiddenCogwheelCompat.isOwnOrLegacyCogwheel(block)
                        ? () -> BntPhysicsTuning.stressImpact(CogwheelSize.of(block))
                        : null
                );
        }
    }
}
