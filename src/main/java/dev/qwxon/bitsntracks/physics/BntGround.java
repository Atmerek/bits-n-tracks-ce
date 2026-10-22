package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

public final class BntGround {
    private static final double SLIPPERY_FLOOR = 0.1;
    private static final double BELT_HARD = 1.0;
    private static final double BELT_SOFT = 0.85;
    private static final double TREAD_HARD = 0.9;
    private static final double TREAD_SOFT = 1.15;
    private static final double SINK = 1.0 / 16.0;
    private static final double MIN_SPREAD = 0.75;
    private static final double MAX_SPREAD = 1.3;

    public record Grip(Optional<Double> grip, boolean soft) {
        public static final Codec<Grip> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("grip").forGetter(Grip::grip),
            Codec.BOOL.optionalFieldOf("soft", false).forGetter(Grip::soft)
        ).apply(instance, Grip::new));
    }

    public record Surface(double grip, boolean soft) {
    }

    public static final DataMapType<Block, Grip> GRIP = DataMapType.builder(BitsNTracks.asResource("ground"), Registries.BLOCK, Grip.CODEC).build();
    public static final Surface UNKNOWN = new Surface(1.0, false);

    private BntGround() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(GRIP);
    }

    public static Surface of(BlockState state) {
        double friction = PhysicsBlockPropertyHelper.getFriction(state);
        double sable = friction < 1.0 ? SLIPPERY_FLOOR + (1.0 - SLIPPERY_FLOOR) * friction : friction;
        Grip mapped = state.getBlockHolder().getData(GRIP);
        return mapped == null ? new Surface(sable, false) : new Surface(mapped.grip().orElse(sable), mapped.soft());
    }

    public static double patch(double radius) {
        return 2.0 * Math.sqrt(2.0 * Math.max(radius, 0.0) * SINK);
    }

    public static double spread(double pressure) {
        return pressure > 0.0
            ? Mth.clamp(Math.sqrt(BntPhysicsTuning.getSoftGroundPressure() / pressure), MIN_SPREAD, MAX_SPREAD)
            : 1.0;
    }

    public static double grip(Surface surface, @Nullable CogwheelChainType type) {
        boolean tread = type == BntCogwheelChainTypes.TANK_TREAD_CHAIN.get();
        return surface.grip() * (surface.soft() ? (tread ? TREAD_SOFT : BELT_SOFT) : (tread ? TREAD_HARD : BELT_HARD));
    }
}
