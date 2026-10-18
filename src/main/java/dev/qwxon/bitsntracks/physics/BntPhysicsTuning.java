package dev.qwxon.bitsntracks.physics;

import dev.qwxon.bitsntracks.content.CogwheelSize;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class BntPhysicsTuning {
    public static final ModConfigSpec SPEC;

    public static final double SUSPENSION_GAIN = 15.0;
    public static final double SPRING_SCALE = 200.0;
    public static final double DAMPING_SCALE = 14.0;
    public static final double WHEEL_SPRING = 0.5;
    public static final double WHEEL_DAMPING = 0.3;
    public static final double BELT_SUPPORT = 0.6;

    private static final BooleanValue DEBUG_LOGGING;
    private static final DoubleValue SUSPENSION_SMOOTHING;
    private static final DoubleValue IMPULSE_SCALE;
    private static final DoubleValue BUMP_STOP_SCALE;
    private static final DoubleValue MAX_SUSPENSION_SPEED;

    private static final DoubleValue TRACK_MAX_IMPULSE_MULTIPLIER;

    private static final Map<CogwheelSize, DoubleValue> COLLISION_RADIUS;
    private static final Map<CogwheelSize, DoubleValue> TRACK_RADIUS;
    private static final Map<CogwheelSize, DoubleValue> VERTICAL_OFFSET;
    private static final Map<CogwheelSize, DoubleValue> VISUAL_VERTICAL_OFFSET;
    private static final Map<CogwheelSize, DoubleValue> TOOL_HIGHLIGHT_RADIUS;

    private static final DoubleValue DRIVE_TRACTION;
    private static final DoubleValue BRAKE_TRACTION;
    private static final DoubleValue TRACTION_RESPONSE;

    private static final DoubleValue COGWHEEL_GRIP_MULTIPLIER;
    private static final DoubleValue TRACK_GRIP_MULTIPLIER;
    private static final DoubleValue LATERAL_TRACTION;
    private static final DoubleValue PIVOT_SCRUB;
    private static final DoubleValue ROLLING_RESISTANCE;

    private static final BooleanValue BELT_COLLISION_ENABLED;
    private static final BooleanValue BELT_DRAPE_ENABLED;
    private static final DoubleValue BELT_NODE_SPACING;
    private static final DoubleValue BELT_SURFACE_CLEARANCE;
    private static final DoubleValue BELT_MAX_SAG;
    private static final DoubleValue BELT_STIFFNESS_RANGE;
    private static final DoubleValue BELT_GRIP;
    private static final DoubleValue BELT_TENSION_STEP;
    private static final DoubleValue BELT_LINK_PITCH;
    private static final IntValue BELT_LINKS_PER_TEXTURE;
    private static final IntValue BELT_MAX_SLACK_LINKS;
    private static final DoubleValue BELT_PRE_TENSION;
    private static final DoubleValue BELT_MASS_PER_BLOCK;
    private static final DoubleValue BELT_TIGHT_SIDE_BIAS;
    private static final DoubleValue BELT_TIGHT_SIDE_SPEED;
    private static final BooleanValue BELT_HOLD_ENABLED;
    private static final DoubleValue BELT_MAX_HOLD;

    private static final Map<CogwheelSize, DoubleValue> STRESS_IMPACT;

    private static final BooleanValue LANDING_SOUNDS_ENABLED;
    private static final DoubleValue LANDING_SOUND_MIN_FALL_BLOCKS;

    private BntPhysicsTuning() {
    }

    static {
        Builder builder = new Builder();

        DEBUG_LOGGING = builder
            .comment("Write belt layout changes, kinetic repairs and a once a second wheel summary for every vehicle to the log. Use it to diagnose a track that wraps its cogwheels wrongly or throws its vehicle.")
            .define("debugLogging", false);

        builder.comment("Suspension response. Forces are sized per contact point: each cogwheel takes a share of the vehicle mass, so a long track does not apply the whole vehicle's suspension force once per wheel. Stiffness, damping, travel and spring strength are set on each cogwheel with the Suspension Tool.")
            .push("suspension");
        SUSPENSION_SMOOTHING = builder
            .comment("How fast a drawn wheel catches up to the terrain under it. One snaps to every block edge, lower eases the step out over several ticks.")
            .defineInRange("suspensionSmoothing", 0.7, 0.01, 1.0);
        IMPULSE_SCALE = builder
            .comment("Converts the suspension gain into the per-tick impulse ceiling.")
            .defineInRange("impulseScale", 200.0, 0.0, 10000.0);
        BUMP_STOP_SCALE = builder
            .comment("Extra impulse headroom while the suspension is fully compressed.")
            .defineInRange("bumpStopScale", 3.0, 1.0, 100.0);
        MAX_SUSPENSION_SPEED = builder
            .comment("Fastest a wheel may push its share of the vehicle off a surface, in blocks per second, on top of whatever it takes to stop the approach. Bounds the kick a wheel gets when it ends up buried in terrain or in another vehicle, which is what happens for a moment when a structure breaks in two.")
            .defineInRange("maxSuspensionSpeed", 6.0, 0.0, 1000.0);
        TRACK_MAX_IMPULSE_MULTIPLIER = builder.defineInRange("trackMaxImpulseMultiplier", 1.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Per size geometry. Sizes are tiny, intermediate, small, medium and large.").push("geometry");
        COLLISION_RADIUS = perSize(builder, "CollisionRadius", null, 0.01, new double[]{0.25, 0.375, 0.5, 0.75, 0.8});
        TRACK_RADIUS = perSize(builder, "TrackRadius", null, 0.01, new double[]{0.35, 0.425, 0.55, 0.74, 1.1});
        VERTICAL_OFFSET = perSize(builder, "VerticalOffset", null, -8.0, new double[]{-0.1, -0.09, -0.08, 0.0, 0.1});
        VISUAL_VERTICAL_OFFSET = perSize(builder, "VisualVerticalOffset",
            "How much higher than its collision radius a cogwheel rests above the ground, in blocks. Set so the outside of the track rests on the ground: track radius plus half a track link, plus the vertical offset, less the collision radius.",
            -8.0, new double[]{0.0625, 0.0225, 0.0325, 0.0525, 0.4625});
        TOOL_HIGHLIGHT_RADIUS = perSize(builder, "ToolHighlightRadius", "Radius of the alignment tool's outline.", 0.01,
            new double[]{0.34, 0.38, 0.42, 0.56, 0.68});
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
            .defineInRange("tractionResponse", 0.35, 0.0, 1.0);
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

        builder.comment("How the run of track between two cogwheels meets the ground, and how tension changes that. Tension is set per chain with the alignment lever, from fully slack to fully taut.").push("belt");
        BELT_COLLISION_ENABLED = builder
            .comment("Let the track between cogwheels rest on terrain. With this off only the cogwheels themselves touch the ground.")
            .define("beltCollisionEnabled", true);
        BELT_DRAPE_ENABLED = builder
            .comment("Let the run of track between two cogwheels bend over what it crosses instead of passing through it. Turning this off leaves it a straight line.")
            .define("beltDrapeEnabled", true);
        BELT_NODE_SPACING = builder
            .comment("Distance between the points a run of track is shaped by, in blocks. Smaller follows the ground more closely and costs more to work out.")
            .defineInRange("beltNodeSpacing", 0.25, 0.05, 4.0);
        BELT_SURFACE_CLEARANCE = builder
            .comment("Extra gap the track keeps above anything it climbs over, in blocks, on top of how it already sits on flat ground.")
            .defineInRange("beltSurfaceClearance", 0.15, 0.0, 1.0);
        BELT_MAX_SAG = builder
            .comment("Maximum belt hang, in blocks, so a long run does not droop through the floor.")
            .defineInRange("beltMaxSag", 1.0, 0.0, 8.0);
        BELT_STIFFNESS_RANGE = builder
            .comment("Spread between slack and taut. A taut belt pushes back this many times harder than a neutral one, a fully slack one this many times softer.")
            .defineInRange("beltStiffnessRange", 2.0, 1.0, 20.0);
        BELT_GRIP = builder
            .comment("Additional grip the track picks up from ground it touches between the cogwheels.")
            .defineInRange("beltGrip", 1.0, 0.0, 10.0);
        BELT_TENSION_STEP = builder
            .comment("Tension change from one click of the alignment lever.")
            .defineInRange("beltTensionStep", 0.1, 0.01, 1.0);
        BELT_LINK_PITCH = builder
            .comment("Length of one track link, in blocks. A loop is always a whole number of links, and slack is the surplus of that length over the taut path.")
            .defineInRange("beltLinkPitch", 0.25, 0.0625, 2.0);
        BELT_LINKS_PER_TEXTURE = builder
            .comment("Links drawn by one pass of the belt texture. The default texture is sixteen rows tall and shows four links.")
            .defineInRange("beltLinksPerTexture", 4, 1, 64);
        BELT_MAX_SLACK_LINKS = builder
            .comment("Links of slack a fully slack loop carries beyond the taut path. The hang follows the lever evenly, so this sets how deep the slackest setting hangs.")
            .defineInRange("beltMaxSlackLinks", 2, 0, 256);
        BELT_PRE_TENSION = builder
            .comment("Length a fully taut loop is fitted short by, in blocks, so it pulls on its wheels instead of merely reaching round them. At zero a taut loop is exactly the length of the path it wraps.")
            .defineInRange("beltPreTension", 0.3, 0.0, 8.0);
        BELT_MASS_PER_BLOCK = builder
            .comment("Mass of one block of track, in kilograms. Runs that are not resting on anything hang from the cogwheels under this weight.")
            .defineInRange("beltMassPerBlock", 0.1, 0.0, 1000.0);
        BELT_TIGHT_SIDE_BIAS = builder
            .comment("Share of its slack a run gives up when the sprocket is pulling into it. At zero the loop holds one tension all the way round.")
            .defineInRange("beltTightSideBias", 0.85, 0.0, 1.0);
        BELT_TIGHT_SIDE_SPEED = builder
            .comment("Chain speed at which the tight side reaches its full bias, in revolutions per minute.")
            .defineInRange("beltTightSideSpeed", 32.0, 1.0, 1024.0);
        BELT_HOLD_ENABLED = builder
            .comment("Let a loop that has run out of length hold its drooping wheels up instead of stretching to follow them.")
            .define("beltHoldEnabled", true);
        BELT_MAX_HOLD = builder
            .comment("Furthest the track may hold one wheel above the ground it would otherwise drop to, in blocks.")
            .defineInRange("beltMaxHold", 2.0, 0.0, 8.0);
        builder.pop();

        builder.comment("Create stress consumed by each cogwheel size.").push("stress");
        STRESS_IMPACT = perSize(builder, "StressImpact", null, 0.0, 1024.0, new double[]{2.0, 3.0, 4.0, 6.0, 8.0});
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

    public static boolean isDebugLogging() {
        return DEBUG_LOGGING.get();
    }

    public static boolean isBeltCollisionEnabled() {
        return BELT_COLLISION_ENABLED.get();
    }

    public static boolean isBeltDrapeEnabled() {
        return BELT_DRAPE_ENABLED.get();
    }

    public static double getBeltNodeSpacing() {
        return BELT_NODE_SPACING.get();
    }

    public static double getBeltSurfaceClearance() {
        return BELT_SURFACE_CLEARANCE.get();
    }

    public static double getBeltMaxSag() {
        return BELT_MAX_SAG.get();
    }

    public static double getBeltStiffnessRange() {
        return BELT_STIFFNESS_RANGE.get();
    }

    public static double getBeltTensionStep() {
        return BELT_TENSION_STEP.get();
    }

    public static double getBeltLinkPitch() {
        return BELT_LINK_PITCH.get();
    }

    public static int getBeltLinksPerTexture() {
        return BELT_LINKS_PER_TEXTURE.get();
    }

    public static int getBeltMaxSlackLinks() {
        return BELT_MAX_SLACK_LINKS.get();
    }

    public static double getBeltPreTension() {
        return BELT_PRE_TENSION.get();
    }

    public static double getBeltMassPerBlock() {
        return BELT_MASS_PER_BLOCK.get();
    }

    public static double getBeltTightSideBias() {
        return BELT_TIGHT_SIDE_BIAS.get();
    }

    public static double getBeltTightSideSpeed() {
        return BELT_TIGHT_SIDE_SPEED.get();
    }

    public static boolean isBeltHoldEnabled() {
        return BELT_HOLD_ENABLED.get();
    }

    public static double getBeltMaxHold() {
        return BELT_MAX_HOLD.get();
    }

    public static double getSuspensionSmoothing() {
        return SUSPENSION_SMOOTHING.get();
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

    public static double getTrackMaxImpulseMultiplier() {
        return TRACK_MAX_IMPULSE_MULTIPLIER.get();
    }

    private static Map<CogwheelSize, DoubleValue> perSize(Builder builder, String name, String comment, double min, double[] defaults) {
        return perSize(builder, name, comment, min, 8.0, defaults);
    }

    private static Map<CogwheelSize, DoubleValue> perSize(
        Builder builder, String name, String comment, double min, double max, double[] defaults
    ) {
        Map<CogwheelSize, DoubleValue> values = new EnumMap<>(CogwheelSize.class);
        for (CogwheelSize size : CogwheelSize.values()) {
            if (comment != null && size.ordinal() == 0) {
                builder.comment(comment);
            }
            values.put(size, builder.defineInRange(size.name().toLowerCase(Locale.ROOT) + name, defaults[size.ordinal()], min, max));
        }
        return values;
    }

    public static double collisionRadius(CogwheelSize size) {
        return COLLISION_RADIUS.get(size).get();
    }

    public static double trackRadius(CogwheelSize size) {
        return TRACK_RADIUS.get(size).get();
    }

    public static double verticalOffset(CogwheelSize size) {
        return VERTICAL_OFFSET.get(size).get();
    }

    public static double visualVerticalOffset(CogwheelSize size) {
        return VISUAL_VERTICAL_OFFSET.get(size).get();
    }

    public static double toolHighlightRadius(CogwheelSize size) {
        return TOOL_HIGHLIGHT_RADIUS.get(size).get();
    }

    public static double stressImpact(CogwheelSize size) {
        return STRESS_IMPACT.get(size).get();
    }

    public static double getDriveTraction() {
        return DRIVE_TRACTION.get();
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

    public static double getBeltGrip() {
        return BELT_GRIP.get();
    }

    public static double getBrakeTraction() {
        return BRAKE_TRACTION.get();
    }

    public static double getTractionResponse() {
        return TRACTION_RESPONSE.get();
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

}
