package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltRefit;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksDataComponents;
import dev.qwxon.bitsntracks.interaction.BntToolMessages;
import dev.qwxon.bitsntracks.interaction.BntToolSounds;
import dev.qwxon.bitsntracks.interaction.BntCogwheelInteraction;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class CogAlignmentLeverItem extends Item {
    public static final float STEP = 0.0625F;
    public static final float LIMIT = 1.0F;

    public CogAlignmentLeverItem(Properties properties) {
        super(properties);
    }

    public static boolean wholeTrack(ItemStack stack) {
        return stack.getOrDefault(BitsNTracksDataComponents.ALIGNMENT_WHOLE_TRACK.get(), false);
    }

    public static void setWholeTrack(ItemStack stack, boolean wholeTrack) {
        stack.set(BitsNTracksDataComponents.ALIGNMENT_WHOLE_TRACK.get(), wholeTrack);
    }

    public static BntLeverMode mode(ItemStack stack) {
        return BntLeverMode.byIndex(stack.getOrDefault(BitsNTracksDataComponents.ALIGNMENT_MODE.get(), 0));
    }

    public static void setMode(ItemStack stack, BntLeverMode mode) {
        stack.set(BitsNTracksDataComponents.ALIGNMENT_MODE.get(), mode.ordinal());
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bits_n_tracks.alignment.mode",
                Component.translatable(mode(stack).translationKey()).withStyle(ChatFormatting.GOLD))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bits_n_tracks.alignment.scope",
                BntToolScope.name(wholeTrack(stack)).withStyle(ChatFormatting.GOLD))
            .withStyle(ChatFormatting.GRAY));
    }

    public static void nudge(Level level, Player player, BlockPos pos, Axis moveAxis, int steps, boolean wholeTrack) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (steps == 0 || !state.hasProperty(BlockStateProperties.AXIS)
            || !(be instanceof KineticBlockEntity kinetic) || !(be instanceof KineticBlockEntityPhysicsAccess access)) {
            return;
        }

        Axis blockAxis = state.getValue(BlockStateProperties.AXIS);
        float delta = STEP * steps;
        if (moveAxis == blockAxis && !wholeTrack && BntSuspension.isBogie(be)) {
            BntToolMessages.send(player, Component.translatable("bits_n_tracks.tool_panel.message.bogie.axle"));
            BntToolSounds.refuse(player);
            return;
        }

        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
        Set<BlockPos> moved = new LinkedHashSet<>();
        boolean changed = false;
        if (moveAxis != Axis.Y && moveAxis != blockAxis && BntSuspension.isBogie(be)) {
            Vec3 across = BntSuspension.across(blockAxis);
            double outward = -Integer.signum(access.bnt$getSuspensionSide()) * delta * (moveAxis == Axis.X ? across.x : across.z);
            Set<BlockPos> spread = spreadBogies(level, wholeTrack ? collectChainPositions(level, pos) : Set.of(pos), outward, LIMIT);
            if (spread.isEmpty()) {
                BntToolMessages.send(player, Component.translatable(outward < 0.0
                    ? "bits_n_tracks.tool_panel.message.bogie.closest"
                    : "bits_n_tracks.tool_panel.message.bogie.farthest"));
                BntToolSounds.refuse(player);
                return;
            }
            changed = true;
            if (wholeTrack) {
                moved.addAll(spread);
            }
        } else if (wholeTrack) {
            boolean physics = access.bnt$isPhysicsEnabled();
            for (BlockPos nodePos : collectChainPositions(level, pos)) {
                BlockEntity nodeBe = level.getBlockEntity(nodePos);
                if (nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess
                    && nodeAccess.bnt$isPhysicsEnabled() == physics) {
                    changed |= shiftAxis(nodeAccess, moveAxis, delta, LIMIT, BntSuspension.isBogie(nodeBe));
                    moved.add(nodePos);
                }
            }
            for (BlockPos nodePos : Set.copyOf(moved)) {
                if (level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                    for (KineticBlockEntity cog : BntSuspension.bogieCogs(level, nodePos)) {
                        if (moved.add(cog.getBlockPos())) {
                            ((KineticBlockEntityPhysicsAccess)cog).bnt$setAlignmentOffsetY(nodeAccess.bnt$getAlignmentOffsetY());
                        }
                    }
                }
            }
        } else {
            changed = shiftAxis(access, moveAxis, delta, LIMIT, BntSuspension.isBogie(be));
            for (KineticBlockEntity cog : BntSuspension.bogieCogs(level, pos)) {
                if (cog != be) {
                    ((KineticBlockEntityPhysicsAccess)cog).bnt$setAlignmentOffsetY(access.bnt$getAlignmentOffsetY());
                    cog.setChanged();
                    cog.sendData();
                }
            }
        }

        if (!moved.isEmpty()) {
            for (BlockPos nodePos : moved) {
                if (level.getBlockEntity(nodePos) instanceof KineticBlockEntity node) {
                    node.setChanged();
                    node.sendData();
                }
            }
        } else {
            kinetic.setChanged();
            kinetic.sendData();
            BntCogwheelPairing.pushSettingsToPartner(level, pos);
        }

        BntChainEngagement.refresh(level, pos, engagementBefore);
        BntBeltRefit.queue(level, pos);
        if (!changed) {
            BntToolSounds.refuse(player);
            return;
        }
        BntToolSounds.adjust(level, pos, (offset(access, moveAxis) + LIMIT) / (2.0F * LIMIT));
        Component message = shiftMessage(level, pos, BntCogwheelPairing.countWheels(level, moved));
        if (message != null) {
            BntToolMessages.send(player, message);
        }
    }

    private static float offset(KineticBlockEntityPhysicsAccess access, Axis axis) {
        return switch (axis) {
            case X -> access.bnt$getAlignmentOffsetX();
            case Y -> access.bnt$getAlignmentOffsetY();
            case Z -> access.bnt$getAlignmentOffsetZ();
        };
    }

    public static void toggleVisibility(Level level, Player player, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kinetic) || !(be instanceof KineticBlockEntityPhysicsAccess access)) {
            return;
        }

        access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
        kinetic.setChanged();
        kinetic.sendData();
        BntCogwheelPairing.pushSettingsToPartner(level, pos);
        BntToolSounds.toggle(level, pos, !access.bnt$isHiddenByLever());
    }

    public static void resize(Level level, Player player, BlockPos pos, int direction, boolean wholeTrack) {
        BlockState aimed = level.getBlockState(pos);
        if (direction == 0 || !aimed.hasProperty(BntCogwheelWidth.TRIM)) {
            BntToolSounds.refuse(player);
            return;
        }
        Set<BlockPos> positions = new LinkedHashSet<>();
        if (wholeTrack) {
            positions.addAll(collectChainPositions(level, pos));
        } else {
            positions.add(pos);
            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
            if (partnerPos != null) {
                positions.add(partnerPos);
            }
        }
        if (!BntCogwheelWidth.apply(level, positions, BntCogwheelWidth.stepped(aimed, direction))) {
            BntToolSounds.refuse(player);
            return;
        }
        BlockState state = level.getBlockState(pos);
        BntToolSounds.adjust(level, pos, BntCogwheelWidth.quarters(state) / (float)BntCogwheelWidth.full(state));
    }

    public static void reset(Level level, Player player, BlockPos pos) {
        Map<BlockPos, Integer> engagementBefore = BntChainEngagement.snapshot(level, pos);
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
        BntToolSounds.reset(level, pos);
        BntToolMessages.send(player, Component.translatable("bits_n_tracks.tool_panel.message.reset"));
    }

    private static boolean shiftAxis(KineticBlockEntityPhysicsAccess access, Axis axis, float delta, float limit, boolean bogie) {
        float before = offset(access, axis);
        shiftClamped(access, axis, delta, limit, bogie);
        return offset(access, axis) != before;
    }

    private static void shiftClamped(KineticBlockEntityPhysicsAccess access, Axis axis, float delta, float limit, boolean bogie) {
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
                List<KineticBlockEntity> cogs = BntSuspension.bogieCogs(level, nodePos);
                for (KineticBlockEntity cog : cogs) {
                    seen.add(cog.getBlockPos());
                }
                double current = BntSuspension.bogieSpread(nodeBe);
                double next = Mth.clamp(current + outward, BntSuspension.closestBogieSpread(nodeBe.getBlockState()), limit);
                if (Math.abs(next - current) >= 1.0E-4) {
                    BntSuspension.spreadBogie((KineticBlockEntity)nodeBe, partner, next);
                    for (KineticBlockEntity cog : cogs) {
                        spread.add(cog.getBlockPos());
                    }
                }
            }
        }
        return spread;
    }

    private static Component shiftMessage(Level level, BlockPos pos, int wheels) {
        if (wheels == 0) {
            return BntCogwheelInteraction.trackStatus(level, pos);
        }
        return Component.translatable(
            wheels == 1 ? "bits_n_tracks.tool_panel.message.moved.single" : "bits_n_tracks.tool_panel.message.moved", wheels);
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
            for (KineticBlockEntity cog : BntSuspension.bogieCogs(level, nodePos)) {
                result.add(cog.getBlockPos());
            }
        }

        return result;
    }
}
