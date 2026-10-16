package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class BntLeverHandler {
    private static final int MAX_STEPS = 16;

    private BntLeverHandler() {
    }

    public static void applyFromClient(Player player, BntLeverPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos pos = payload.pos();
        Item lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.get();
        if (level.isClientSide || !level.isLoaded(pos) || !player.getMainHandItem().is(lever) || !BntReach.block(level, player, pos)) {
            return;
        }

        if (payload.action() == BntLeverPayload.PHYSICS) {
            WrenchPhysicsHandler.togglePhysics(level, player, pos);
            return;
        }
        if (!HiddenCogwheelCompat.isOwnCogwheel(level, pos) || !level.getBlockState(pos).hasProperty(BlockStateProperties.AXIS)) {
            return;
        }

        switch (payload.action()) {
            case BntLeverPayload.OFFSET -> {
                if (payload.axis() >= 0 && payload.axis() < Axis.values().length) {
                    int steps = Math.max(-MAX_STEPS, Math.min(MAX_STEPS, payload.steps()));
                    CogAlignmentLeverItem.nudge(level, player, pos, Axis.values()[payload.axis()], steps, payload.wholeTrack());
                }
            }
            case BntLeverPayload.VISIBILITY -> CogAlignmentLeverItem.toggleVisibility(level, player, pos);
            case BntLeverPayload.RESET -> CogAlignmentLeverItem.reset(level, player, pos);
            default -> {
            }
        }
    }
}
