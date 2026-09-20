package dev.qwxon.bitsntracks.content;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.qwxon.bitsntracks.client.SuspensionToolItemRenderer;
import dev.qwxon.bitsntracks.index.BitsNTracksDataComponents;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class SuspensionToolItem extends Item {
    public SuspensionToolItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new SuspensionToolItemRenderer()));
    }

    public static BntTuning setting(ItemStack stack) {
        return BntTuning.byIndex(stack.getOrDefault(BitsNTracksDataComponents.TUNING_SETTING.get(), 0));
    }

    public static boolean wholeTrack(ItemStack stack) {
        return stack.getOrDefault(BitsNTracksDataComponents.TUNING_WHOLE_TRACK.get(), false);
    }

    public static void setMode(ItemStack stack, BntTuning setting, boolean wholeTrack) {
        stack.set(BitsNTracksDataComponents.TUNING_SETTING.get(), setting.ordinal());
        stack.set(BitsNTracksDataComponents.TUNING_WHOLE_TRACK.get(), wholeTrack);
    }

    public static MutableComponent scopeName(boolean wholeTrack) {
        return Component.translatable(wholeTrack ? "bits_n_tracks.scope.track" : "bits_n_tracks.scope.cogwheel");
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bits_n_tracks.tuning.setting",
                Component.translatable(setting(stack).translationKey()).withStyle(ChatFormatting.GOLD))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bits_n_tracks.tuning.scope",
                scopeName(wholeTrack(stack)).withStyle(ChatFormatting.GOLD))
            .withStyle(ChatFormatting.GRAY));
    }
}
