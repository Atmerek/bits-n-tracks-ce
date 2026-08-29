package dev.qwxon.bitsntracks;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import dev.qwxon.bitsntracks.client.BntClientConfig;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntStressValues;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod("bits_n_tracks")
public class BitsNTracks {
    public static final String MOD_ID = "bits_n_tracks";
    public static final String NAME = "Bits 'n' Tracks";
    public static final CreateRegistrate REGISTRATE = (CreateRegistrate)CreateRegistrate.create("bits_n_tracks").defaultCreativeTab((ResourceKey)null);

    public BitsNTracks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(Type.CLIENT, BntClientConfig.SPEC);
        modContainer.registerConfig(Type.SERVER, BntPhysicsTuning.SPEC);
        REGISTRATE.setTooltipModifierFactory(item -> new Modifier(item, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));
        BitsNTracksItems.init();
        BitsNTracksBlocks.init();
        BntStressValues.register();
        BitsNTracksBlockEntityTypes.init();
        BntCogwheelChainTypes.init(modEventBus);
        REGISTRATE.registerEventListeners(modEventBus);
        BntPhysicsEvents.register();
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath("bits_n_tracks", path);
    }
}
