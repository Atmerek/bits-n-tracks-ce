package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltRefit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.physics.BntTuning;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BntTuningHandler {
    private static final double REACH_MARGIN = 3.0;

    private BntTuningHandler() {
    }

    public static void modeFromClient(Player player, BntTuningModePayload payload) {
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof SuspensionToolItem) {
            SuspensionToolItem.setMode(stack, BntTuning.byIndex(payload.setting()), payload.wholeTrack());
        }
    }

    public static void tuneFromClient(Player player, BntTuningPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos pos = payload.pos();
        if (level.isClientSide || !level.isLoaded(pos) || !(player.getMainHandItem().getItem() instanceof SuspensionToolItem)) {
            return;
        }
        if (!withinReach(level, player, pos)
            || !(level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess target)
            || !BntSuspension.hasPiece(level.getBlockEntity(pos))) {
            return;
        }

        BntTuning setting = BntTuning.byIndex(payload.setting());
        int value = BntTuning.clamp(target.bnt$getTuning(setting) + payload.step());
        if (value == target.bnt$getTuning(setting) && !payload.wholeTrack()) {
            BntToolSounds.refuse(player);
            return;
        }
        Set<BlockPos> positions = payload.wholeTrack() ? BntBeltTension.chainPositions(level, pos) : new LinkedHashSet<>(Set.of(pos));
        for (BlockPos nodePos : Set.copyOf(positions)) {
            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, nodePos);
            if (partnerPos != null) {
                positions.add(partnerPos);
            }
            for (KineticBlockEntity cog : BntSuspension.bogieCogs(level, nodePos)) {
                positions.add(cog.getBlockPos());
            }
        }

        Map<BlockPos, Integer> engagementBefore = setting == BntTuning.TRAVEL ? BntChainEngagement.snapshot(level, pos) : null;
        Set<BlockPos> tuned = new LinkedHashSet<>();
        for (BlockPos nodePos : positions) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity kinetic
                && kinetic instanceof KineticBlockEntityPhysicsAccess access
                && BntSuspension.hasPiece(kinetic)) {
                access.bnt$setTuning(setting, value);
                kinetic.setChanged();
                kinetic.sendData();
                tuned.add(nodePos);
            }
        }
        if (engagementBefore != null) {
            BntChainEngagement.refresh(level, pos, engagementBefore);
            BntBeltRefit.queue(level, pos);
        }
        BntToolSounds.adjust(level, pos, (float)(value - BntTuning.MIN) / (BntTuning.MAX - BntTuning.MIN));

        if (payload.wholeTrack()) {
            int wheels = BntCogwheelPairing.countWheels(level, tuned);
            BntToolMessages.send(player, Component.translatable(
                wheels == 1 ? "bits_n_tracks.tool_panel.message.tuned.single" : "bits_n_tracks.tool_panel.message.tuned", wheels));
        }
    }


    private static boolean withinReach(Level level, Player player, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        double reach = player.blockInteractionRange() + REACH_MARGIN;
        return player.getEyePosition().distanceToSqr(world) <= reach * reach;
    }
}
