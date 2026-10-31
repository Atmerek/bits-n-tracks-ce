package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntGrousers;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksParticles;
import dev.qwxon.bitsntracks.physics.BntGround;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3d;

public final class BntWheelEffects {
    private static final double RANGE = 48.0;
    private static final double MIN_ROLLING = 0.5;
    private static final double DUST_PER_SPEED = 0.2;
    private static final double GROUND_SHARE = 0.65;
    private static final double MIN_SLIP = 1.0;
    private static final double SLIP_PER_SPEED = 0.2;
    private static final int MAX_SLIP = 16;
    private static final double FLING_BASE = 0.2;
    private static final double FLING_PER_SPEED = 0.03;
    private static final double FLING_MAX = 0.5;
    private static final double FLING_LIFT = 0.25;
    private static final double FLING_SPREAD = 0.05;
    private static final double SPARK_SHARE = 0.65;
    private static final double SPARK_SPEED = 1.2;
    private static final double SPARK_LIFT = 0.3;
    private static final double SPARK_SPREAD = 0.2;
    private static final double LANDING_SPARK_SPEED = 0.25;
    private static final double MIN_LANDING = 2.0;
    private static final double LANDING_PER_SPEED = 3.0;
    private static final int MAX_LANDING = 40;
    private static final int MIN_AIR_TICKS = 2;
    private static final double STANDARD_WIDTH = 14.0 / 16.0;
    private static final double WHEEL_HALF_WIDTH = 0.3;
    private static final double WHEEL_REACH = 0.15;
    private static final double STEP = 0.25;
    private static final int MAX_SAMPLES = 64;
    private static final double PROBE_ABOVE = 0.25;
    private static final double PROBE_BELOW = 0.12;
    private static final double REAR_SHARE = 0.35;
    private static final int FORGET_TICKS = 40;
    private static final Map<BlockPos, Group> PENDING = new HashMap<>();
    private static final Map<BlockPos, Air> AIR = new HashMap<>();
    private static Level pendingLevel;
    private static Level airLevel;

    private BntWheelEffects() {
    }

    public static void tick(KineticBlockEntity kbe, SubLevel subLevel) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = kbe.getLevel();
        if (level == null || minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        if (subLevel.logicalPose().transformPosition(kbe.getBlockPos().getCenter()).distanceToSqr(camera) > RANGE * RANGE) {
            return;
        }

        if (pendingLevel != level) {
            PENDING.clear();
            pendingLevel = level;
        }
        BlockPos controller = BntChains.controllerPos(BntCogwheelPairing.beltTwin(kbe));
        if (controller != null) {
            BntPhysicsEvents.Drive drive = BntPhysicsEvents.drive(kbe, subLevel);
            if (drive != null) {
                Group group = PENDING.computeIfAbsent(controller, Group::new);
                group.belt = true;
                group.subLevel = subLevel;
                group.drives.add(drive);
            }
            return;
        }
        BntPhysicsEvents.GroundContact contact = BntPhysicsEvents.groundContact(kbe, subLevel);
        if (contact != null) {
            PENDING.computeIfAbsent(kbe.getBlockPos(), Group::new).wheels.add(contact);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            PENDING.clear();
            AIR.clear();
            pendingLevel = null;
            airLevel = null;
            return;
        }
        Level level = pendingLevel;
        if (airLevel != level) {
            AIR.clear();
            airLevel = level;
        }
        if (level == null || PENDING.isEmpty()) {
            forget(level);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RandomSource random = level.getRandom();
        long now = level.getGameTime();
        for (Group group : PENDING.values()) {
            Air air = AIR.computeIfAbsent(group.key, ignored -> new Air());
            air.seen = now;
            Patch patch = group.belt ? beltPatch(level, group) : wheelPatch(level, group);
            if (patch != null) {
                effects(minecraft, level, random, patch, air);
            }
        }
        PENDING.clear();
        forget(level);
    }

    private static Patch beltPatch(Level level, Group group) {
        BlockPos controller = group.key;
        CogwheelChain chain = BntChains.chainAt(level, controller);
        BlockEntity controllerBe = level.getBlockEntity(controller);
        if (chain == null || !(controllerBe instanceof KineticBlockEntity kinetic) || group.drives.isEmpty()) {
            return null;
        }
        List<RenderedChainPathNode> nodes = chain.getChainPathNodes();
        if (nodes == null || nodes.size() < 2) {
            return null;
        }

        BntPhysicsEvents.Drive drive = group.drives.get(0);
        double speed = 0.0;
        for (BntPhysicsEvents.Drive each : group.drives) {
            speed += each.speed();
        }
        drive = new BntPhysicsEvents.Drive(drive.travel(), drive.side(), speed / group.drives.size(), drive.driven());

        int count = nodes.size();
        Vec3 base = Vec3.atLowerCornerOf(controller);
        Map<BlockPos, Vec3> shifts = new HashMap<>();
        Vec3[] path = new Vec3[count];
        Vec3 centroid = Vec3.ZERO;
        for (int i = 0; i < count; i++) {
            RenderedChainPathNode node = nodes.get(i);
            Vec3 shift = shifts.computeIfAbsent(node.relativePos(),
                relative -> BntClientCompat.getTransformedPosition(controllerBe, Vec3.ZERO, relative));
            path[i] = base.add(node.getPosition()).add(shift);
            centroid = centroid.add(path[i]);
        }
        centroid = centroid.scale(1.0 / count);
        double loop = 0.0;
        for (int i = 0; i < count; i++) {
            loop += path[i].distanceTo(path[(i + 1) % count]);
        }

        SubLevel subLevel = group.subLevel;
        Pose3dc pose = subLevel.logicalPose();
        Vec3 down = pose.transformNormalInverse(new Vec3(0.0, -1.0, 0.0));
        double step = Math.max(STEP, loop / MAX_SAMPLES);
        List<Sample> samples = new ArrayList<>();
        double next = step * 0.5;
        double walked = 0.0;
        for (int i = 0; i < count; i++) {
            Vec3 from = path[i];
            Vec3 to = path[(i + 1) % count];
            double length = from.distanceTo(to);
            while (next <= walked + length) {
                Vec3 point = length < 1.0E-9 ? from : from.lerp(to, (next - walked) / length);
                next += step;
                if (point.subtract(centroid).dot(down) > 0.0) {
                    Sample sample = probe(level, subLevel, pose, point, drive, step);
                    if (sample != null) {
                        samples.add(sample);
                    }
                }
            }
            walked += length;
        }

        Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(centroid.x, centroid.y, centroid.z));
        boolean metal = chain.getChainType() == BntCogwheelChainTypes.TANK_TREAD_CHAIN.get() || BntGrousers.fitted(controllerBe);
        double halfWidth = BntCogwheelWidth.trackPixels(BntCogwheelWidth.chainQuarters(kinetic)) / 32.0;
        return new Patch(samples, drive, halfWidth, step * 0.5, metal, velocity.y);
    }

