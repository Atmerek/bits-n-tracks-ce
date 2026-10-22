package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainWrap;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class BntChainWrapHandler {
    private BntChainWrapHandler() {
    }

    public static void applyFromClient(Player player, BntChainWrapPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos controllerPos = payload.controllerPos();
        ItemStack held = player.getMainHandItem();
        if (level.isClientSide || !level.isLoaded(controllerPos) || !BntCogwheels.isOwnChain(level, controllerPos)
            || !BntReach.track(level, player, controllerPos) || !held.is(Items.CHAIN)) {
            return;
        }

        int cost = BntChainWrap.cost(level, controllerPos);
        if (!BntChainWrap.accepts(BntChains.chainAt(level, controllerPos)) || BntChainWrap.at(level, controllerPos) > 0
            || !player.hasInfiniteMaterials() && held.getCount() < cost) {
            BntToolSounds.refuse(player);
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            held.shrink(cost);
        }
        BntChainWrap.write(level, controllerPos, cost);
        level.playSound(null, controllerPos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
