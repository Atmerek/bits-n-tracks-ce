package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

public final class BntBeltTensionHandler {
    private static final double MAX_REACH = 64.0;

    private BntBeltTensionHandler() {
    }

    public static void applyFromClient(Player player, BntBeltTensionPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos controllerPos = payload.controllerPos();
        if (level.isClientSide || !level.isLoaded(controllerPos) || !withinReach(level, player, controllerPos)) {
            return;
        }
        if (!holdingLever(player) || !HiddenCogwheelCompat.isOwnChain(level, controllerPos)) {
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

    private static boolean holdingLever(Player player) {
        Item lever = (Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get();
        return player.getMainHandItem().is(lever) || player.getOffhandItem().is(lever);
    }

    private static boolean withinReach(Level level, Player player, BlockPos controllerPos) {
        Vec3 centre = Vec3.atCenterOf(controllerPos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        return player.position().distanceToSqr(world) <= MAX_REACH * MAX_REACH;
    }
}
