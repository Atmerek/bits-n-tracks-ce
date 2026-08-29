package dev.qwxon.bitsntracks.index;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import net.minecraft.world.item.Item;

public class BitsNTracksItems {
    public static final ItemEntry<Item> INDUSTRIAL_BELT = BitsNTracks.REGISTRATE.item("industrial_belt", Item::new).register();
    public static final ItemEntry<CogAlignmentLeverItem> COG_ALIGNMENT_LEVER = BitsNTracks.REGISTRATE
        .item("cog_alignment_lever", CogAlignmentLeverItem::new)
        .register();

    public static void init() {
    }
}
