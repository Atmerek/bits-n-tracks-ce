package dev.qwxon.bitsntracks.index;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlockEntity;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelRenderer;
import dev.qwxon.bitsntracks.content.HiddenCogwheelRenderer;

public class BitsNTracksBlockEntityTypes {
    public static final BlockEntityEntry<KineticBlockEntity> HIDDEN_COGWHEEL = BitsNTracks.REGISTRATE
        .blockEntity("hidden_cogwheel", KineticBlockEntity::new)
        .validBlocks(
            BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL,
            BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL,
            BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL,
            BitsNTracksBlocks.LARGE_HIDDEN_FLANGED_COGWHEEL
        )
        .renderer(() -> HiddenCogwheelRenderer::new)
        .register();
    public static final BlockEntityEntry<BntFlangedCogwheelBlockEntity> SIMPLE_KINETIC = BitsNTracks.REGISTRATE
        .blockEntity("bnt_flanged_cogwheel", BntFlangedCogwheelBlockEntity::new)
        .validBlocks(
            BitsNTracksBlocks.TINY_FLANGED_COGWHEEL,
            BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL,
            BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL,
            BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL,
            BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL,
            BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL,
            BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL,
            BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL
        )
        .renderer(() -> BntFlangedCogwheelRenderer::new)
        .register();

    public static void init() {
    }
}
