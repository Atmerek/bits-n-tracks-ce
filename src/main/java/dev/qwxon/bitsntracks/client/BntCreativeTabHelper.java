package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class BntCreativeTabHelper {
    private static final int ROW = 9;
    private static final List<Supplier<? extends ItemLike>> TRACKS = List.of(
        BitsNTracksItems.INDUSTRIAL_BELT,
        BitsNTracksItems.TANK_TREAD,
        BitsNTracksItems.GROUSER,
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
        BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL
    );
    private static final List<Supplier<? extends ItemLike>> TEAM = List.of(
        BitsNTracksBlocks.QWXONN_HEAD,
        BitsNTracksBlocks.ATMEREK_HEAD,
        BitsNTracksBlocks.CUBESTER_HEAD,
        BitsNTracksBlocks.ALESRR_HEAD,
        BitsNTracksBlocks.HANAHAKI_HEAD,
        BitsNTracksBlocks.SPECIAL_THANKS_HEAD
    );

    public static int currentRow;
    public static int tracksRow;
    public static int teamRow;

    private BntCreativeTabHelper() {
    }

    public static void accept(CreativeModeTab.Output output) {
        for (Supplier<? extends ItemLike> entry : TRACKS) {
            output.accept(entry.get());
        }
        for (Supplier<? extends ItemLike> entry : TEAM) {
            output.accept(entry.get());
        }
    }

    public static List<ItemStack> layout(Collection<ItemStack> items) {
        List<ItemStack> team = new ArrayList<>();
        List<ItemStack> tracks = new ArrayList<>();
        for (ItemStack item : items) {
            (isTeam(item) ? team : tracks).add(item);
        }

        List<ItemStack> slots = new ArrayList<>();
        tracksRow = 0;
        bannerRow(slots);
        slots.addAll(tracks);
        endRow(slots);
        teamRow = slots.size() / ROW;
        bannerRow(slots);
        slots.addAll(team);
        return slots;
    }

    private static boolean isTeam(ItemStack item) {
        for (Supplier<? extends ItemLike> entry : TEAM) {
            if (item.is(entry.get().asItem())) {
                return true;
            }
        }
        return false;
    }

    private static void bannerRow(List<ItemStack> slots) {
        for (int i = 0; i < ROW; i++) {
            slots.add(ItemStack.EMPTY);
        }
    }

    private static void endRow(List<ItemStack> slots) {
        while (slots.size() % ROW != 0) {
            slots.add(ItemStack.EMPTY);
        }
    }
}
