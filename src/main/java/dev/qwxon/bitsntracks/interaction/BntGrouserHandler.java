package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntGrousers;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BntGrouserHandler {
    private BntGrouserHandler() {
    }

    public static void applyFromClient(Player player, BntGrouserPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos controllerPos = payload.controllerPos();
        ItemStack held = player.getMainHandItem();
        if (level.isClientSide || !level.isLoaded(controllerPos) || !BntCogwheels.isOwnChain(level, controllerPos)
            || !BntReach.track(level, player, controllerPos) || !held.is(BitsNTracksItems.GROUSER.get())) {
            return;
        }

        int cost = BntGrousers.cost(level, controllerPos);
        if (!BntGrousers.accepts(BntChains.chainAt(level, controllerPos)) || BntGrousers.at(level, controllerPos) > 0
            || !player.hasInfiniteMaterials() && held.getCount() < cost) {
            BntToolSounds.refuse(player);
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            held.shrink(cost);
        }
        BntGrousers.write(level, controllerPos, cost);
        level.playSound(null, controllerPos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
