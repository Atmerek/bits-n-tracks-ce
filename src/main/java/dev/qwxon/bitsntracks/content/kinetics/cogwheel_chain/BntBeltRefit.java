package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber
public final class BntBeltRefit {
    private static final long DELAY = 2L;
    private static final Map<Level, Map<BlockPos, Long>> PENDING = new WeakHashMap<>();

    private BntBeltRefit() {
    }

    public static void queue(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) {
            return;
        }
        PENDING.computeIfAbsent(level, ignored -> new LinkedHashMap<>()).put(pos.immutable(), level.getGameTime() + DELAY);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        Map<BlockPos, Long> pending = PENDING.get(level);
        if (level.isClientSide || pending == null || pending.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Set<BlockPos> chains = new HashSet<>();
        Iterator<Map.Entry<BlockPos, Long>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (entry.getValue() > now) {
                continue;
            }
            iterator.remove();
            BlockPos pos = entry.getKey();
            if (level.isLoaded(pos) && chains.add(BntBeltTension.controllerPos(level, pos))) {
                BntBeltTension.refit(level, pos);
            }
        }
    }
}
