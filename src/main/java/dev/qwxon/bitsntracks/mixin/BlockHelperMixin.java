package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockHelper.class, remap = false)
public abstract class BlockHelperMixin {
    @WrapMethod(method = "placeSchematicBlock")
    private static void bnt$markPrinted(Level level, BlockState state, BlockPos target, ItemStack stack, CompoundTag data,
                                        Operation<Void> original) {
        BntCogwheels.print(() -> original.call(level, state, target, stack, data));
    }
}
