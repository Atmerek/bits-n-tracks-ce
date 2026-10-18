package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.registry.datagen.BnbCreativeTabs;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.client.BntCreativeTabHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({CreativeModeTab.class})
public class BntCreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;
    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @WrapMethod(method = "buildContents")
    private void bnt$buildContents(ItemDisplayParameters parameters, Operation<Void> original) {
        original.call(parameters);
        if ((Object)this != BnbCreativeTabs.BASE_CREATIVE_TAB.get()) {
            return;
        }
        List<ItemStack> displayItems = new ArrayList<>();
        Set<ItemStack> searchItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        BntCreativeTabHelper.processItems(new ArrayList<>(this.displayItems), displayItems, searchItems);
        this.displayItems = displayItems;
        this.displayItemsSearchTab = searchItems;
    }
}
