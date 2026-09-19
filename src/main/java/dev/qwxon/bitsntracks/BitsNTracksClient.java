package dev.qwxon.bitsntracks;

import dev.qwxon.bitsntracks.client.BntClientKeyMappings;
import dev.qwxon.bitsntracks.client.BntShaderHand;
import dev.qwxon.bitsntracks.client.BntTunerGauge;
import dev.qwxon.bitsntracks.client.BntTunerInput;
import dev.qwxon.bitsntracks.client.ponder.BntPonderPlugin;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelRenderer;
import dev.qwxon.bitsntracks.content.HiddenCogwheelRenderer;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(
    value = "bits_n_tracks",
    dist = {Dist.CLIENT}
)
public class BitsNTracksClient {
    public BitsNTracksClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(BitsNTracksClient::registerRenderers);
        modEventBus.addListener(BitsNTracksClient::onClientSetup);
        NeoForge.EVENT_BUS.addListener(BntClientKeyMappings::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntTunerInput::onInteraction);
        NeoForge.EVENT_BUS.addListener(BntTunerInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntTunerGauge::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntTunerGauge::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(BntShaderHand::onRenderFrame);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new BntPonderPlugin()));
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)BitsNTracksBlockEntityTypes.HIDDEN_COGWHEEL.get(), HiddenCogwheelRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get(), BntFlangedCogwheelRenderer::new);
    }
}
