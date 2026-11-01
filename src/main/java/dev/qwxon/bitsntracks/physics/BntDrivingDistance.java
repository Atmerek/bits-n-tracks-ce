package dev.qwxon.bitsntracks.physics;

import com.mojang.serialization.Codec;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.index.BitsNTracksAdvancements;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3dc;

@EventBusSubscriber
public final class BntDrivingDistance {
    public static final double GOAL = 10000.0;
    private static final int INTERVAL = 10;
    private static final double MAX_STEP = 20.0;
    private static final long FRESH = 20L;
    private static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BitsNTracks.MOD_ID);
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Double>> DRIVEN = REGISTER.register("driven",
        () -> AttachmentType.builder(() -> 0.0).serialize(Codec.DOUBLE).copyOnDeath().build());
    private static final Map<ServerSubLevel, Long> MOVING = new WeakHashMap<>();
    private static final Map<ServerSubLevel, Long> MUDDY = new WeakHashMap<>();
    private static final Map<ServerPlayer, Sample> LAST = new WeakHashMap<>();

    private BntDrivingDistance() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    public static void moving(ServerSubLevel subLevel, long now) {
        MOVING.put(subLevel, now);
    }

    public static void muddy(ServerSubLevel subLevel, long now) {
        MUDDY.put(subLevel, now);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 0) {
            return;
        }
        ServerSubLevel subLevel = Sable.HELPER.getTrackingOrVehicleSubLevel(player) instanceof ServerSubLevel riding ? riding : null;
        Long moved = subLevel == null ? null : MOVING.get(subLevel);
        Long mud = subLevel == null ? null : MUDDY.get(subLevel);
        if (mud != null && player.level().getGameTime() - mud <= FRESH) {
            BitsNTracksAdvancements.MUD_SEASON.award(player);
        }
        if (BntDebugLog.enabled() && player.tickCount % (INTERVAL * 10) == 0) {
            BntDebugLog.LOG.info("Driving {}: on a sub-level {}, tracks driving it {} ticks ago, {} blocks so far", player.getScoreboardName(),
                subLevel != null, moved == null ? "never" : player.level().getGameTime() - moved, Math.round(player.getData(DRIVEN)));
        }
        if (moved == null || player.level().getGameTime() - moved > FRESH) {
            LAST.remove(player);
            return;
        }
        Vector3dc position = subLevel.logicalPose().position();
        Sample last = LAST.put(player, new Sample(subLevel, position.x(), position.z()));
        if (last == null || last.subLevel() != subLevel) {
            return;
        }
        double step = Math.hypot(position.x() - last.x(), position.z() - last.z());
        if (step > MAX_STEP) {
            return;
        }
        double driven = player.getData(DRIVEN) + step;
        player.setData(DRIVEN, Math.min(driven, GOAL));
        BitsNTracksAdvancements.ROLLING_THUNDER.award(player, (int)Math.min(driven, GOAL));
    }

    private record Sample(ServerSubLevel subLevel, double x, double z) {
    }
}
