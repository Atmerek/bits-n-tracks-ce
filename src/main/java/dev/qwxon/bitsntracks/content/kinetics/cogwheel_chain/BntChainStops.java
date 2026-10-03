package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final int FLICKER_LIMIT = 128;
    private static final long STRIKE_WINDOW = 100L;
    private static final int STRIKE_LIMIT = 3;
    private static final Map<Level, Pending> PENDING = new WeakHashMap<>();

    private BntChainStops() {
    }

    public static boolean intercept(Level level, KineticBlockEntity kinetic) {
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
        Strike strike = pending.strikes.get(pos);
        if (strike != null && strike.last() == now) {
            return true;
        }

        int count = strike != null && now - strike.first() <= STRIKE_WINDOW ? strike.count() + 1 : 1;
        if (count > STRIKE_LIMIT) {
            pending.strikes.remove(pos);
            BntDebugLog.LOG.warn("Let Create break {}: its network kept failing after {} restarts", pos.toShortString(), STRIKE_LIMIT);
            return false;
        }

        pending.strikes.put(pos, new Strike(count == 1 ? now : strike.first(), now, count));
        pending.stops.addAll(network);
        if (BntDebugLog.enabled()) {
            BntDebugLog.LOG.info("stopping {} instead of letting Create break it, speed {} network {} strike {}",
                pos.toShortString(), kinetic.getTheoreticalSpeed(), network.size(), count);
        }
        return true;
    }

    public static void runaway(Level level, KineticBlockEntity kinetic) {
        if (level == null || level.isClientSide || intercept(level, kinetic)) {
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
        pending.strikes.values().removeIf(strike -> now - strike.last() > STRIKE_WINDOW);
        if (!pending.stops.isEmpty()) {
            List<BlockPos> stops = new ArrayList<>(pending.stops);
            pending.stops.clear();
            BntChainEngagement.restore(level, BntChainEngagement.detach(level, stops));
        }
        if (!pending.breaks.isEmpty()) {
            List<BlockPos> breaks = new ArrayList<>(pending.breaks);
            pending.breaks.clear();
            for (BlockPos pos : breaks) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof KineticBlockEntity) {
                    level.destroyBlock(pos, true);
                }
            }
        }
    }

    private record Strike(long first, long last, int count) {
    }

    private static final class Pending {
        private final Set<BlockPos> stops = new LinkedHashSet<>();
        private final Set<BlockPos> breaks = new LinkedHashSet<>();
        private final Map<BlockPos, Strike> strikes = new HashMap<>();
    }
}
