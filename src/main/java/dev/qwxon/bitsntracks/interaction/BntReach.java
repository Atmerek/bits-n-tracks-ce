package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BntReach {
    private static final double MARGIN = 3.0;

    private BntReach() {
    }

    public static boolean block(Level level, Player player, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        double reach = player.blockInteractionRange() + MARGIN;
        return player.getEyePosition().distanceToSqr(world) <= reach * reach;
    }

    public static boolean track(Level level, Player player, BlockPos controller) {
        for (BlockPos pos : BntBeltTension.chainPositions(level, controller)) {
            if (block(level, player, pos)) {
                return true;
            }
        }
        return false;
    }
}
