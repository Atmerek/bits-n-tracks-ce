package dev.qwxon.bitsntracks.interaction;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltRefit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionPieceItem;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber
public final class BntSuspensionHandler {
    private BntSuspensionHandler() {
    }

    public static void placeFromClient(Player player, BntSuspensionPayload payload) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos pos = payload.pos();
        if (level.isClientSide || !level.isLoaded(pos) || !BntReach.block(level, player, pos)) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().getItem() instanceof BntSuspensionPieceItem ? InteractionHand.MAIN_HAND
            : player.getOffhandItem().getItem() instanceof BntSuspensionPieceItem ? InteractionHand.OFF_HAND
            : null;
        BlockEntity be = level.getBlockEntity(pos);
        if (hand == null || !(be instanceof KineticBlockEntity kinetic) || !BntSuspension.canCarry(be)) {
            return;
        }
        if (Math.abs(payload.side()) == BntSuspension.BOGIE) {
            placeBogie(player, level, pos, kinetic, Integer.signum(payload.side()), Integer.signum(payload.facing()), hand);
            return;
        }
        int side = Integer.signum(payload.side());
        int facing = BntSuspension.facingFor(level, pos, be.getBlockState(), side, Integer.signum(payload.facing()));
        if (facing == 0) {
            return;
        }

        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
        BntSuspension.attach(kinetic, side, facing);
        BntChainEngagement.refresh(level, pos, engagementBefore);
        BntBeltRefit.queue(level, pos);
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        BntToolSounds.mount(level, pos, true);
    }

    private static void placeBogie(
        Player player, Level level, BlockPos pos, KineticBlockEntity kinetic, int toward, int preferred, InteractionHand hand
    ) {
        int facing = BntSuspension.bogieFacing(level, pos, toward, preferred);
        if (facing == 0) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && stack.getCount() < BntSuspension.BOGIE) {
            return;
        }
        BlockPos partnerPos = BntSuspension.neighbour(pos, kinetic.getBlockState(), toward);
        if (!BntReach.block(level, player, partnerPos) || !(level.getBlockEntity(partnerPos) instanceof KineticBlockEntity partner)) {
            return;
        }

        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
        BntSuspension.attachBogie(kinetic, partner, toward, facing);
        BntChainEngagement.refresh(level, pos, engagementBefore);
        BntBeltRefit.queue(level, pos);
        if (!player.getAbilities().instabuild) {
            stack.shrink(BntSuspension.BOGIE);
        }
        BntToolSounds.mount(level, pos, true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kinetic) || !BntSuspension.hasPiece(be)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) {
            return;
        }
        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
        int pieces = BntSuspension.detach(level, pos, kinetic, false);
        BntChainEngagement.refresh(level, pos, engagementBefore);
        BntBeltRefit.queue(level, pos);
        if (!player.getAbilities().instabuild && pieces > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(BitsNTracksItems.SUSPENSION_PIECE.get(), pieces));
        }
        BntToolSounds.mount(level, pos, false);
    }
}
