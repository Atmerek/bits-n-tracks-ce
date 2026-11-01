package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainStops;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.index.BitsNTracksParticles;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class BntOverspeedEffects {
    private static final double RANGE = 48.0;
    private static final double MIN_HEAT = 0.25;
    private static final double SPARKS = 3.0;
    private static final double SPARK_SPEED = 0.3;
    private static final double SPARK_LIFT = 0.15;
    private static final double SMOKE = 0.5;
    private static final double LARGE_SMOKE_FROM = 0.5;
    private static final double SMOKE_LIFT = 0.03;
    private static final int MAX_TRACKED = 256;
    private static final Map<BlockPos, Heat> HEAT = new HashMap<>();
    private static Level heatLevel;

    private BntOverspeedEffects() {
    }

    public static void tick(KineticBlockEntity kbe) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = kbe.getLevel();
        if (level == null || minecraft.options.particles().get() == ParticleStatus.MINIMAL
            || Math.abs(kbe.getSpeed()) <= AllConfigs.server().kinetics.maxRotationSpeed.get()
            || BntChains.controllerPos(kbe) == null) {
            return;
        }
        BlockState state = kbe.getBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContainingClient(kbe) instanceof SubLevel containing ? containing : null;
        Vec3 center = kbe.getBlockPos().getCenter().add(BntClientCompat.getTransformedPosition(kbe, Vec3.ZERO, BlockPos.ZERO));
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        if ((subLevel == null ? center : subLevel.logicalPose().transformPosition(center)).distanceToSqr(camera) > RANGE * RANGE) {
            return;
        }

        double heat = heat(level, kbe.getBlockPos());
        RandomSource random = level.getRandom();
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        Vec3 u = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(next(axis), Direction.AxisDirection.POSITIVE).getNormal());
        Vec3 v = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(next(next(axis)), Direction.AxisDirection.POSITIVE).getNormal());
        double radius = CogwheelSize.of(state.getBlock()).chainRadius();
        double turning = Math.signum(kbe.getSpeed());

        int sparks = count(random, SPARKS * heat);
        for (int i = 0; i < sparks; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 out = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            Vec3 along = v.scale(Math.cos(angle)).subtract(u.scale(Math.sin(angle))).scale(turning);
            Vec3 velocity = along.scale(SPARK_SPEED * (0.6 + random.nextDouble() * 0.8)).add(out.scale(0.05));
            emit(level, subLevel, BitsNTracksParticles.SPARK.get(), center.add(out.scale(radius)), velocity, SPARK_LIFT * random.nextDouble());
        }
        if (random.nextDouble() < SMOKE * heat) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 at = center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
            boolean large = heat >= LARGE_SMOKE_FROM && random.nextDouble() < heat;
            emit(level, subLevel, large ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, at, Vec3.ZERO, SMOKE_LIFT * (0.5 + random.nextDouble()));
        }
    }

    private static void emit(Level level, SubLevel subLevel, ParticleOptions particle,Vec3 at, Vec3 velocity, double lift) {
        Vec3 pos = subLevel == null ? at : subLevel.logicalPose().transformPosition(at);
        Vec3 moving = subLevel == null ? velocity : subLevel.logicalPose().transformNormal(velocity);
        level.addParticle(particle, pos.x, pos.y, pos.z, moving.x, moving.y + lift, moving.z);
    }

    private static double heat(Level level, BlockPos pos) {
        if (heatLevel != level || HEAT.size() > MAX_TRACKED) {
            HEAT.clear();
            heatLevel = level;
        }
        long now = level.getGameTime();
        Heat heat = HEAT.computeIfAbsent(pos.immutable(), ignored -> new Heat(now));
        if (now - heat.last > 1L) {
            heat.since = now;
        }
        heat.last = now;
        double share = (double)(now - heat.since) / BntChainStops.OVERSPEED_GRACE;
        return Mth.clamp(MIN_HEAT + (1.0 - MIN_HEAT) * share, MIN_HEAT, 1.0);
    }

    private static Axis next(Axis axis) {
        return Axis.values()[(axis.ordinal() + 1) % 3];
    }

    private static int count(RandomSource random, double expected) {
        int whole = (int)expected;
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }

    private static final class Heat {
        private long since;
        private long last;

        private Heat(long now) {
            this.since = now;
            this.last = now;
        }
    }
}