    private static Sample probe(Level level, SubLevel subLevel, Pose3dc pose, Vec3 point, BntPhysicsEvents.Drive drive, double step) {
        Vec3 world = pose.transformPosition(point);
        ClipContext clip = new ClipContext(
            world.add(0.0, PROBE_ABOVE, 0.0), world.subtract(0.0, CogwheelSizeHelper.TRACK_HALF_THICKNESS + PROBE_BELOW, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        ((ClipContextExtension)clip).sable$setSubLevelIgnoring(other -> other == subLevel);
        BlockHitResult hit = level.clip(clip);
        if (hit.getType() == HitResult.Type.MISS || hit.isInside() || hit.getDirection().getStepY() <= 0) {
            return null;
        }
        BlockState ground = level.getBlockState(hit.getBlockPos());
        if (ground.getRenderShape() == RenderShape.INVISIBLE) {
            return null;
        }
        Vec3 location = hit.getLocation();
        SubLevel hitSubLevel = Sable.HELPER.getContaining(level, location);
        if (hitSubLevel != null) {
            location = hitSubLevel.logicalPose().transformPosition(location);
        }

        Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(point.x, point.y, point.z));
        Vec3 moving = new Vec3(velocity.x, velocity.y, velocity.z);
        double rolling = moving.dot(drive.travel());
        return new Sample(location, ground, location.dot(drive.travel()),
            flat(moving.subtract(drive.travel().scale(drive.target(rolling)))), rolling, step);
    }

    private static Patch wheelPatch(Level level, Group group) {
        List<Sample> samples = new ArrayList<>();
        double vertical = 0.0;
        BntPhysicsEvents.Drive drive = null;
        for (BntPhysicsEvents.GroundContact contact : group.wheels) {
            vertical = Math.min(vertical, contact.vertical());
            drive = drive == null ? contact.drive() : drive;
            if (!contact.grounded()) {
                continue;
            }
            BlockState ground = level.getBlockState(contact.ground());
            if (ground.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }
            BntPhysicsEvents.Drive own = contact.drive();
            Vec3 slip = own.travel().scale(contact.rolling() - own.target(contact.rolling())).add(own.side().scale(contact.sideways()));
            samples.add(new Sample(
                contact.point(), ground, contact.point().dot(own.travel()), flat(slip), contact.rolling(), 2.0 * WHEEL_REACH));
        }
        return drive == null ? null : new Patch(samples, drive, WHEEL_HALF_WIDTH, WHEEL_REACH, false, vertical);
    }

    private static void effects(Minecraft minecraft, Level level, RandomSource random, Patch patch, Air air) {
        if (patch.samples.isEmpty()) {
            air.ticks++;
            air.fall = Math.min(air.fall, patch.vertical);
            return;
        }
        if (air.ticks >= MIN_AIR_TICKS && -air.fall >= MIN_LANDING) {
            landing(level, random, patch, -air.fall);
        }
        air.ticks = 0;
        air.fall = 0.0;

        double width = patch.halfWidth * 2.0 / STANDARD_WIDTH;
        double rolling = 0.0;
        for (Sample sample : patch.samples) {
            rolling += sample.rolling;
        }
        rolling /= patch.samples.size();
        if (Math.abs(rolling) >= MIN_ROLLING) {
            int count = count(random, Math.abs(rolling) * DUST_PER_SPEED * width);
            double rear = -Math.signum(rolling);
            Vec3 thrown = patch.travel.scale(rear);
            for (int i = 0; i < count; i++) {
                Spot spot = patch.towardEnd(random, rear);
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, spot.ground), spot.pos.x, spot.pos.y + 0.1, spot.pos.z,
                    thrown.x, 1.5, thrown.z);
            }
        }

