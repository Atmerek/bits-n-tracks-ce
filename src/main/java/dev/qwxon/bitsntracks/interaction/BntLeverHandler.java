package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntTrackPlane;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
            BntCogwheelInteraction.togglePhysics(level, player, pos);
            return;
        }
        if (!BntCogwheels.isOwnCogwheel(level, pos) || !level.getBlockState(pos).hasProperty(BlockStateProperties.AXIS)) {
            return;
        }

        switch (payload.action()) {
            case BntLeverPayload.OFFSET -> {
                if (payload.axis() >= 0 && payload.axis() < Axis.values().length) {
                    int steps = Math.max(-MAX_STEPS, Math.min(MAX_STEPS, payload.steps()));
                    CogAlignmentLeverItem.nudge(level, player, pos, Axis.values()[payload.axis()], steps, payload.wholeTrack());
                }
            }
            case BntLeverPayload.PIVOT -> {
                if (payload.axis() >= 0 && payload.axis() < Axis.values().length) {
                    int steps = Math.max(-MAX_STEPS, Math.min(MAX_STEPS, payload.steps()));
                    CogAlignmentLeverItem.movePivot(level, player, pos, Axis.values()[payload.axis()], steps, payload.wholeTrack());
                }
            }
            case BntLeverPayload.TRACK_PLANE -> {
                if (payload.steps() != 0 && BntTrackPlane.set(level, pos, payload.steps() > 0)) {
                    BntToolSounds.toggle(level, pos, payload.steps() > 0);
                } else {
                    BntToolSounds.refuse(player);
                }
            }
            case BntLeverPayload.TRACK_FIT -> {
                if (payload.steps() != 0 && BntTrackPlane.ignore(level, pos, payload.steps() < 0)) {
                    BntToolSounds.toggle(level, pos, payload.steps() > 0);
                } else {
                    BntToolSounds.refuse(player);
                }
            }
            case BntLeverPayload.VISIBILITY -> CogAlignmentLeverItem.toggleVisibility(level, player, pos);
            case BntLeverPayload.RESET -> CogAlignmentLeverItem.reset(level, player, pos, payload.wholeTrack());
            case BntLeverPayload.WIDTH -> CogAlignmentLeverItem.resize(level, player, pos, payload.steps(), payload.wholeTrack());
            case BntLeverPayload.SHAFT -> {
                if (payload.axis() >= 0 && payload.axis() < Direction.values().length) {
                    CogAlignmentLeverItem.toggleShaft(level, player, pos, Direction.values()[payload.axis()], payload.wholeTrack());
                }
            }
            default -> {
            }
        }
    }
}
