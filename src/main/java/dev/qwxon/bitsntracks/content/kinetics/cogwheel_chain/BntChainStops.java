package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.qwxon.bitsntracks.index.BitsNTracksAdvancements;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber
public final class BntChainStops {
    public static final int RUNAWAY_REENTRIES = 64;
    public static final long OVERSPEED_GRACE = 60L;
    private static final int FLICKER_LIMIT = 128;
    private static final long CALM_TICKS = 20L;
    private static final Map<Level, Pending> PENDING = new WeakHashMap<>();

    private BntChainStops() {
    }

    public static int speedLimit(KineticBlockEntity kinetic, int limit) {
        Level level = kinetic.getLevel();
        Pending pending = level == null || level.isClientSide ? null : PENDING.get(level);
        if (pending == null || pending.overspeeds.isEmpty()) {
            return limit;
        }
        Overspeed overspeed = pending.find(kinetic.getBlockPos());
        if (overspeed == null && kinetic.network != null) {
            overspeed = pending.find(kinetic.network);
        }
        return overspeed != null ? Integer.MAX_VALUE : limit;
    }

    public static boolean overspeed(Level level, KineticBlockEntity kinetic) {
        if (level == null || level.isClientSide || kinetic.getFlickerScore() > FLICKER_LIMIT) {
            return false;
        }
        List<BlockPos> network = BntChainEngagement.chainNetwork(kinetic);
        if (network == null) {
            return false;
        }

        Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
        BlockPos pos = kinetic.getBlockPos().immutable();
        long now = level.getGameTime();
        Overspeed overspeed = pending.find(network);
        if (overspeed == null && kinetic.network != null) {
            overspeed = pending.find(kinetic.network);
        }
        if (overspeed == null) {
            overspeed = new Overspeed(now);
            pending.overspeeds.add(overspeed);
            if (BntDebugLog.enabled()) {
                BntDebugLog.LOG.info("{} spins its track too fast, breaking it in {} ticks unless it slows, network {}",
                    pos.toShortString(), OVERSPEED_GRACE, network.size());
            }
        }
        overspeed.members.addAll(network);
        if (kinetic.network != null) {
            overspeed.networks.add(kinetic.network);
        }
        overspeed.last = now;
        pending.stops.addAll(network);
        return true;
    }

    public static void runaway(Level level, KineticBlockEntity kinetic) {
        if (level == null || level.isClientSide) {
            return;
        }
        BntDebugLog.LOG.warn("Breaking {}: its rotation source kept flipping without settling", kinetic.getBlockPos().toShortString());
        PENDING.computeIfAbsent(level, ignored -> new Pending()).breaks.add(kinetic.getBlockPos().immutable());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        Pending pending = PENDING.get(level);
        if (level.isClientSide || pending == null) {
            return;
        }

        long now = level.getGameTime();
        if (!pending.overspeeds.isEmpty()) {
            int limit = AllConfigs.server().kinetics.maxRotationSpeed.get();
            pending.overspeeds.removeIf(overspeed -> overspeed.settle(level, now, limit, pending));
        }
        if (!pending.breaks.isEmpty()) {
            List<BlockPos> breaks = new ArrayList<>(pending.breaks);
            pending.breaks.clear();
            for (BlockPos pos : breaks) {
                pending.stops.remove(pos);
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof KineticBlockEntity) {
                    level.destroyBlock(pos, true);
                }
            }
        }
        if (!pending.stops.isEmpty()) {
            List<BlockPos> stops = new ArrayList<>(pending.stops);
            pending.stops.clear();
            BntChainEngagement.restore(level, BntChainEngagement.detach(level, stops));
        }
    }

    private static final class Overspeed {
        private final long start;
        private final Set<BlockPos> members = new HashSet<>();
        private final Set<Long> networks = new HashSet<>();
        private long last;
        private KineticBlockEntity fastest;

        private Overspeed(long start) {
            this.start = start;
            this.last = start;
        }

        private boolean settle(Level level, long now, int limit, Pending pending) {
            this.fastest = null;
            Set<Long> scanned = new HashSet<>();
            for (BlockPos pos : List.copyOf(this.members)) {
                if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic)) {
                    continue;
                }
                if (!kinetic.hasNetwork()) {
                    this.check(kinetic, now, limit);
                    continue;
                }
                if (!scanned.add(kinetic.network)) {
                    continue;
                }
                this.networks.add(kinetic.network);
                for (KineticBlockEntity member : kinetic.getOrCreateNetwork().members.keySet()) {
                    this.members.add(member.getBlockPos().immutable());
                    this.check(member, now, limit);
                }
            }
            if (now - this.last > CALM_TICKS) {
                for (BlockPos pos : this.members) {
                    if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic
                        && BntChainEngagement.partOfChain(kinetic) && kinetic.getSpeed() != 0.0F
                        && Math.abs(kinetic.getTheoreticalSpeed()) <= limit) {
                        BitsNTracksAdvancements.CLOSE_SHAVE.awardNearby(level, pos);
                        break;
                    }
                }
                return true;
            }
            if (now - this.start < OVERSPEED_GRACE || this.fastest == null) {
                return false;
            }
            BlockPos broken = this.fastest.getBlockPos().immutable();
            BntDebugLog.LOG.warn("Breaking {}: its track ran too fast for {} ticks", broken.toShortString(), OVERSPEED_GRACE);
            pending.breaks.add(broken);
            pending.stops.addAll(this.members);
            BitsNTracksAdvancements.SPUN_OUT.awardNearby(level, broken);
            return true;
        }

        private void check(KineticBlockEntity kinetic, long now, int limit) {
            float speed = Math.abs(kinetic.getTheoreticalSpeed());
            if (speed <= limit) {
                return;
            }
            this.last = now;
            if (this.fastest == null || faster(kinetic, speed, this.fastest)) {
                this.fastest = kinetic;
            }
        }

        private static boolean faster(KineticBlockEntity kinetic, float speed, KineticBlockEntity than) {
            boolean chained = BntChainEngagement.partOfChain(kinetic);
            boolean thanChained = BntChainEngagement.partOfChain(than);
            return chained != thanChained ? chained : speed > Math.abs(than.getTheoreticalSpeed());
        }
    }

    private static final class Pending {
        private final Set<BlockPos> stops = new LinkedHashSet<>();
        private final Set<BlockPos> breaks = new LinkedHashSet<>();
        private final List<Overspeed> overspeeds = new ArrayList<>();

        private Overspeed find(BlockPos pos) {
            for (Overspeed overspeed : this.overspeeds) {
                if (overspeed.members.contains(pos)) {
                    return overspeed;
                }
            }
            return null;
        }

        private Overspeed find(Long network) {
            for (Overspeed overspeed : this.overspeeds) {
                if (overspeed.networks.contains(network)) {
                    return overspeed;
                }
            }
            return null;
        }

        private Overspeed find(List<BlockPos> network) {
            for (BlockPos pos : network) {
                Overspeed overspeed = this.find(pos);
                if (overspeed != null) {
                    return overspeed;
                }
            }
            return null;
        }
    }
}
