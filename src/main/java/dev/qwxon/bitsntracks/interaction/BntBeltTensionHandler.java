package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.client.BntBeltClick;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

@EventBusSubscriber
public final class BntBeltTensionHandler {
    private static final double MAX_REACH = 64.0;

    private BntBeltTensionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        if (tryTension(event.getLevel(), event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(RightClickItem event) {
        if (tryTension(event.getLevel(), event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    private static boolean tryTension(Level level, Player player, ItemStack stack) {
        return player != null
            && level.isClientSide
            && stack.is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())
            && BntBeltClick.send(player);
    }

    /** Applies one lever click to the whole chain. */
    public static void applyFromClient(Player player, BntBeltTensionPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos controllerPos = payload.controllerPos();
        if (!level.isLoaded(controllerPos) || !withinReach(level, player, controllerPos)) {
            return;
        }

        float tension = BntBeltTension.step(level, controllerPos, payload.tighten());
        player.displayClientMessage(message(tension), true);
    }

    private static boolean withinReach(Level level, Player player, BlockPos controllerPos) {
        Vec3 centre = Vec3.atCenterOf(controllerPos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        return player.position().distanceToSqr(world) <= MAX_REACH * MAX_REACH;
    }

    private static Component message(float tension) {
        Component label;
        ChatFormatting colour;
        if (tension >= BntBeltTension.MAX) {
            label = Component.translatable("chat.bits_n_tracks.belt_tension.taut");
            colour = ChatFormatting.GREEN;
        } else if (tension <= BntBeltTension.MIN) {
            label = Component.translatable("chat.bits_n_tracks.belt_tension.slack");
            colour = ChatFormatting.RED;
        } else {
            label = Component.literal(Math.round(tension * 100.0F) + "%");
            colour = ChatFormatting.GRAY;
        }
        return Component.translatable("chat.bits_n_tracks.belt_tension", label).withStyle(colour);
    }
}
