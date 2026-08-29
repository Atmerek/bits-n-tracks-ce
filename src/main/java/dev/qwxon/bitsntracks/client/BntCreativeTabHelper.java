package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class BntCreativeTabHelper {
    public static int CURRENT_ROW = 0;
    public static int BNB_ROW = 0;
    public static int BNT_ROW = 0;

    public static void processItems(List<ItemStack> baseItems, List<ItemStack> displayItems, Set<ItemStack> searchItems) {
        BNB_ROW = 0;
        int bnbCount = 0;

        for (ItemStack item : baseItems) {
            if (!item.isEmpty()) {
                displayItems.add(item);
                searchItems.add(item);
                bnbCount++;
            }
        }

        int bnbPadding = 9 - bnbCount % 9;
        if (bnbPadding < 9) {
            bnbPadding += 9;
        }

        for (int i = 0; i < bnbPadding; i++) {
            displayItems.add(ItemStack.EMPTY);
        }

        BNT_ROW = (int)Math.ceil(bnbCount / 9.0) + bnbPadding / 9 - 1;

        for (ItemStack itemx : List.of(
            new ItemStack((ItemLike)BitsNTracksItems.INDUSTRIAL_BELT.get()),
            new ItemStack((ItemLike)BitsNTracksItems.COG_ALIGNMENT_LEVER.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get()),
            new ItemStack((ItemLike)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get())
        )) {
            displayItems.add(itemx);
            searchItems.add(itemx);
        }
    }
}
