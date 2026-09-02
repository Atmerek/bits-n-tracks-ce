package dev.qwxon.bitsntracks.interaction;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntClientRouteClick;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @SubscribeEvent
    public static void onLeftClickBlock(LeftClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        if (player != null) {
            ItemStack stack = event.getItemStack();
            if (stack.is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())) {
                event.setCanceled(true);
                BlockPos pos = event.getPos();
                if (level.isClientSide) {
                    if (player.isShiftKeyDown()
                        && event.getAction() == LeftClickBlock.Action.START
                        && isToggleableCogwheel(level.getBlockState(pos).getBlock())) {
                        BntClientRouteClick.send(pos);
                    }
                } else {
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    if (isToggleableCogwheel(block)) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
                            if (player.isShiftKeyDown()) {
                                return;
                            }

                            boolean newState = !access.bnt$isPhysicsEnabled();
                            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
                            Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
                            setPhysicsAt(level, pos, newState);
                            if (partnerPos != null) {
                                setPhysicsAt(level, partnerPos, newState);
                            }

                            BntChainEngagement.refresh(level, pos, engagementBefore);
                            Component message = Component.translatable(
                                "chat.bits_n_tracks.alignment.track.status",
                                Component.translatable(newState
                                    ? "tooltip.bits_n_tracks.physics_enabled"
                                    : "tooltip.bits_n_tracks.physics_disabled"),
                                trackStatus(level, pos))
                                .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);
                            player.displayClientMessage(message, true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakEvent event) {
        if (event.getPlayer() != null && event.getPlayer().getMainHandItem().is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide) {
            BlockPos pos = event.getPos();
            Block block = level.getBlockState(pos).getBlock();
            if (isToggleableCogwheel(block)) {
                ItemStack[] handItems = new ItemStack[]{event.getEntity().getMainHandItem(), event.getEntity().getOffhandItem()};
                boolean hasWrench = false;

                for (ItemStack stack : handItems) {
                    if (AllItems.WRENCH.isIn(stack)) {
                        hasWrench = true;
                        break;
                    }
                }

                if (hasWrench) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof KineticBlockEntity) {
                        CogwheelChainBehaviour chainBehaviour = (CogwheelChainBehaviour)BlockEntityBehaviour.get(level, pos, CogwheelChainBehaviour.TYPE);
                        if (chainBehaviour != null && chainBehaviour.isPartOfChain()) {
                            chainBehaviour.destroyChain(!event.getEntity().isCreative(), true);
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

    public static void routeFromClient(Player player, BntRouteSidePayload payload) {
        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();
        if (level.isClientSide || !level.isLoaded(pos)) {
            return;
        }
        if (!player.getMainHandItem().is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())
            && !player.getOffhandItem().is((Item)BitsNTracksItems.COG_ALIGNMENT_LEVER.get())) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!isToggleableCogwheel(state.getBlock()) || !state.hasProperty(BlockStateProperties.AXIS)
            || !(level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access)) {
            return;
        }
        if (payload.zone() == FLAT_FACE) {
            player.displayClientMessage(
                Component.translatable("chat.bits_n_tracks.alignment.route.flat_face").withStyle(ChatFormatting.RED), true);
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
        player.displayClientMessage(
            (side < 0
                ? Component.translatable("chat.bits_n_tracks.alignment.route.automatic")
                : Component.translatable("chat.bits_n_tracks.alignment.route.wrapped",
                    Component.translatable("chat.bits_n_tracks.alignment.route." + zone.getOpposite().getName())))
                .withStyle(ChatFormatting.AQUA),
            true);
    }

    private static Component trackStatus(Level level, BlockPos pos) {
        Boolean engaged = BntChainEngagement.engagementAt(level, pos);
        return Component.translatable(engaged == null
            ? "chat.bits_n_tracks.alignment.track.detached"
            : engaged ? "chat.bits_n_tracks.alignment.track.driving" : "chat.bits_n_tracks.alignment.track.free");
    }

    private static void setRouteAt(Level level, BlockPos pos, int side) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic
            && kinetic instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setTrackRouteSide(side);
            kinetic.setChanged();
            kinetic.sendData();
        }
    }

    private static void setPhysicsAt(Level level, BlockPos pos, boolean enabled) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!isToggleableCogwheel(block)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled() != enabled) {
            if (isCogwheelVariant(block)) {
                swapCogwheelBlock(level, pos, state, be, enabled);
            } else {
                access.bnt$setPhysicsEnabled(enabled);
            }
        }
    }

    private static boolean isToggleableCogwheel(Block block) {
        return isCogwheelVariant(block);
    }

    private static boolean isCogwheelVariant(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String namespace = id.getNamespace();
        if (!"bits_n_bobs".equals(namespace) && !"bits_n_tracks".equals(namespace) && !"dndecor".equals(namespace)) {
            return false;
        } else {
            String path = id.getPath();
            return path.equals("small_flanged_cogwheel")
                || path.equals("large_flanged_cogwheel")
                || path.equals("flanged_cogwheel")
                || path.equals("small_empty_flanged_cogwheel")
                || path.equals("large_empty_flanged_cogwheel")
                || path.equals("medium_flanged_cogwheel")
                || path.equals("medium_industrial_flanged_cogwheel")
                || path.equals("small_hidden_flanged_cogwheel")
                || path.equals("medium_hidden_flanged_cogwheel")
                || path.equals("large_hidden_flanged_cogwheel")
                || path.equals("industrial_flanged_cogwheel")
                || path.equals("large_industrial_flanged_cogwheel")
                || path.equals("industrial_cogwheel")
                || path.equals("large_industrial_cogwheel")
                || path.equals("tiny_flanged_cogwheel")
                || path.equals("industrial_tiny_flanged_cogwheel")
                || path.equals("tiny_hidden_flanged_cogwheel");
        }
    }

    private static void swapCogwheelBlock(Level level, BlockPos pos, BlockState oldState, BlockEntity oldBe, boolean physicsEnabled) {
        BlockState newState = physicsEnabled
            ? HiddenCogwheelCompat.toHiddenCogwheelState(oldState)
            : HiddenCogwheelCompat.toVisibleCogwheelState(oldState, oldBe);
        if (newState != null && newState.getBlock() != oldState.getBlock()) {
            swapBlock(level, pos, oldBe, newState, physicsEnabled);
        } else {
            if (oldBe instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setPhysicsEnabled(physicsEnabled);
            }
        }
    }

    private static void swapBlock(Level level, BlockPos pos, BlockEntity oldBe, BlockState newState, boolean physicsEnabled) {
        CompoundTag tag = oldBe.saveWithoutMetadata(level.registryAccess());
        tag.putBoolean("BntPhysicsEnabled", physicsEnabled);
        if (physicsEnabled && !tag.contains("BntOriginalBlock")) {
            BlockState oldState = level.getBlockState(pos);
            ResourceLocation oldBlockId = BuiltInRegistries.BLOCK.getKey(oldState.getBlock());
            tag.putString("BntOriginalBlock", oldBlockId.toString());
        }

        HiddenCogwheelCompat.replaceBlockForPhysicsSwap(level, pos, newState);
        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe != null) {
            newBe.loadWithComponents(tag, level.registryAccess());
            if (newBe instanceof KineticBlockEntityPhysicsAccess newAccess) {
                newAccess.bnt$setPhysicsEnabled(physicsEnabled);
                if (physicsEnabled && tag.contains("BntOriginalBlock")) {
                    newAccess.bnt$setOriginalBlock(tag.getString("BntOriginalBlock"));
                }
            }

            newBe.setChanged();
            if (newBe instanceof KineticBlockEntity kinetic) {
                kinetic.sendData();
            }

            BntChainEngagement.rebuild(level, pos);
        }
    }
}