        double[] weights = new double[patch.samples.size()];
        double total = 0.0;
        for (int i = 0; i < weights.length; i++) {
            Sample sample = patch.samples.get(i);
            double slip = sample.slip.length();
            weights[i] = slip >= MIN_SLIP ? slip * sample.length : 0.0;
            total += weights[i];
        }
        if (total <= 0.0) {
            return;
        }
        int count = Math.min(MAX_SLIP, count(random, total * SLIP_PER_SPEED * width));
        for (int i = 0; i < count; i++) {
            Sample sample = patch.samples.get(pick(random, weights, total));
            double slip = sample.slip.length();
            Vec3 direction = sample.slip.scale(1.0 / slip);
            double forward = sample.slip.dot(patch.travel);
            double sideways = sample.slip.dot(patch.side);
            Spot spot = random.nextDouble() * (Math.abs(forward) + Math.abs(sideways)) < Math.abs(sideways)
                ? patch.edge(random, sample, Math.signum(sideways))
                : patch.end(random, Math.signum(forward));
            fling(minecraft, level, random, spot, direction, slip, patch.metal);
        }
    }

    private static void landing(Level level, RandomSource random, Patch patch, double speed) {
        double size = patch.halfWidth * 2.0 / STANDARD_WIDTH * Math.max(1.0, patch.length());
        int count = Mth.clamp((int)(speed * LANDING_PER_SPEED * Math.sqrt(size)), 4, MAX_LANDING);
        for (int i = 0; i < count; i++) {
            Spot spot = patch.anywhere(random);
            Vec3 out = patch.travel.scale(random.nextDouble() * 2.0 - 1.0)
                .add(patch.side.scale((random.nextDouble() * 2.0 - 1.0) * 0.5));
            if (patch.metal && BntGround.sparks(spot.ground) && random.nextDouble() < SPARK_SHARE) {
                double scale = LANDING_SPARK_SPEED * (0.5 + random.nextDouble());
                level.addParticle(BitsNTracksParticles.SPARK.get(), spot.pos.x, spot.pos.y + 0.05, spot.pos.z,
                    out.x * scale, SPARK_LIFT * (0.3 + random.nextDouble() * 0.7), out.z * scale);
                continue;
            }
            if (random.nextDouble() < GROUND_SHARE) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, spot.ground), spot.pos.x, spot.pos.y + 0.1, spot.pos.z,
                    out.x, 1.0, out.z);
            }
        }
    }

    private static void fling(Minecraft minecraft, Level level, RandomSource random, Spot spot, Vec3 direction, double slip, boolean metal) {
        boolean spark = metal && BntGround.sparks(spot.ground) && random.nextDouble() < SPARK_SHARE;
        double speed = Mth.clamp(FLING_BASE + slip * FLING_PER_SPEED, FLING_BASE, FLING_MAX) * (spark ? SPARK_SPEED : 1.0);
        double scale = speed * (0.7 + random.nextDouble() * 0.6);
        Vec3 across = new Vec3(-direction.z, 0.0, direction.x).scale((random.nextDouble() * 2.0 - 1.0) * (spark ? SPARK_SPREAD : FLING_SPREAD));
        double lift = (spark ? SPARK_LIFT : FLING_LIFT) * (0.6 + random.nextDouble() * 0.8);
        Vec3 pos = spot.pos.add(direction.scale(0.1));
        Vec3 velocity = direction.scale(scale).add(across);
        if (spark) {
            level.addParticle(BitsNTracksParticles.SPARK.get(), pos.x, pos.y + 0.05, pos.z, velocity.x, lift, velocity.z);
            return;
        }
        if (random.nextDouble() >= GROUND_SHARE
            || minecraft.options.particles().get() == ParticleStatus.DECREASED && random.nextInt(3) == 0) {
            return;
        }
        Particle particle = minecraft.particleEngine.createParticle(
            new BlockParticleOption(ParticleTypes.BLOCK, spot.ground), pos.x, pos.y + 0.1, pos.z, 0.0, 0.0, 0.0);
        if (particle != null) {
            particle.setParticleSpeed(velocity.x, lift, velocity.z);
        }
    }

    private static Vec3 flat(Vec3 vector) {
        return new Vec3(vector.x, 0.0, vector.z);
    }

    private static int pick(RandomSource random, double[] weights, double total) {
        double target = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            target -= weights[i];
            if (target < 0.0 && weights[i] > 0.0) {
                return i;
            }
        }
        for (int i = weights.length - 1; i >= 0; i--) {
            if (weights[i] > 0.0) {
                return i;
            }
        }
        return 0;
    }

    private static void forget(Level level) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Air> states = AIR.values().iterator();
        while (states.hasNext()) {
            if (now - states.next().seen > FORGET_TICKS) {
                states.remove();
            }
        }
    }

    private static int count(RandomSource random, double expected) {
        int whole = (int)expected;
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }

    private record Spot(Vec3 pos, BlockState ground) {
    }

    private record Sample(Vec3 pos, BlockState ground, double along, Vec3 slip, double rolling, double length) {
    }

    private static final class Patch {
        private final List<Sample> samples;
        private final Vec3 travel;
        private final Vec3 side;
        private final double halfWidth;
        private final double reach;
        private final boolean metal;
        private final double vertical;

        private Patch(List<Sample> samples, BntPhysicsEvents.Drive drive, double halfWidth, double reach, boolean metal, double vertical) {
            this.samples = samples;
            this.samples.sort(Comparator.comparingDouble(Sample::along));
            this.travel = drive.travel();
            this.side = drive.side();
            this.halfWidth = halfWidth;
            this.reach = reach;
            this.metal = metal;
            this.vertical = vertical;
        }

        private double length() {
            return this.samples.get(this.samples.size() - 1).along - this.samples.get(0).along + 2.0 * this.reach;
        }

        private Spot anywhere(RandomSource random) {
            Sample sample = this.samples.get(random.nextInt(this.samples.size()));
            return this.at(sample, (random.nextDouble() * 2.0 - 1.0) * this.reach, this.across(random));
        }

        private Spot towardEnd(RandomSource random, double end) {
            if (random.nextDouble() < REAR_SHARE) {
                return this.end(random, end);
            }
            int count = this.samples.size();
            int fromEnd = Math.min(count - 1, (int)(random.nextDouble() * random.nextDouble() * count));
            Sample sample = this.samples.get(end > 0.0 ? count - 1 - fromEnd : fromEnd);
            return this.at(sample, (random.nextDouble() * 2.0 - 1.0) * this.reach, this.across(random));
        }

        private Spot end(RandomSource random, double end) {
            Sample tip = end > 0.0 ? this.samples.get(this.samples.size() - 1) : this.samples.get(0);
            return this.at(tip, end * random.nextDouble() * this.reach, this.across(random));
        }

        private Spot edge(RandomSource random, Sample sample, double sideways) {
            return this.at(sample, (random.nextDouble() * 2.0 - 1.0) * this.reach, sideways * this.halfWidth * (0.75 + random.nextDouble() * 0.25));
        }

        private double across(RandomSource random) {
            return (random.nextDouble() * 2.0 - 1.0) * this.halfWidth;
        }

        private Spot at(Sample sample, double along, double across) {
            return new Spot(sample.pos.add(flat(this.travel).scale(along)).add(flat(this.side).scale(across)), sample.ground);
        }
    }

    private static final class Group {
        private final BlockPos key;
        private final List<BntPhysicsEvents.Drive> drives = new ArrayList<>();
        private final List<BntPhysicsEvents.GroundContact> wheels = new ArrayList<>();
        private boolean belt;
        private SubLevel subLevel;

        private Group(BlockPos key) {
            this.key = key;
        }
    }

    private static final class Air {
        private int ticks;
        private double fall;
        private long seen;
    }
}
