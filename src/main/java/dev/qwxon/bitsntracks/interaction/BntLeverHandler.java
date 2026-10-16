package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class BntLeverHandler {
    private static final double REACH_MARGIN = 3.0;
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
        if (level.isClientSide || !level.isLoaded(pos) || !player.getMainHandItem().is(lever) || !withinReach(level, player, pos)) {
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

    public static boolean withinReach(Level level, Player player, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        double reach = player.blockInteractionRange() + REACH_MARGIN;
        return player.getEyePosition().distanceToSqr(world) <= reach * reach;
    }
}
