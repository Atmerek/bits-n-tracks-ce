package dev.qwxon.bitsntracks;

import dev.qwxon.bitsntracks.client.BntClientKeyMappings;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelRenderer;
import dev.qwxon.bitsntracks.content.HiddenCogwheelRenderer;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        NeoForge.EVENT_BUS.addListener(BntClientKeyMappings::onClientTick);
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)BitsNTracksBlockEntityTypes.HIDDEN_COGWHEEL.get(), HiddenCogwheelRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get(), BntFlangedCogwheelRenderer::new);
    }
}
