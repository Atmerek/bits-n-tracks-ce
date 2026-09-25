package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltRefit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksDataComponents;
import dev.qwxon.bitsntracks.physics.BntTuning;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class CogAlignmentLeverItem extends Item {
    public CogAlignmentLeverItem(Properties properties) {
        super(properties);
    }

    public static boolean wholeTrack(ItemStack stack) {
        return stack.getOrDefault(BitsNTracksDataComponents.ALIGNMENT_WHOLE_TRACK.get(), false);
    }

    public static void setWholeTrack(ItemStack stack, boolean wholeTrack) {
        stack.set(BitsNTracksDataComponents.ALIGNMENT_WHOLE_TRACK.get(), wholeTrack);
    }

    public static MutableComponent scopeName(boolean wholeTrack) {
        return Component.translatable(wholeTrack ? "bits_n_tracks.scope.track" : "bits_n_tracks.scope.cogwheel");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bits_n_tracks.alignment.scope",
                scopeName(wholeTrack(stack)).withStyle(ChatFormatting.GOLD))
            .withStyle(ChatFormatting.GRAY));
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return InteractionResult.PASS;
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
                Direction clickedFace = context.getClickedFace();
                Axis blockAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                } else {
                    Player player = context.getPlayer();
                    Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
                    if (player != null && player.isShiftKeyDown()) {
                        for (BlockPos nodePos : collectChainPositions(level, pos)) {
                            BlockEntity nodeBe = level.getBlockEntity(nodePos);
                            if (nodeBe instanceof KineticBlockEntity kinetic && nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                                nodeAccess.bnt$setAlignmentOffsetX(0.0F);
                                nodeAccess.bnt$setAlignmentOffsetY(0.0F);
                                nodeAccess.bnt$setAlignmentOffsetZ(0.0F);
                                nodeAccess.bnt$setHiddenByLever(false);
                                nodeAccess.bnt$setTrackRouteSide(-1);
                                for (BntTuning setting : BntTuning.values()) {
                                    nodeAccess.bnt$setTuning(setting, BntTuning.DEFAULT);
                                }
                                kinetic.setChanged();
                                kinetic.sendData();
                            }
                        }

                        BntChainEngagement.refresh(level, pos, engagementBefore);
                        BntBeltRefit.queue(level, pos);
                        player.displayClientMessage(shiftMessage(access, level, pos, 0), true);
                        return InteractionResult.SUCCESS;
                    } else {
                        Vec3 hitVec = context.getClickLocation().subtract(HiddenCogwheelCompat.getModelTranslation(be, 1.0F));
                        double localX = hitVec.x - pos.getX();
                        double localY = hitVec.y - pos.getY();
                        double localZ = hitVec.z - pos.getZ();
                        double dx = localX - 0.5;
                        double dy = localY - 0.5;
                        double dz = localZ - 0.5;
                        double radius = CogwheelSizeHelper.getToolHighlightRadius(state.getBlock());
                        double centerThresh = 0.25 * radius * radius;
                        float step = 0.0625F;
                        float limit = 1.0F;
                        boolean wholeTrack = wholeTrack(context.getItemInHand());
                        boolean toggledVisibility = false;
                        Axis moveAxis = null;
                        float delta = 0.0F;
                        if (clickedFace.getAxis() != blockAxis) {
                            moveAxis = blockAxis;
                            delta = getAxisDelta(blockAxis, dx, dy, dz) > 0.0 ? step : -step;
                        } else {
                            Axis first = blockAxis == Axis.X ? Axis.Z : Axis.X;
                            Axis second = blockAxis == Axis.Y ? Axis.Z : Axis.Y;
                            double firstDelta = getAxisDelta(first, dx, dy, dz);
                            double secondDelta = getAxisDelta(second, dx, dy, dz);
                            if (firstDelta * firstDelta + secondDelta * secondDelta < centerThresh) {
                                access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                                toggledVisibility = true;
                            } else {
                                moveAxis = Math.abs(firstDelta) > Math.abs(secondDelta) ? first : second;
                                delta = getAxisDelta(moveAxis, dx, dy, dz) > 0.0 ? step : -step;
                            }
                        }

                        if (moveAxis == blockAxis && !wholeTrack && BntSuspension.isBogie(be)) {
                            if (player != null) {
                                player.displayClientMessage(Component.translatable("chat.bits_n_tracks.alignment.bogie"), true);
                            }
                            return InteractionResult.SUCCESS;
                        }

                        Set<BlockPos> moved = new LinkedHashSet<>();
                        if (moveAxis != null && moveAxis != Axis.Y && moveAxis != blockAxis && BntSuspension.isBogie(be)) {
                            Vec3 across = BntSuspension.across(blockAxis);
                            double outward = -Integer.signum(access.bnt$getSuspensionSide()) * delta * (moveAxis == Axis.X ? across.x : across.z);
                            Set<BlockPos> spread = spreadBogies(level, wholeTrack ? collectChainPositions(level, pos) : Set.of(pos), outward, limit);
                            if (spread.isEmpty()) {
                                if (player != null) {
                                    player.displayClientMessage(Component.translatable(outward < 0.0
                                        ? "chat.bits_n_tracks.alignment.bogie.closest"
                                        : "chat.bits_n_tracks.alignment.bogie.farthest"), true);
                                }
                                return InteractionResult.SUCCESS;
                            }
                            if (wholeTrack) {
                                moved.addAll(spread);
                            }
                        } else if (moveAxis != null && wholeTrack) {
                            boolean physics = access.bnt$isPhysicsEnabled();
                            for (BlockPos nodePos : collectChainPositions(level, pos)) {
                                BlockEntity nodeBe = level.getBlockEntity(nodePos);
                                if (nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess
                                    && nodeAccess.bnt$isPhysicsEnabled() == physics) {
                                    shiftAxis(nodeAccess, moveAxis, delta, limit, BntSuspension.isBogie(nodeBe));
                                    moved.add(nodePos);
                                }
                            }
                            for (BlockPos nodePos : Set.copyOf(moved)) {
                                if (level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess nodeAccess
                                    && BntSuspension.partner(level, nodePos, level.getBlockEntity(nodePos)) instanceof KineticBlockEntity partner
                                    && moved.add(partner.getBlockPos())) {
                                    ((KineticBlockEntityPhysicsAccess)partner).bnt$setAlignmentOffsetY(nodeAccess.bnt$getAlignmentOffsetY());
                                }
                            }
                        } else if (moveAxis != null) {
                            shiftAxis(access, moveAxis, delta, limit, BntSuspension.isBogie(be));
                            if (BntSuspension.partner(level, pos, be) instanceof KineticBlockEntity partner) {
                                ((KineticBlockEntityPhysicsAccess)partner).bnt$setAlignmentOffsetY(access.bnt$getAlignmentOffsetY());
                                partner.setChanged();
                                partner.sendData();
                            }
                        }

                        if (!moved.isEmpty()) {
                            for (BlockPos nodePos : moved) {
                                if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity kinetic) {
                                    kinetic.setChanged();
                                    kinetic.sendData();
                                }
                            }
                        } else {
                            be.setChanged();
                            ((KineticBlockEntity)be).sendData();
                            BntCogwheelPairing.pushSettingsToPartner(level, pos);
                        }

                        BntChainEngagement.refresh(level, pos, engagementBefore);
                        if (moveAxis != null) {
                            BntBeltRefit.queue(level, pos);
                        }

                        if (player != null) {
                            if (toggledVisibility) {
                                Component status = access.bnt$isHiddenByLever()
                                    ? Component.translatable("chat.bits_n_tracks.alignment.visibility.hidden")
                                    : Component.translatable("chat.bits_n_tracks.alignment.visibility.shown");
                                player.displayClientMessage(Component.translatable("chat.bits_n_tracks.alignment.visibility", new Object[]{status}), true);
                            } else {
                                player.displayClientMessage(
                                    shiftMessage(access, level, pos, BntCogwheelPairing.countWheels(level, moved)), true);
                            }
                        }

                        return InteractionResult.SUCCESS;
                    }
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    private static double getAxisDelta(Axis axis, double dx, double dy, double dz) {
        return switch (axis) {
            case X -> dx;
            case Y -> dy;
            case Z -> dz;
            default -> throw new MatchException(null, null);
        };
    }

    private static void shiftAxis(KineticBlockEntityPhysicsAccess access, Axis axis, float delta, float limit, boolean bogie) {
        if (bogie) {
            if (axis == Axis.Y) {
                access.bnt$setAlignmentOffsetY(Mth.clamp(access.bnt$getAlignmentOffsetY() + delta, -limit, 0.0F));
            }
            return;
        }
        switch (axis) {
            case X:
                access.bnt$setAlignmentOffsetX(Mth.clamp(access.bnt$getAlignmentOffsetX() + delta, -limit, limit));
                break;
            case Y:
                access.bnt$setAlignmentOffsetY(Mth.clamp(access.bnt$getAlignmentOffsetY() + delta, -limit, limit));
                break;
            case Z:
                access.bnt$setAlignmentOffsetZ(Mth.clamp(access.bnt$getAlignmentOffsetZ() + delta, -limit, limit));
        }
    }

    private static Set<BlockPos> spreadBogies(Level level, Set<BlockPos> positions, double outward, float limit) {
        Set<BlockPos> seen = new LinkedHashSet<>();
        Set<BlockPos> spread = new LinkedHashSet<>();
        for (BlockPos nodePos : positions) {
            BlockEntity nodeBe = level.getBlockEntity(nodePos);
            if (!seen.contains(nodePos) && BntSuspension.partner(level, nodePos, nodeBe) instanceof KineticBlockEntity partner) {
                seen.add(nodePos);
                seen.add(partner.getBlockPos());
                double current = BntSuspension.bogieSpread(nodeBe);
                double next = Mth.clamp(current + outward, BntSuspension.closestBogieSpread(nodeBe.getBlockState()), limit);
                if (Math.abs(next - current) >= 1.0E-4) {
                    BntSuspension.spreadBogie((KineticBlockEntity)nodeBe, partner, next);
                    spread.add(nodePos);
                    spread.add(partner.getBlockPos());
                }
            }
        }
        return spread;
    }

    private static Component shiftMessage(KineticBlockEntityPhysicsAccess access, Level level, BlockPos pos, int wheels) {
        MutableComponent message = Component.translatable(
            "chat.bits_n_tracks.alignment.shift.3d",
            new Object[]{
                formatPixels(access.bnt$getAlignmentOffsetX()), formatPixels(access.bnt$getAlignmentOffsetY()), formatPixels(access.bnt$getAlignmentOffsetZ())
            }
        );

        if (wheels > 0) {
            message.append(" ").append(Component.translatable(
                wheels == 1 ? "chat.bits_n_tracks.alignment.scope.single" : "chat.bits_n_tracks.alignment.scope.track", wheels));
        }

        Boolean engaged = BntChainEngagement.engagementAt(level, pos);
        if (engaged == null) {
            return message;
        }
        return message.append(" ").append(
            Component.translatable(engaged
                    ? "chat.bits_n_tracks.alignment.track.driving"
                    : "chat.bits_n_tracks.alignment.track.free")
                .withStyle(engaged ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    private static String formatPixels(float offset) {
        int px = Math.round(offset / 0.0625F);
        return (px > 0 ? "+" : "") + px + "px";
    }

    private static Set<BlockPos> collectChainPositions(Level level, BlockPos pos) {
        return withWidePartners(level, BntBeltTension.chainPositions(level, pos));
    }

    private static Set<BlockPos> withWidePartners(Level level, Set<BlockPos> positions) {
        Set<BlockPos> result = new LinkedHashSet<>(positions);

        for (BlockPos nodePos : positions) {
            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, nodePos);
            if (partnerPos != null) {
                result.add(partnerPos);
            }
            if (BntSuspension.partner(level, nodePos, level.getBlockEntity(nodePos)) instanceof KineticBlockEntity bogie) {
                result.add(bogie.getBlockPos());
            }
        }

        return result;
    }
}
