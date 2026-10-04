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
    private static final IntValue ANIMATION_DISTANCE;

    private BntClientConfig() {
    }

    public static int trackUpdateRate() {
        return SPEC.isLoaded() ? TRACK_UPDATE_RATE.get() : 30;
    }

    public static int animationDistance() {
        return SPEC.isLoaded() ? ANIMATION_DISTANCE.get() : 48;
    }

    public static boolean isFlywheelVisualizationDisabled() {
        return (Boolean)DISABLE_FLYWHEEL_VISUALIZATION.get();
    }

    public static boolean isFlywheelRenderingEnabled() {
        return !SPEC.isLoaded() || FLYWHEEL_RENDERING.get() && !DISABLE_FLYWHEEL_VISUALIZATION.get();
    }

    static {
        Builder builder = new Builder();
        builder.push("client");
        DISABLE_FLYWHEEL_VISUALIZATION = builder.comment(
                "Disable Flywheel block entity visualization and let Create render kinetic blocks with its normal block entity renderer. Bits 'n' Tracks cogwheels and tracks are then drawn the same way."
            )
            .define("disableFlywheelVisualization", false);
        FLYWHEEL_RENDERING = builder.comment(
                "Draw Bits 'n' Tracks cogwheels and tracks through Flywheel instancing when Flywheel is active. Turn off to draw them with the block entity renderer instead. Has no effect while Disable Flywheel Rendering is on."
            )
            .define("flywheelRendering", true);
        TRACK_UPDATE_RATE = builder.comment(
                "How many times a second a track drawn through Flywheel recomputes its shape within 32 blocks. Tracks within 64 blocks update at half this rate and farther ones at a third, and the scroll in between is carried on smoothly. 0 recomputes every frame."
            )
            .defineInRange("trackUpdateRate", 30, 0, 240);
        ANIMATION_DISTANCE = builder.comment(
                "Blocks from the camera past which tracks drawn through Flywheel stop scrolling and suspension is drawn at its resting pose, so a far vehicle's running gear looks rigid. 0 animates them at any distance."
            )
            .defineInRange("animationDistance", 48, 0, 1024);
        builder.pop();
        SPEC = builder.build();
    }
}
