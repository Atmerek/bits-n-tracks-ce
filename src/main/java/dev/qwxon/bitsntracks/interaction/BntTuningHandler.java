package dev.qwxon.bitsntracks.interaction;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.physics.BntTuning;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
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
            || !target.bnt$isPhysicsEnabled()) {
            return;
        }

        BntTuning setting = BntTuning.byIndex(payload.setting());
        int value = BntTuning.clamp(target.bnt$getTuning(setting) + Integer.signum(payload.step()));
        Set<BlockPos> positions = payload.wholeTrack() ? BntBeltTension.chainPositions(level, pos) : new LinkedHashSet<>(Set.of(pos));
        for (BlockPos nodePos : Set.copyOf(positions)) {
            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, nodePos);
            if (partnerPos != null) {
                positions.add(partnerPos);
            }
        }

        Map<BlockPos, Integer> engagementBefore = setting == BntTuning.TRAVEL ? BntChainEngagement.snapshot(level, pos) : null;
        int tuned = 0;
        for (BlockPos nodePos : positions) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity kinetic
                && kinetic instanceof KineticBlockEntityPhysicsAccess access
                && access.bnt$isPhysicsEnabled()) {
                access.bnt$setTuning(setting, value);
                kinetic.setChanged();
                kinetic.sendData();
                tuned++;
            }
        }
        if (engagementBefore != null) {
            BntChainEngagement.refresh(level, pos, engagementBefore);
        }

        Component scope = payload.wholeTrack()
            ? Component.translatable("chat.bits_n_tracks.tuning.scope.track", tuned)
            : Component.translatable("chat.bits_n_tracks.tuning.scope.cogwheel");
        player.displayClientMessage(
            Component.translatable("chat.bits_n_tracks.tuning.level",
                    Component.translatable(setting.translationKey()), value, BntTuning.MAX, scope)
                .withStyle(ChatFormatting.GOLD),
            true);
    }

    private static boolean withinReach(Level level, Player player, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        SubLevel subLevel = Sable.HELPER.getContaining(level, centre);
        Vec3 world = subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
        double reach = player.blockInteractionRange() + REACH_MARGIN;
        return player.getEyePosition().distanceToSqr(world) <= reach * reach;
    }
}
