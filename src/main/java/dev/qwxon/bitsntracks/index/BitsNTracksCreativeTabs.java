package dev.qwxon.bitsntracks.index;

import com.kipti.bnb.registry.datagen.BnbCreativeTabs;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.client.BntCreativeTabHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BitsNTracksCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BitsNTracks.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE = REGISTER.register("base", () -> CreativeModeTab.builder()
        .title(Component.translatable("tab.bits_n_tracks.base"))
        .icon(() -> new ItemStack(BitsNTracksItems.TANK_TREAD.get()))
        .withTabsBefore(BnbCreativeTabs.PALETTES_CREATIVE_TAB.getId())
        .displayItems((parameters, output) -> BntCreativeTabHelper.accept(output))
        .build());

    private BitsNTracksCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
