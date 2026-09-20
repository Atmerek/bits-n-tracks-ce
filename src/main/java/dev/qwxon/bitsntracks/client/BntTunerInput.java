package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.interaction.BntAlignmentModePayload;
import dev.qwxon.bitsntracks.interaction.BntTuningModePayload;
import dev.qwxon.bitsntracks.interaction.BntTuningPayload;
import dev.qwxon.bitsntracks.physics.BntTuning;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class BntTunerInput {
    private static boolean attackHandled;
    private static boolean useHandled;
    private static boolean attackWasDown;
    private static boolean useWasDown;

    private BntTunerInput() {
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof CogAlignmentLeverItem) {
            if (event.isAttack() && Screen.hasControlDown()) {
                event.setCanceled(true);
                event.setSwingHand(false);
                if (!attackHandled && !attackWasDown) {
                    attackHandled = true;
                    changeAlignmentScope(minecraft, stack);
                }
            }
            return;
        }
        if (!(stack.getItem() instanceof SuspensionToolItem)) {
            return;
        }

        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!attackHandled && !attackWasDown) {
                attackHandled = true;
                changeMode(minecraft, player, stack);
            }
        } else if (event.isUseItem()) {
            BlockPos pos = BntTunerGauge.tunableTarget();
            if (pos == null) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!useHandled && !useWasDown) {
                useHandled = true;
                tune(minecraft, player, stack, pos);
            }
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        attackWasDown = minecraft.options.keyAttack.isDown();
        useWasDown = minecraft.options.keyUse.isDown();
        attackHandled = false;
        useHandled = false;
    }

    private static void changeAlignmentScope(Minecraft minecraft, ItemStack stack) {
        boolean wholeTrack = !CogAlignmentLeverItem.wholeTrack(stack);
        CogAlignmentLeverItem.setWholeTrack(stack, wholeTrack);
        PacketDistributor.sendToServer(new BntAlignmentModePayload(wholeTrack));
        minecraft.gui.setOverlayMessage(
            Component.translatable("chat.bits_n_tracks.alignment.scope.selected",
                    CogAlignmentLeverItem.scopeName(wholeTrack))
                .withStyle(ChatFormatting.GOLD),
            false);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), wholeTrack ? 1.2F : 1.6F, 0.25F));
    }

    private static void changeMode(Minecraft minecraft, LocalPlayer player, ItemStack stack) {
        BntTuning setting = SuspensionToolItem.setting(stack);
        boolean wholeTrack = SuspensionToolItem.wholeTrack(stack);
        if (Screen.hasControlDown()) {
            wholeTrack = !wholeTrack;
        } else {
            setting = setting.cycle(player.isShiftKeyDown() ? 1 : -1);
        }

        SuspensionToolItem.setMode(stack, setting, wholeTrack);
        PacketDistributor.sendToServer(new BntTuningModePayload(setting.ordinal(), wholeTrack));
        minecraft.gui.setOverlayMessage(
            Component.translatable("chat.bits_n_tracks.tuning.selected",
                    Component.translatable(setting.translationKey()), SuspensionToolItem.scopeName(wholeTrack))
                .withStyle(ChatFormatting.GOLD),
            false);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), wholeTrack ? 1.2F : 1.6F, 0.25F));
    }

    private static void tune(Minecraft minecraft, LocalPlayer player, ItemStack stack, BlockPos pos) {
        BntTuning setting = SuspensionToolItem.setting(stack);
        int step = player.isShiftKeyDown() ? -1 : 1;
        int current = BntTunerGauge.readingAt(pos, setting);
        int next = BntTuning.clamp(current + step);
        if (next == current) {
            BntTunerGauge.kick(step);
        }

        PacketDistributor.sendToServer(new BntTuningPayload(pos, setting.ordinal(), step, SuspensionToolItem.wholeTrack(stack)));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.COMPARATOR_CLICK, 0.7F + 0.08F * next, 0.4F));
    }
}
