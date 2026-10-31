package dev.qwxon.bitsntracks.index;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BitsNTracksParticles {
    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, BitsNTracks.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK =
        REGISTER.register("spark", () -> new SimpleParticleType(false));

    private BitsNTracksParticles() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
