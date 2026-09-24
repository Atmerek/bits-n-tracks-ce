package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionPieceItem;
import dev.qwxon.bitsntracks.interaction.BntSuspensionPayload;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class BntSuspensionPlacement {
    private static final double KEEP_RANGE = 2.5;
    private static final double SIDE_DEAD_ZONE = 0.2;
    private static final int OUTLINE = 0xFFFFFF;
    private static final int CHOSEN = 0x66FF66;
    private static BlockPos selected;
    private static int side;
    private static int facing;
    private static BlockPos first;
    private static BlockPos lead;
    private static int toward;
    private static int bogieFacing;
    private static float bogieDrop;
    private static boolean useHandled;
    private static boolean useWasDown;

    private BntSuspensionPlacement() {
    }

    public static boolean previewAt(BlockPos pos) {
        return toward == 0 && selected != null && selected.equals(pos) && side != 0 && facing != 0;
    }

    public static boolean bogiePreviewAt(BlockPos pos) {
        return lead != null && lead.equals(pos) && bogieFacing != 0;
    }

    public static int side() {
        return side;
    }

    public static int facing() {
        return facing;
    }

    public static int bogieFacing() {
        return bogieFacing;
    }

    public static double bogieDrop() {
        return -bogieDrop;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        useWasDown = minecraft.options.keyUse.isDown();
        useHandled = false;
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || held(player).isEmpty()) {
            first = null;
            clear();
            return;
        }
        if (first != null && (!BntSuspension.canPair(level.getBlockEntity(first)) || !inRange(player, level, first))) {
            first = null;
        }
        if (first != null) {
            Outliner.getInstance().showAABB("bnt_suspension_first", new AABB(first)).colored(CHOSEN).lineWidth(1.0F / 16.0F);
        }

        if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
            && BntSuspension.canCarry(level.getBlockEntity(hit.getBlockPos()))) {
            selected = hit.getBlockPos();
        }
        if (selected == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(selected);
        if (!BntSuspension.canCarry(be)) {
            clear();
            return;
        }

        BlockState state = be.getBlockState();
        Axis axis = BntSuspension.axis(state);
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        if (Sable.HELPER.getContainingClient(be) instanceof ClientSubLevel subLevel) {
            Pose3dc pose = subLevel.logicalPose();
            eye = pose.transformPositionInverse(eye);
            look = pose.transformNormalInverse(look);
        }

        Vec3 centre = Vec3.atCenterOf(selected);
        Vec3 along = BntSuspension.along(axis);
        double facingRay = look.dot(along);
        double distance = Math.abs(facingRay) < 1.0E-4 ? -1.0 : centre.subtract(eye).dot(along) / facingRay;
        if (distance <= 0.0 || distance > player.blockInteractionRange() + KEEP_RANGE) {
            clear();
            return;
        }

        Vec3 aim = eye.add(look.scale(distance)).subtract(centre);
        double across = aim.dot(BntSuspension.across(axis));
        if (Math.hypot(across, aim.y) > KEEP_RANGE) {
            clear();
            return;
        }

        side = across > SIDE_DEAD_ZONE ? 1 : across < -SIDE_DEAD_ZONE ? -1 : 0;
        facing = side == 0 ? 0 : BntSuspension.facingFor(level, selected, state, side);
        toward = first == null ? 0 : BntSuspension.towards(level, first, selected);
        lead = toward > 0 ? first : toward < 0 ? selected : null;
        bogieFacing = toward == 0 || !enough(player) ? 0 : BntSuspension.bogieFacing(level, first, toward);
        bogieDrop = toward == 0 ? 0.0F : BntSuspension.bogieDrop(level.getBlockEntity(first), be);
        Outliner.getInstance().showAABB("bnt_suspension_target", new AABB(selected)).colored(toward != 0 ? CHOSEN : OUTLINE).lineWidth(1.0F / 32.0F);
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !event.isUseItem() || !(player.getItemInHand(event.getHand()).getItem() instanceof BntSuspensionPieceItem)) {
            return;
        }
        if (selected == null) {
            return;
        }

        event.setCanceled(true);
        boolean fresh = !useHandled && !useWasDown;
        if (toward != 0) {
            event.setSwingHand(bogieFacing != 0);
            if (fresh) {
                useHandled = true;
                if (bogieFacing != 0) {
                    PacketDistributor.sendToServer(new BntSuspensionPayload(first, toward * BntSuspension.BOGIE));
                    first = null;
                } else if (!enough(player)) {
                    player.displayClientMessage(Component.translatable("chat.bits_n_tracks.suspension.bogie.pieces"), true);
                }
            }
            return;
        }
        if (side != 0) {
            event.setSwingHand(facing != 0);
            if (facing != 0 && fresh) {
                useHandled = true;
                PacketDistributor.sendToServer(new BntSuspensionPayload(selected, side));
                first = null;
            }
            return;
        }

        event.setSwingHand(false);
        if (!fresh) {
            return;
        }
        useHandled = true;
        if (selected.equals(first)) {
            first = null;
            player.displayClientMessage(Component.translatable("chat.bits_n_tracks.suspension.bogie.cleared"), true);
        } else if (BntSuspension.canPair(player.level().getBlockEntity(selected))) {
            first = selected;
            player.displayClientMessage(Component.translatable("chat.bits_n_tracks.suspension.bogie.first"), true);
        }
    }

    private static ItemStack held(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof BntSuspensionPieceItem ? player.getMainHandItem()
            : player.getOffhandItem().getItem() instanceof BntSuspensionPieceItem ? player.getOffhandItem()
            : ItemStack.EMPTY;
    }

    private static boolean enough(LocalPlayer player) {
        return player.getAbilities().instabuild || held(player).getCount() >= BntSuspension.BOGIE;
    }

    private static boolean inRange(LocalPlayer player, Level level, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        if (Sable.HELPER.getContainingClient(level.getBlockEntity(pos)) instanceof ClientSubLevel subLevel) {
            centre = subLevel.logicalPose().transformPosition(centre);
        }
        double reach = player.blockInteractionRange() + KEEP_RANGE * 2.0;
        return player.getEyePosition(1.0F).distanceToSqr(centre) <= reach * reach;
    }

    private static void clear() {
        selected = null;
        side = 0;
        facing = 0;
        lead = null;
        toward = 0;
        bogieFacing = 0;
    }
}
