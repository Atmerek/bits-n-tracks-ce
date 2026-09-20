package dev.qwxon.bitsntracks.index;

import com.mojang.serialization.Codec;
import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BitsNTracksDataComponents {
    private static final DeferredRegister.DataComponents REGISTER =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BitsNTracks.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TUNING_SETTING =
        REGISTER.registerComponentType("tuning_setting", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TUNING_WHOLE_TRACK =
        REGISTER.registerComponentType("tuning_whole_track", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ALIGNMENT_WHOLE_TRACK =
        REGISTER.registerComponentType("alignment_whole_track", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private BitsNTracksDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
