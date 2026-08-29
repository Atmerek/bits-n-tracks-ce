package dev.qwxon.bitsntracks.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;

public final class BntClientConfig {
    public static final ModConfigSpec SPEC;
    private static final BooleanValue DISABLE_FLYWHEEL_VISUALIZATION;

    private BntClientConfig() {
    }

    public static boolean isFlywheelVisualizationDisabled() {
        return (Boolean)DISABLE_FLYWHEEL_VISUALIZATION.get();
    }

    static {
        Builder builder = new Builder();
        builder.push("client");
        DISABLE_FLYWHEEL_VISUALIZATION = builder.comment(
                "Disable Flywheel block entity visualization and let Create render kinetic blocks with its normal block entity renderer."
            )
            .define("disableFlywheelVisualization", false);
        builder.pop();
        SPEC = builder.build();
    }
}
