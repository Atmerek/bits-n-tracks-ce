package dev.qwxon.bitsntracks.interaction;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltRefit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

@EventBusSubscriber
public class WrenchPhysicsHandler {
    public static final int FLAT_FACE = -2;

    public static void alignmentModeFromClient(Player player, BntAlignmentModePayload payload) {
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof CogAlignmentLeverItem) {
            CogAlignmentLeverItem.setMode(stack, BntLeverMode.byIndex(payload.mode()));
            CogAlignmentLeverItem.setWholeTrack(stack, payload.wholeTrack());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(LeftClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (event.getEntity() != null && event.getItemStack().is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())
            && HiddenCogwheelCompat.isOwnCogwheel(level, pos)) {
            event.setCanceled(true);
        }
    }

    public static boolean canTogglePhysics(Level level, BlockPos pos) {
        return HiddenCogwheelCompat.isOwnCogwheel(level, pos);
    }

    public static void togglePhysics(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BntFlangedCogwheelBlock)) {
            return;
        }

        boolean newState = !state.getValue(BntFlangedCogwheelBlock.PHYSICS);
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
        BntFlangedCogwheelBlock.setPhysics(level, pos, newState);
        if (partnerPos != null) {
            BntFlangedCogwheelBlock.setPhysics(level, partnerPos, newState);
        }

        BntChainEngagement.refresh(level, pos, engagementBefore);
        BntBeltRefit.queue(level, pos);
        BntToolSounds.physics(level, pos, newState);
        Component status = trackStatus(level, pos);
        if (status != null) {
            BntToolMessages.send(player, status);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakEvent event) {
        if (event.getPlayer() != null && event.getPlayer().getMainHandItem().is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())
            && HiddenCogwheelCompat.isOwnCogwheel(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock)
            || !AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())
            || !(BlockEntityBehaviour.get(level, pos, CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour chain)
            || !chain.isPartOfChain()) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!level.isClientSide) {
            chain.destroyChain(!player.isCreative(), true);
        }
    }

    public static void routeFromClient(Player player, BntRouteSidePayload payload) {
        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();
        if (level.isClientSide || !level.isLoaded(pos) || !player.getMainHandItem().is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())
            || !BntReach.block(level, player, pos)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!HiddenCogwheelCompat.isOwnCogwheel(level, pos) || !state.hasProperty(BlockStateProperties.AXIS)
            || !(level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access)) {
            return;
        }
        if (payload.zone() == FLAT_FACE) {
            BntToolMessages.send(player, Component.translatable("bits_n_tracks.tool_panel.message.flat_face"));
            BntToolSounds.refuse(player);
            return;
        }

        Direction zone = payload.zone() < 0 || payload.zone() >= Direction.values().length
            ? null
            : Direction.values()[payload.zone()];
        int side = zone == null || zone.ordinal() == access.bnt$getTrackRouteSide() ? -1 : zone.ordinal();
        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
        setRouteAt(level, pos, side);
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        if (partnerPos != null) {
            setRouteAt(level, partnerPos, side);
        }

        BntChainEngagement.refresh(level, pos, engagementBefore);
        BntBeltRefit.queue(level, pos);
        BntToolSounds.toggle(level, pos, side >= 0);
    }

    public static Component trackStatus(Level level, BlockPos pos) {
        Boolean engaged = BntChainEngagement.engagementAt(level, pos);
        if (engaged == null) {
            return null;
        }
        return Component.translatable(engaged ? "bits_n_tracks.tool_panel.message.on_track" : "bits_n_tracks.tool_panel.message.off_track");
    }

    private static void setRouteAt(Level level, BlockPos pos, int side) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic
            && kinetic instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setTrackRouteSide(side);
            kinetic.setChanged();
            kinetic.sendData();
        }
    }
}
