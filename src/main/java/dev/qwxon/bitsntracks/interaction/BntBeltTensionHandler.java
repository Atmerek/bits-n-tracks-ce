package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BntBeltTensionHandler {
    private BntBeltTensionHandler() {
    }

    public static void applyFromClient(Player player, BntBeltTensionPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos controllerPos = payload.controllerPos();
        if (level.isClientSide || !level.isLoaded(controllerPos) || !player.getMainHandItem().is(BitsNTracksItems.COG_ALIGNMENT_LEVER.get())
            || !HiddenCogwheelCompat.isOwnChain(level, controllerPos) || !BntReach.track(level, player, controllerPos)) {
            return;
        }

        float before = BntBeltTension.at(level, controllerPos);
        float after = BntBeltTension.step(level, controllerPos, payload.tighten());
        if (after == before) {
            BntToolSounds.refuse(player);
        } else {
            BntToolSounds.adjust(level, controllerPos, after);
        }
    }
}
