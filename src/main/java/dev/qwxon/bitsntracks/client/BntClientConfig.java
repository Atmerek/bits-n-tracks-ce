package dev.qwxon.bitsntracks.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class BntClientConfig {
    public static final ModConfigSpec SPEC;
    private static final BooleanValue DISABLE_FLYWHEEL_VISUALIZATION;
    private static final BooleanValue FLYWHEEL_RENDERING;
    private static final IntValue TRACK_UPDATE_RATE;

    private BntClientConfig() {
    }

    public static int trackUpdateRate() {
        return SPEC.isLoaded() ? TRACK_UPDATE_RATE.get() : 30;
    }

    public static boolean isFlywheelVisualizationDisabled() {
        return (Boolean)DISABLE_FLYWHEEL_VISUALIZATION.get();
    }

    public static boolean isFlywheelRenderingEnabled() {
        return !SPEC.isLoaded() || FLYWHEEL_RENDERING.get();
    }

    static {
        Builder builder = new Builder();
        builder.push("client");
        DISABLE_FLYWHEEL_VISUALIZATION = builder.comment(
                "Disable Flywheel block entity visualization and let Create render kinetic blocks with its normal block entity renderer."
            )
            .define("disableFlywheelVisualization", false);
        FLYWHEEL_RENDERING = builder.comment(
                "Draw Bits 'n' Tracks cogwheels and tracks through Flywheel instancing when Flywheel is active. Turn off to draw them with the block entity renderer instead."
            )
            .define("flywheelRendering", true);
        TRACK_UPDATE_RATE = builder.comment(
                "How many times a second a track drawn through Flywheel recomputes its shape within 32 blocks. Tracks within 64 blocks update at half this rate and farther ones at a third, and the scroll in between is carried on smoothly. 0 recomputes every frame."
            )
            .defineInRange("trackUpdateRate", 30, 0, 240);
        builder.pop();
        SPEC = builder.build();
    }
}
