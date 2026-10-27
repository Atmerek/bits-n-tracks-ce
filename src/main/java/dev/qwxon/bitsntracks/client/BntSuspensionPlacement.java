package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionPieceItem;
import dev.qwxon.bitsntracks.interaction.BntReach;
import dev.qwxon.bitsntracks.interaction.BntSuspensionPayload;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private static BlockPos selected;
    private static int side;
    private static int facing;
    private static int preferred;
    private static BlockPos first;
    private static BlockPos lead;
    private static int toward;
    private static int bogieFacing;
    private static float bogieDrop;
    private static String armRefusal;
    private static String bogieRefusal;
    private static int ghostFacing;
    private static int bogieGhostFacing;
    private static BlockPos physicsOff;
    private static boolean useHandled;
    private static boolean useWasDown;

    private BntSuspensionPlacement() {
    }

    public static boolean previewAt(BlockPos pos) {
        return toward == 0 && selected != null && selected.equals(pos) && side != 0 && ghostFacing != 0;
    }

    public static boolean bogiePreviewAt(BlockPos pos) {
        return lead != null && lead.equals(pos) && bogieGhostFacing != 0;
    }

    public static boolean previewFits() {
        return facing != 0;
    }

    public static boolean bogiePreviewFits() {
        return bogieFacing != 0;
    }

    public static int side() {
        return side;
    }

    public static int facing() {
        return ghostFacing;
    }

    public static int bogieFacing() {
        return bogieGhostFacing;
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
            physicsOff = null;
            clear();
            return;
        }
        if (first != null && (!BntSuspension.canCarry(level.getBlockEntity(first)) || !BntReach.block(level, player, first))) {
            first = null;
        }
        if (first != null && level instanceof ClientLevel clientLevel) {
            BntToolVisuals.selected("bnt_suspension_first", clientLevel, first);
        }

        physicsOff = null;
        if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            if (BntSuspension.needsPhysics(level.getBlockState(hit.getBlockPos()))) {
                physicsOff = hit.getBlockPos();
                clear();
                if (level instanceof ClientLevel clientLevel) {
                    BntToolVisuals.cog("bnt_suspension_target", clientLevel, physicsOff, BntToolVisuals.WARNING, BntToolVisuals.THIN);
                }
                return;
            }
            if (BntSuspension.canCarry(level.getBlockEntity(hit.getBlockPos()))) {
                selected = hit.getBlockPos();
            }
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

        boolean chosen = selected.equals(first);
        preferred = (int)Math.signum(facingRay);
        side = !chosen ? 0 : across > SIDE_DEAD_ZONE ? 1 : across < -SIDE_DEAD_ZONE ? -1 : 0;
        facing = side == 0 ? 0 : BntSuspension.facingFor(level, selected, state, side, preferred);
        armRefusal = side == 0 || facing != 0 ? null : BntSuspension.armRefusal(level, selected, state, side);
        ghostFacing = facing != 0 ? facing
            : "arm.support".equals(armRefusal) ? BntSuspension.armShaftFacing(level, selected, state, preferred) : 0;
        toward = first == null || chosen ? 0 : BntSuspension.towards(level, first, selected);
        lead = toward > 0 ? first : toward < 0 ? selected : null;
        bogieFacing = toward == 0 || !enough(player) ? 0 : BntSuspension.bogieFacing(level, first, toward, preferred);
        bogieRefusal = toward == 0 || !enough(player) || bogieFacing != 0 ? null : BntSuspension.bogieRefusal(level, first, toward);
        bogieGhostFacing = bogieFacing != 0 ? bogieFacing
            : "bogie.support".equals(bogieRefusal) ? BntSuspension.bogieShaftFacing(level, first, toward, preferred) : 0;
        bogieDrop = toward == 0 ? 0.0F : BntSuspension.bogieDrop(level.getBlockEntity(first), be);
        if (!chosen && level instanceof ClientLevel clientLevel) {
            BntToolVisuals.cog("bnt_suspension_target", clientLevel, selected,
                toward == 0 ? BntToolVisuals.HOVER : bogieFacing != 0 ? BntToolVisuals.GOOD : BntToolVisuals.WARNING, BntToolVisuals.THIN);
        }
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !event.isUseItem() || !(player.getItemInHand(event.getHand()).getItem() instanceof BntSuspensionPieceItem)) {
            return;
        }
        boolean fresh = !useHandled && !useWasDown;
        if (physicsOff != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (fresh) {
                useHandled = true;
                refuse(player, "physics");
            }
            return;
        }
        if (selected == null) {
            return;
        }

        event.setCanceled(true);
        if (toward != 0) {
            event.setSwingHand(bogieFacing != 0);
            if (fresh) {
                useHandled = true;
                if (bogieFacing != 0) {
                    PacketDistributor.sendToServer(new BntSuspensionPayload(first, toward * BntSuspension.BOGIE, preferred));
                    first = null;
                } else if (!enough(player)) {
                    refuse(player, "bogie.pieces");
                } else if (bogieRefusal != null) {
                    refuse(player, bogieRefusal);
                }
            }
            return;
        }
        if (side != 0) {
            event.setSwingHand(facing != 0);
            if (fresh) {
                useHandled = true;
                if (facing != 0) {
                    PacketDistributor.sendToServer(new BntSuspensionPayload(selected, side, preferred));
                    first = null;
                } else if (armRefusal != null) {
                    refuse(player, armRefusal);
                }
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
            player.displayClientMessage(Component.translatable("chat.bits_n_tracks.suspension.cleared"), true);
        } else {
            first = selected;
            player.displayClientMessage(Component.translatable(BntSuspension.canPair(player.level().getBlockEntity(selected))
                ? "chat.bits_n_tracks.suspension.first.pair"
                : "chat.bits_n_tracks.suspension.first"), true);
        }
        BntToolCues.select(first != null);
    }

    private static ItemStack held(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof BntSuspensionPieceItem ? player.getMainHandItem()
            : player.getOffhandItem().getItem() instanceof BntSuspensionPieceItem ? player.getOffhandItem()
            : ItemStack.EMPTY;
    }

    private static void refuse(LocalPlayer player, String reason) {
        player.displayClientMessage(Component.translatable("chat.bits_n_tracks.suspension." + reason), true);
        BntToolCues.refuse();
    }

    private static boolean enough(LocalPlayer player) {
        return player.getAbilities().instabuild || held(player).getCount() >= BntSuspension.BOGIE;
    }

    private static void clear() {
        selected = null;
        side = 0;
        facing = 0;
        preferred = 0;
        lead = null;
        toward = 0;
        bogieFacing = 0;
        armRefusal = null;
        bogieRefusal = null;
        ghostFacing = 0;
        bogieGhostFacing = 0;
    }
}
