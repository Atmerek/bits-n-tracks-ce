package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class BntCreativeTabHelper {
    private static final int ROW = 9;
    private static final List<Supplier<? extends ItemLike>> ITEMS = List.of(
        BitsNTracksItems.INDUSTRIAL_BELT,
        BitsNTracksItems.TANK_TREAD,
        BitsNTracksItems.COG_ALIGNMENT_LEVER,
        BitsNTracksItems.SUSPENSION_TOOL,
        BitsNTracksItems.SUSPENSION_PIECE,
        BitsNTracksBlocks.TINY_FLANGED_COGWHEEL,
        BitsNTracksBlocks.INTERMEDIATE_FLANGED_COGWHEEL,
        BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL,
        BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL,
        BitsNTracksBlocks.MEDIUM_LARGE_FLANGED_COGWHEEL,
        BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL,
        BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL,
        BitsNTracksBlocks.INTERMEDIATE_INDUSTRIAL_FLANGED_COGWHEEL,
        BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL,
        BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL,
        BitsNTracksBlocks.MEDIUM_LARGE_INDUSTRIAL_FLANGED_COGWHEEL,
        BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL,
        BitsNTracksBlocks.QWXONN_HEAD,
        BitsNTracksBlocks.ATMEREK_HEAD,
        BitsNTracksBlocks.CUBESTER_HEAD,
        BitsNTracksBlocks.ALESRR_HEAD,
        BitsNTracksBlocks.SPECIAL_THANKS_HEAD
    );

    public static int currentRow;
    public static int tracksRow;

    private BntCreativeTabHelper() {
    }

    public static void processItems(List<ItemStack> baseItems, List<ItemStack> displayItems, Set<ItemStack> searchItems) {
        int baseCount = 0;
        for (ItemStack item : baseItems) {
            if (!item.isEmpty()) {
                displayItems.add(item);
                searchItems.add(item);
                baseCount++;
            }
        }

        int padding = ROW - baseCount % ROW;
        if (padding < ROW) {
            padding += ROW;
        }
        for (int i = 0; i < padding; i++) {
            displayItems.add(ItemStack.EMPTY);
        }
        tracksRow = (baseCount + ROW - 1) / ROW + padding / ROW - 1;

        for (Supplier<? extends ItemLike> entry : ITEMS) {
            ItemStack item = new ItemStack(entry.get());
            displayItems.add(item);
            searchItems.add(item);
        }
    }
}
