package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.registry.datagen.BnbCreativeTabs;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.client.BntCreativeTabHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({CreativeModeTab.class})
public class BntCreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;
    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @WrapMethod(
        method = {"buildContents"}
    )
    private void bitsntracks$buildContents(ItemDisplayParameters parameters, Operation<Void> original) {
        CreativeModeTab self = (CreativeModeTab)(Object)this;
        if (self == BnbCreativeTabs.BASE_CREATIVE_TAB.get()) {
            original.call(new Object[]{parameters});
            List<ItemStack> baseItems = new ArrayList<>(this.displayItems);
            List<ItemStack> newDisplayItems = new LinkedList<>();
            Set<ItemStack> newSearchItems = new LinkedHashSet<>();
            BntCreativeTabHelper.processItems(baseItems, newDisplayItems, newSearchItems);
            this.displayItems = newDisplayItems;
            this.displayItemsSearchTab = newSearchItems;
        } else {
            original.call(new Object[]{parameters});
        }
    }
}
