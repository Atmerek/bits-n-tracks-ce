package dev.qwxon.bitsntracks.index;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Optional;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public enum BitsNTracksAdvancements {
    TRACK("track"),
    INDUSTRIAL_BELT("industrial_belt"),
    TANK_TREAD("tank_tread"),
    WIDE("wide"),
    GROUSERS("grousers"),
    MUD_SEASON("mud_season"),
    PHYSICS("physics"),
    ROUTE("route"),
    TENSION("tension"),
    WIDTH("width"),
    SHAFT("shaft"),
    SUSPENSION_ARM("suspension_arm"),
    BOGIE("bogie"),
    TUNING("tuning"),
    CLOSE_SHAVE("close_shave"),
    SPUN_OUT("spun_out"),
    ROLLING_THUNDER("rolling_thunder");

    private static final double NEARBY = 32.0;
    private static final DeferredRegister<CriterionTrigger<?>> REGISTER = DeferredRegister.create(Registries.TRIGGER_TYPE, BitsNTracks.MOD_ID);
    private static final DeferredHolder<CriterionTrigger<?>, ActionTrigger> ACTION = REGISTER.register("action", ActionTrigger::new);

    private final String id;

    BitsNTracksAdvancements(String id) {
        this.id = id;
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    public void award(Player player) {
        this.award(player, 0);
    }

    public void award(Player player, int progress) {
        if (player instanceof ServerPlayer serverPlayer) {
            ACTION.get().trigger(serverPlayer, this.id, progress);
        }
    }

    public void awardNearby(Level level, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        for (Player player : level.players()) {
            if (player.distanceToSqr(world) <= NEARBY * NEARBY) {
                this.award(player);
            }
        }
    }

    public static final class ActionTrigger extends SimpleCriterionTrigger<ActionTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        private void trigger(ServerPlayer player, String action, int progress) {
            this.trigger(player, instance -> instance.action().equals(action) && progress >= instance.atLeast());
        }

        public record Instance(Optional<ContextAwarePredicate> player, String action, int atLeast) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("action").forGetter(Instance::action),
                Codec.INT.optionalFieldOf("at_least", 0).forGetter(Instance::atLeast)
            ).apply(instance, Instance::new));
        }
    }
}
