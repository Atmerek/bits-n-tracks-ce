package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qwxon.bitsntracks.client.BntCreativeTabHelper;
import dev.qwxon.bitsntracks.index.BitsNTracksCreativeTabs;
import java.util.Collection;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({CreativeModeTab.class})
public class BntCreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @WrapMethod(method = "buildContents")
    private void bnt$buildContents(ItemDisplayParameters parameters, Operation<Void> original) {
        original.call(parameters);
        if ((Object)this == BitsNTracksCreativeTabs.BASE.get()) {
            this.displayItems = BntCreativeTabHelper.layout(this.displayItems);
        }
    }
}
