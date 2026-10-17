package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class BntWheelEffects {
    private static final double RANGE = 48.0;
    private static final double MIN_ROLLING = 0.5;
    private static final double DUST_PER_SPEED = 0.3;
    private static final double MIN_SLIP = 1.0;
    private static final double SLIP_PER_SPEED = 0.6;
    private static final int MAX_SLIP = 10;
    private static final double FLING_BASE = 0.2;
    private static final double FLING_PER_SPEED = 0.03;
    private static final double FLING_MAX = 0.5;
    private static final double FLING_LIFT = 0.25;
    private static final double FLING_SPREAD = 0.05;
    private static final double MIN_LANDING = 2.0;
    private static final double LANDING_PER_SPEED = 3.0;
    private static final int MAX_LANDING = 40;
    private static final int MIN_AIR_TICKS = 2;
    private static final double HALF_WIDTH = 0.3;
    private static final double END_REACH = 0.15;
    private static final double REAR_SHARE = 0.35;
    private static final int FORGET_TICKS = 40;
    private static final Map<BlockPos, List<BntPhysicsEvents.GroundContact>> PENDING = new HashMap<>();
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

        BntPhysicsEvents.GroundContact contact = BntPhysicsEvents.groundContact(kbe, subLevel);
        if (contact == null) {
            return;
        }
        if (pendingLevel != level) {
            PENDING.clear();
            pendingLevel = level;
        }
        PENDING.computeIfAbsent(trackKey(kbe), ignored -> new ArrayList<>()).add(contact);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
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
        for (Map.Entry<BlockPos, List<BntPhysicsEvents.GroundContact>> entry : PENDING.entrySet()) {
            Air air = AIR.computeIfAbsent(entry.getKey(), ignored -> new Air());
            air.seen = now;
            track(minecraft, level, random, entry.getValue(), air);
        }
        PENDING.clear();
        forget(level);
    }

    private static void track(
        Minecraft minecraft, Level level, RandomSource random, List<BntPhysicsEvents.GroundContact> contacts, Air air
    ) {
        List<BntPhysicsEvents.GroundContact> grounded = new ArrayList<>();
        for (BntPhysicsEvents.GroundContact contact : contacts) {
            if (contact.grounded()) {
                grounded.add(contact);
            } else {
                air.fall = Math.min(air.fall, contact.vertical());
            }
        }
        if (grounded.isEmpty()) {
            air.ticks++;
            return;
        }

        Footprint footprint = new Footprint(grounded);
        if (air.ticks >= MIN_AIR_TICKS && -air.fall >= MIN_LANDING) {
            landing(level, random, footprint, -air.fall);
        }
        air.ticks = 0;
        air.fall = 0.0;

        double rolling = 0.0;
        double target = 0.0;
        for (BntPhysicsEvents.GroundContact contact : grounded) {
            rolling += contact.rolling();
            target += contact.target();
        }
        rolling /= grounded.size();
        target /= grounded.size();
        double width = footprint.width() / (2.0 * HALF_WIDTH);

        if (Math.abs(rolling) >= MIN_ROLLING) {
            int count = count(random, Math.abs(rolling) * DUST_PER_SPEED * width);
            double rear = -Math.signum(rolling);
            Vec3 thrown = footprint.travel.scale(rear);
            for (int i = 0; i < count; i++) {
                Spot spot = footprint.towardEnd(random, rear);
                if (spot != null) {
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, spot.ground), spot.pos.x, spot.pos.y + 0.1, spot.pos.z,
                        thrown.x, 1.5, thrown.z);
                }
            }
        }

        double slip = target - rolling;
        if (Math.abs(slip) >= MIN_SLIP) {
            int count = Math.min(MAX_SLIP, count(random, Math.abs(slip) * SLIP_PER_SPEED * width));
            double speed = Mth.clamp(FLING_BASE + Math.abs(slip) * FLING_PER_SPEED, FLING_BASE, FLING_MAX);
            double rear = -Math.signum(slip);
            Vec3 back = footprint.travel.scale(rear);
            for (int i = 0; i < count; i++) {
                Spot spot = footprint.atEnd(random, rear);
                if (spot != null) {
                    fling(minecraft, random, spot, back, footprint.side, speed);
                }
            }
        }
    }

    private static void landing(Level level, RandomSource random, Footprint footprint, double speed) {
        double size = footprint.width() / (2.0 * HALF_WIDTH) * Math.max(1.0, footprint.length());
        int count = Mth.clamp((int)(speed * LANDING_PER_SPEED * Math.sqrt(size)), 4, MAX_LANDING);
        for (int i = 0; i < count; i++) {
            Spot spot = footprint.anywhere(random);
            if (spot == null) {
                continue;
            }
            Vec3 out = footprint.travel.scale(random.nextDouble() * 2.0 - 1.0)
                .add(footprint.side.scale((random.nextDouble() * 2.0 - 1.0) * 0.5));
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, spot.ground), spot.pos.x, spot.pos.y + 0.1, spot.pos.z,
                out.x, 1.0, out.z);
        }
    }

    private static void fling(Minecraft minecraft, RandomSource random, Spot spot, Vec3 back, Vec3 side, double speed) {
        if (minecraft.options.particles().get() == ParticleStatus.DECREASED && random.nextInt(3) == 0) {
            return;
        }
        Vec3 pos = spot.pos.add(back.scale(0.1));
        Particle particle = minecraft.particleEngine.createParticle(
            new BlockParticleOption(ParticleTypes.BLOCK, spot.ground), pos.x, pos.y + 0.1, pos.z, 0.0, 0.0, 0.0);
        if (particle != null) {
            double scale = speed * (0.7 + random.nextDouble() * 0.6);
            Vec3 jitter = side.scale((random.nextDouble() * 2.0 - 1.0) * FLING_SPREAD);
            particle.setParticleSpeed(
                back.x * scale + jitter.x, FLING_LIFT * (0.6 + random.nextDouble() * 0.8), back.z * scale + jitter.z);
        }
    }

    private static BlockPos trackKey(KineticBlockEntity kbe) {
        KineticBlockEntity track = BntCogwheelPairing.beltTwin(kbe);
        if (track.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour && behaviour.isPartOfChain()) {
            if (behaviour.getControlledChain() != null) {
                return track.getBlockPos();
            }
            if (behaviour.getControllerOffset() != null) {
                return track.getBlockPos().offset(behaviour.getControllerOffset());
            }
        }
        return kbe.getBlockPos();
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

    private static final class Footprint {
        private final Level level;
        private final List<BntPhysicsEvents.GroundContact> contacts;
        private final double[] along;
        private final Vec3 origin;
        private final Vec3 travel;
        private final Vec3 side;
        private final double sideMin;
        private final double sideMax;

        private Footprint(List<BntPhysicsEvents.GroundContact> grounded) {
            this.level = pendingLevel;
            this.origin = grounded.get(0).point();
            this.travel = grounded.get(0).travel();
            this.side = grounded.get(0).side();
            this.contacts = new ArrayList<>(grounded);
            this.contacts.sort(Comparator.comparingDouble(contact -> contact.point().subtract(this.origin).dot(this.travel)));
            this.along = new double[this.contacts.size()];
            double low = Double.MAX_VALUE;
            double high = -Double.MAX_VALUE;
            for (int i = 0; i < this.contacts.size(); i++) {
                Vec3 offset = this.contacts.get(i).point().subtract(this.origin);
                this.along[i] = offset.dot(this.travel);
                double across = offset.dot(this.side);
                low = Math.min(low, across);
                high = Math.max(high, across);
            }
            this.sideMin = low - HALF_WIDTH;
            this.sideMax = high + HALF_WIDTH;
        }

        private double width() {
            return this.sideMax - this.sideMin;
        }

        private double length() {
            return this.along[this.along.length - 1] - this.along[0] + 2.0 * END_REACH;
        }

        private Spot anywhere(RandomSource random) {
            return this.at(Mth.lerp(random.nextDouble(), this.along[0] - END_REACH, this.along[this.along.length - 1] + END_REACH), random);
        }

        private Spot towardEnd(RandomSource random, double end) {
            if (random.nextDouble() < REAR_SHARE) {
                return this.atEnd(random, end);
            }
            double reach = random.nextDouble() * random.nextDouble();
            double start = end < 0.0 ? this.along[0] - END_REACH : this.along[this.along.length - 1] + END_REACH;
            return this.at(start - end * reach * this.length(), random);
        }

        private Spot atEnd(RandomSource random, double end) {
            double tip = end < 0.0 ? this.along[0] - END_REACH : this.along[this.along.length - 1] + END_REACH;
            return this.at(tip - end * random.nextDouble() * END_REACH, random);
        }

        private Spot at(double s, RandomSource random) {
            int next = 0;
            while (next < this.along.length && this.along[next] < s) {
                next++;
            }
            int before = Math.max(0, next - 1);
            int after = Math.min(this.along.length - 1, next);
            double span = this.along[after] - this.along[before];
            double t = span < 1.0E-6 ? 0.0 : Mth.clamp((s - this.along[before]) / span, 0.0, 1.0);
            BntPhysicsEvents.GroundContact from = this.contacts.get(before);
            BntPhysicsEvents.GroundContact to = this.contacts.get(after);
            double height = Mth.lerp(t, from.point().subtract(this.origin).dot(new Vec3(0.0, 1.0, 0.0)),
                to.point().subtract(this.origin).dot(new Vec3(0.0, 1.0, 0.0)));
            double across = Mth.lerp(random.nextDouble(), this.sideMin, this.sideMax);
            Vec3 pos = this.origin.add(this.travel.scale(s)).add(this.side.scale(across));
            pos = new Vec3(pos.x, this.origin.y + height, pos.z);
            BlockState ground = this.level.getBlockState(t < 0.5 ? from.ground() : to.ground());
            return ground.getRenderShape() == RenderShape.INVISIBLE ? null : new Spot(pos, ground);
        }
    }

    private static final class Air {
        private int ticks;
        private double fall;
        private long seen;
    }
}
