package dev.qwxon.bitsntracks.physics;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public final class BntPhysicsTuning {
    public static final ModConfigSpec SPEC;

    private static final BooleanValue COGWHEEL_SUSPENSION_ENABLED;
    private static final BooleanValue TRACK_SUSPENSION_ENABLED;
    private static final DoubleValue BASE_SUSPENSION_STRENGTH;
    private static final DoubleValue SPRING_SCALE;
    private static final DoubleValue DAMPING_SCALE;
    private static final DoubleValue IMPULSE_SCALE;
    private static final DoubleValue BUMP_STOP_SCALE;
    private static final DoubleValue MAX_SUSPENSION_SPEED;

    private static final DoubleValue COGWHEEL_SPRING_MULTIPLIER;
    private static final DoubleValue TRACK_SPRING_MULTIPLIER;
    private static final DoubleValue COGWHEEL_DAMPING_MULTIPLIER;
    private static final DoubleValue TRACK_DAMPING_MULTIPLIER;
    private static final DoubleValue COGWHEEL_MAX_IMPULSE_MULTIPLIER;
    private static final DoubleValue TRACK_MAX_IMPULSE_MULTIPLIER;

    private static final DoubleValue TINY_COLLISION_RADIUS;
    private static final DoubleValue SMALL_COLLISION_RADIUS;
    private static final DoubleValue MEDIUM_COLLISION_RADIUS;
    private static final DoubleValue LARGE_COLLISION_RADIUS;
    private static final DoubleValue TINY_TRACK_RADIUS;
    private static final DoubleValue SMALL_TRACK_RADIUS;
    private static final DoubleValue MEDIUM_TRACK_RADIUS;
    private static final DoubleValue LARGE_TRACK_RADIUS;
    private static final DoubleValue TINY_SUSPENSION_REST;
    private static final DoubleValue SMALL_SUSPENSION_REST;
    private static final DoubleValue MEDIUM_SUSPENSION_REST;
    private static final DoubleValue LARGE_SUSPENSION_REST;
    private static final DoubleValue TINY_VERTICAL_OFFSET;
    private static final DoubleValue SMALL_VERTICAL_OFFSET;
    private static final DoubleValue MEDIUM_VERTICAL_OFFSET;
    private static final DoubleValue LARGE_VERTICAL_OFFSET;
    private static final DoubleValue TINY_VISUAL_VERTICAL_OFFSET;
    private static final DoubleValue SMALL_VISUAL_VERTICAL_OFFSET;
    private static final DoubleValue MEDIUM_VISUAL_VERTICAL_OFFSET;
    private static final DoubleValue LARGE_VISUAL_VERTICAL_OFFSET;
    private static final DoubleValue TINY_TOOL_HIGHLIGHT_RADIUS;
    private static final DoubleValue SMALL_TOOL_HIGHLIGHT_RADIUS;
    private static final DoubleValue MEDIUM_TOOL_HIGHLIGHT_RADIUS;
    private static final DoubleValue LARGE_TOOL_HIGHLIGHT_RADIUS;

    private static final DoubleValue DRIVE_TRACTION;
    private static final DoubleValue BRAKE_TRACTION;
    private static final DoubleValue TRACTION_RESPONSE;

    private static final DoubleValue COGWHEEL_GRIP_MULTIPLIER;
    private static final DoubleValue TRACK_GRIP_MULTIPLIER;
    private static final DoubleValue LATERAL_TRACTION;
    private static final DoubleValue PIVOT_SCRUB;
    private static final DoubleValue ROLLING_RESISTANCE;

    private static final DoubleValue TINY_STRESS_IMPACT;
    private static final DoubleValue SMALL_STRESS_IMPACT;
    private static final DoubleValue MEDIUM_STRESS_IMPACT;
    private static final DoubleValue LARGE_STRESS_IMPACT;

    private static final BooleanValue LANDING_SOUNDS_ENABLED;
    private static final DoubleValue LANDING_SOUND_MIN_FALL_BLOCKS;

    private BntPhysicsTuning() {
    }

    static {
        Builder builder = new Builder();

        builder.comment("Suspension response. Spring and damping are sized per contact point: each cogwheel takes a share of the vehicle mass, so a long track does not apply the whole vehicle's suspension force once per wheel.")
            .push("suspension");
        COGWHEEL_SUSPENSION_ENABLED = builder
            .comment("Apply suspension to cogwheels that are not part of a chain.")
            .define("cogwheelSuspensionEnabled", false);
        TRACK_SUSPENSION_ENABLED = builder
            .comment("Apply suspension to cogwheels that are part of a track chain.")
            .define("trackSuspensionEnabled", true);
        BASE_SUSPENSION_STRENGTH = builder
            .comment("Overall suspension gain. Scales spring, damping and the impulse ceiling together. Raise it if a vehicle sags onto its belly, lower it if it bounces.")
            .defineInRange("baseStrength", 15.0, 0.01, 100.0);
        SPRING_SCALE = builder
            .comment("Converts the suspension gain into a spring constant.")
            .defineInRange("springScale", 200.0, 0.0, 10000.0);
        DAMPING_SCALE = builder
            .comment("Converts the suspension gain into a damping constant.")
            .defineInRange("dampingScale", 50.0, 0.0, 10000.0);
        IMPULSE_SCALE = builder
            .comment("Converts the suspension gain into the per-tick impulse ceiling.")
            .defineInRange("impulseScale", 200.0, 0.0, 10000.0);
        BUMP_STOP_SCALE = builder
            .comment("Extra impulse headroom while the suspension is fully compressed.")
            .defineInRange("bumpStopScale", 3.0, 1.0, 100.0);
        MAX_SUSPENSION_SPEED = builder
            .comment("Fastest a wheel may push its share of the vehicle off a surface, in blocks per second, on top of whatever it takes to stop the approach. Bounds the kick a wheel gets when it ends up buried in terrain or in another vehicle, which is what happens for a moment when a structure breaks in two.")
            .defineInRange("maxSuspensionSpeed", 3.0, 0.0, 1000.0);
        COGWHEEL_SPRING_MULTIPLIER = builder.defineInRange("cogwheelSpringMultiplier", 0.0, 0.0, 100.0);
        TRACK_SPRING_MULTIPLIER = builder.defineInRange("trackSpringMultiplier", 0.5, 0.0, 100.0);
        COGWHEEL_DAMPING_MULTIPLIER = builder
            .comment("Damping resists suspension travel. At zero the suspension never settles.")
            .defineInRange("cogwheelDampingMultiplier", 0.3, 0.0, 100.0);
        TRACK_DAMPING_MULTIPLIER = builder.defineInRange("trackDampingMultiplier", 0.3, 0.0, 100.0);
        COGWHEEL_MAX_IMPULSE_MULTIPLIER = builder.defineInRange("cogwheelMaxImpulseMultiplier", 1.0, 0.0, 100.0);
        TRACK_MAX_IMPULSE_MULTIPLIER = builder.defineInRange("trackMaxImpulseMultiplier", 1.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Per size geometry. Sizes are tiny, small, medium and large.").push("geometry");
        TINY_COLLISION_RADIUS = builder.defineInRange("tinyCollisionRadius", 0.25, 0.01, 8.0);
        SMALL_COLLISION_RADIUS = builder.defineInRange("smallCollisionRadius", 0.5, 0.01, 8.0);
        MEDIUM_COLLISION_RADIUS = builder.defineInRange("mediumCollisionRadius", 0.75, 0.01, 8.0);
        LARGE_COLLISION_RADIUS = builder.defineInRange("largeCollisionRadius", 0.8, 0.01, 8.0);
        TINY_TRACK_RADIUS = builder.defineInRange("tinyTrackRadius", 0.35, 0.01, 8.0);
        SMALL_TRACK_RADIUS = builder.defineInRange("smallTrackRadius", 0.55, 0.01, 8.0);
        MEDIUM_TRACK_RADIUS = builder.defineInRange("mediumTrackRadius", 0.74, 0.01, 8.0);
        LARGE_TRACK_RADIUS = builder.defineInRange("largeTrackRadius", 1.1, 0.01, 8.0);
        TINY_SUSPENSION_REST = builder
            .comment("Resting suspension travel, in blocks.")
            .defineInRange("tinySuspensionRest", 0.45, 0.01, 8.0);
        SMALL_SUSPENSION_REST = builder.defineInRange("smallSuspensionRest", 0.65, 0.01, 8.0);
        MEDIUM_SUSPENSION_REST = builder.defineInRange("mediumSuspensionRest", 1.0, 0.01, 8.0);
        LARGE_SUSPENSION_REST = builder.defineInRange("largeSuspensionRest", 1.3, 0.01, 8.0);
        TINY_VERTICAL_OFFSET = builder.defineInRange("tinyVerticalOffset", -0.1, -8.0, 8.0);
        SMALL_VERTICAL_OFFSET = builder.defineInRange("smallVerticalOffset", -0.08, -8.0, 8.0);
        MEDIUM_VERTICAL_OFFSET = builder.defineInRange("mediumVerticalOffset", 0.0, -8.0, 8.0);
        LARGE_VERTICAL_OFFSET = builder.defineInRange("largeVerticalOffset", 0.1, -8.0, 8.0);
        TINY_VISUAL_VERTICAL_OFFSET = builder.defineInRange("tinyVisualVerticalOffset", 0.07, -8.0, 8.0);
        SMALL_VISUAL_VERTICAL_OFFSET = builder.defineInRange("smallVisualVerticalOffset", 0.09, -8.0, 8.0);
        MEDIUM_VISUAL_VERTICAL_OFFSET = builder.defineInRange("mediumVisualVerticalOffset", 0.0, -8.0, 8.0);
        LARGE_VISUAL_VERTICAL_OFFSET = builder.defineInRange("largeVisualVerticalOffset", 0.45, -8.0, 8.0);
        TINY_TOOL_HIGHLIGHT_RADIUS = builder
            .comment("Radius of the alignment tool's outline.")
            .defineInRange("tinyToolHighlightRadius", 0.34, 0.01, 8.0);
        SMALL_TOOL_HIGHLIGHT_RADIUS = builder.defineInRange("smallToolHighlightRadius", 0.42, 0.01, 8.0);
        MEDIUM_TOOL_HIGHLIGHT_RADIUS = builder.defineInRange("mediumToolHighlightRadius", 0.56, 0.01, 8.0);
        LARGE_TOOL_HIGHLIGHT_RADIUS = builder.defineInRange("largeToolHighlightRadius", 0.68, 0.01, 8.0);
        builder.pop();

        builder.comment("How fast a track carries the vehicle. The speed a track aims for is fixed by its cogwheel sizes and its rotational speed, so a track always travels at the speed its links are moving. These values only decide how hard it may push to get there.")
            .push("drive");
        DRIVE_TRACTION = builder
            .comment("Strongest acceleration a track at full grip can apply, in blocks per second squared.")
            .defineInRange("driveTraction", 10.0, 0.0, 1000.0);
        BRAKE_TRACTION = builder
            .comment("Additional acceleration available for braking at full redstone signal.")
            .defineInRange("brakeTraction", 20.0, 0.0, 1000.0);
        TRACTION_RESPONSE = builder
            .comment("Share of the remaining difference between track speed and ground speed corrected each physics step.")
            .defineInRange("tractionResponse", 1.0, 0.0, 1.0);
        builder.pop();

        builder.comment("Grip along and across the wheel.").push("friction");
        COGWHEEL_GRIP_MULTIPLIER = builder
            .comment("Grip of cogwheels that are not part of a chain. At zero they roll freely and only carry the suspension.")
            .defineInRange("cogwheelGripMultiplier", 0.0, 0.0, 100.0);
        TRACK_GRIP_MULTIPLIER = builder.defineInRange("trackGripMultiplier", 1.0, 0.0, 100.0);
        LATERAL_TRACTION = builder
            .comment("Strongest sideways acceleration a track at full grip can apply to stop a slide, in blocks per second squared.")
            .defineInRange("lateralTraction", 12.0, 0.0, 1000.0);
        PIVOT_SCRUB = builder
            .comment("How much of the turn the drive asks for the tracks may scrub through. At 0 they resist that turn as hard as any other sideways slide, which is what stops a vehicle from turning in place. At 1 the vehicle turns at exactly the rate its two track speeds work out to, which is quicker than a real tracked vehicle manages, since real tracks slip. Either way the vehicle stops turning as soon as the track speeds match again.")
            .defineInRange("pivotScrub", 0.7, 0.0, 1.0);
        ROLLING_RESISTANCE = builder
            .comment("Deceleration applied by a cogwheel that is not driven by a chain, in blocks per second squared.")
            .defineInRange("rollingResistance", 1.0, 0.0, 1000.0);
        builder.pop();

        builder.comment("Create stress consumed by each cogwheel size.").push("stress");
        TINY_STRESS_IMPACT = builder.defineInRange("tinyStressImpact", 2.0, 0.0, 1024.0);
        SMALL_STRESS_IMPACT = builder.defineInRange("smallStressImpact", 4.0, 0.0, 1024.0);
        MEDIUM_STRESS_IMPACT = builder.defineInRange("mediumStressImpact", 6.0, 0.0, 1024.0);
        LARGE_STRESS_IMPACT = builder.defineInRange("largeStressImpact", 8.0, 0.0, 1024.0);
        builder.pop();

        builder.comment("Sounds played by the suspension.").push("feedback");
        LANDING_SOUNDS_ENABLED = builder
            .comment("Play an impact sound when a wheel lands.")
            .define("landingSoundsEnabled", true);
        LANDING_SOUND_MIN_FALL_BLOCKS = builder
            .comment("Minimum fall height, in blocks, before a landing sound plays.")
            .defineInRange("landingSoundMinFallBlocks", 2.0, 0.0, 256.0);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean isCogwheelSuspensionEnabled() {
        return COGWHEEL_SUSPENSION_ENABLED.get();
    }

    public static boolean isTrackSuspensionEnabled() {
        return TRACK_SUSPENSION_ENABLED.get();
    }

    public static double getBaseSuspensionStrength() {
        return BASE_SUSPENSION_STRENGTH.get();
    }

    public static double getSpringScale() {
        return SPRING_SCALE.get();
    }

    public static double getDampingScale() {
        return DAMPING_SCALE.get();
    }

    public static double getImpulseScale() {
        return IMPULSE_SCALE.get();
    }

    public static double getBumpStopScale() {
        return BUMP_STOP_SCALE.get();
    }

    public static double getMaxSuspensionSpeed() {
        return MAX_SUSPENSION_SPEED.get();
    }

    public static double getCogwheelSpringMultiplier() {
        return COGWHEEL_SPRING_MULTIPLIER.get();
    }

    public static double getTrackSpringMultiplier() {
        return TRACK_SPRING_MULTIPLIER.get();
    }

    public static double getCogwheelDampingMultiplier() {
        return COGWHEEL_DAMPING_MULTIPLIER.get();
    }

    public static double getTrackDampingMultiplier() {
        return TRACK_DAMPING_MULTIPLIER.get();
    }

    public static double getCogwheelMaxImpulseMultiplier() {
        return COGWHEEL_MAX_IMPULSE_MULTIPLIER.get();
    }

    public static double getTrackMaxImpulseMultiplier() {
        return TRACK_MAX_IMPULSE_MULTIPLIER.get();
    }

    public static double getTinyCollisionRadius() {
        return TINY_COLLISION_RADIUS.get();
    }

    public static double getSmallCollisionRadius() {
        return SMALL_COLLISION_RADIUS.get();
    }

    public static double getMediumCollisionRadius() {
        return MEDIUM_COLLISION_RADIUS.get();
    }

    public static double getLargeCollisionRadius() {
        return LARGE_COLLISION_RADIUS.get();
    }

    public static double getTinyTrackRadius() {
        return TINY_TRACK_RADIUS.get();
    }

    public static double getSmallTrackRadius() {
        return SMALL_TRACK_RADIUS.get();
    }

    public static double getMediumTrackRadius() {
        return MEDIUM_TRACK_RADIUS.get();
    }

    public static double getLargeTrackRadius() {
        return LARGE_TRACK_RADIUS.get();
    }

    public static double getTinySuspensionRest() {
        return TINY_SUSPENSION_REST.get();
    }

    public static double getSmallSuspensionRest() {
        return SMALL_SUSPENSION_REST.get();
    }

    public static double getMediumSuspensionRest() {
        return MEDIUM_SUSPENSION_REST.get();
    }

    public static double getLargeSuspensionRest() {
        return LARGE_SUSPENSION_REST.get();
    }

    public static double getTinyVerticalOffset() {
        return TINY_VERTICAL_OFFSET.get();
    }

    public static double getSmallVerticalOffset() {
        return SMALL_VERTICAL_OFFSET.get();
    }

    public static double getMediumVerticalOffset() {
        return MEDIUM_VERTICAL_OFFSET.get();
    }

    public static double getLargeVerticalOffset() {
        return LARGE_VERTICAL_OFFSET.get();
    }

    public static double getTinyVisualVerticalOffset() {
        return TINY_VISUAL_VERTICAL_OFFSET.get();
    }

    public static double getSmallVisualVerticalOffset() {
        return SMALL_VISUAL_VERTICAL_OFFSET.get();
    }

    public static double getMediumVisualVerticalOffset() {
        return MEDIUM_VISUAL_VERTICAL_OFFSET.get();
    }

    public static double getLargeVisualVerticalOffset() {
        return LARGE_VISUAL_VERTICAL_OFFSET.get();
    }

    public static double getTinyToolHighlightRadius() {
        return TINY_TOOL_HIGHLIGHT_RADIUS.get();
    }

    public static double getSmallToolHighlightRadius() {
        return SMALL_TOOL_HIGHLIGHT_RADIUS.get();
    }

    public static double getMediumToolHighlightRadius() {
        return MEDIUM_TOOL_HIGHLIGHT_RADIUS.get();
    }

    public static double getLargeToolHighlightRadius() {
        return LARGE_TOOL_HIGHLIGHT_RADIUS.get();
    }

    public static double getDriveTraction() {
        return DRIVE_TRACTION.get();
    }

    public static double getBrakeTraction() {
        return BRAKE_TRACTION.get();
    }

    public static double getTractionResponse() {
        return TRACTION_RESPONSE.get();
    }

    public static double getCogwheelGripMultiplier() {
        return COGWHEEL_GRIP_MULTIPLIER.get();
    }

    public static double getTrackGripMultiplier() {
        return TRACK_GRIP_MULTIPLIER.get();
    }

    public static double getLateralTraction() {
        return LATERAL_TRACTION.get();
    }

    public static double getPivotScrub() {
        return PIVOT_SCRUB.get();
    }

    public static double getRollingResistance() {
        return ROLLING_RESISTANCE.get();
    }

    public static boolean isLandingSoundsEnabled() {
        return LANDING_SOUNDS_ENABLED.get();
    }

    public static double getLandingSoundMinFallBlocks() {
        return LANDING_SOUND_MIN_FALL_BLOCKS.get();
    }

    public static double getStressImpact(Block block) {
        if (CogwheelSizeHelper.isLarge(block)) {
            return LARGE_STRESS_IMPACT.get();
        } else if (CogwheelSizeHelper.isMedium(block)) {
            return MEDIUM_STRESS_IMPACT.get();
        } else {
            return CogwheelSizeHelper.isTiny(block) ? TINY_STRESS_IMPACT.get() : SMALL_STRESS_IMPACT.get();
        }
    }
}
