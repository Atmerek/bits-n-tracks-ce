package dev.qwxon.bitsntracks;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.qwxon.bitsntracks.client.BntShaderHand;
import dev.qwxon.bitsntracks.client.BntSuspensionPlacement;
import dev.qwxon.bitsntracks.client.BntToolHud;
import dev.qwxon.bitsntracks.client.BntToolInput;
import dev.qwxon.bitsntracks.client.BntToolOutlines;
import dev.qwxon.bitsntracks.client.BntToolSpin;
import dev.qwxon.bitsntracks.client.BntTunerGauge;
import dev.qwxon.bitsntracks.client.BntWheelEffects;
import dev.qwxon.bitsntracks.client.CogAlignmentLeverItemRenderer;
import dev.qwxon.bitsntracks.client.SuspensionToolItemRenderer;
import dev.qwxon.bitsntracks.client.flywheel.BntFlywheel;
import dev.qwxon.bitsntracks.client.ponder.BntPonderPlugin;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelRenderer;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
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
        modEventBus.addListener(BitsNTracksClient::registerClientExtensions);
        modEventBus.addListener(BntToolHud::registerLayers);
        NeoForge.EVENT_BUS.addListener(BntToolOutlines::onHighlight);
        NeoForge.EVENT_BUS.addListener(BntToolInput::onInteraction);
        NeoForge.EVENT_BUS.addListener(BntToolInput::onScroll);
        NeoForge.EVENT_BUS.addListener(BntSuspensionPlacement::onInteraction);
        NeoForge.EVENT_BUS.addListener(BntSuspensionPlacement::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntToolInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntTunerGauge::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntTunerGauge::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(BntToolSpin::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(BntWheelEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(BntShaderHand::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(BntFlywheel::onRenderFrame);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new BntPonderPlugin()));
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get(), BntFlangedCogwheelRenderer::new);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        CogAlignmentLeverItem lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.get();
        event.registerItem(SimpleCustomRenderer.create(lever, new CogAlignmentLeverItemRenderer()), lever);
        SuspensionToolItem tuner = BitsNTracksItems.SUSPENSION_TOOL.get();
        event.registerItem(SimpleCustomRenderer.create(tuner, new SuspensionToolItemRenderer()), tuner);
    }
}
