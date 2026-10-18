package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.interaction.BntBeltTensionPayload;
import dev.qwxon.bitsntracks.interaction.BntLeverPayload;
import dev.qwxon.bitsntracks.interaction.BntReach;
import dev.qwxon.bitsntracks.interaction.BntTuningPayload;
import dev.qwxon.bitsntracks.interaction.BntToolMessages;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntToolState {
    private static final long RESET_WINDOW_MILLIS = 3000L;
    private static final int TENSION_STEPS_PER_TICK = 4;

    @Nullable
    static BlockPos leverCog;
    @Nullable
    static BlockPos leverChain;
    @Nullable
    static BlockPos tunerCog;
    @Nullable
    static BlockPos resetArmed;
    private static long resetArmedAt;
    private static int axisRow;
    private static int pendingOffset;
    private static int pendingTension;
    private static int pendingTuning;
    @Nullable
    private static ClientLevel lastLevel;

    private BntToolState() {
    }

    @Nullable
    public static BlockPos tunerTarget() {
        return tunerCog;
    }

    static Axis[] axes(Axis blockAxis) {
        return blockAxis == Axis.Y
            ? new Axis[]{Axis.Y, Axis.X, Axis.Z}
            : new Axis[]{blockAxis, Axis.Y, blockAxis == Axis.X ? Axis.Z : Axis.X};
    }

    static String axisKey(Axis blockAxis, Axis axis) {
        if (axis == blockAxis) {
            return "bits_n_tracks.tool_panel.axis.along";
        }
        if (blockAxis == Axis.Y) {
            return axis == Axis.X ? "bits_n_tracks.tool_panel.axis.east_west" : "bits_n_tracks.tool_panel.axis.north_south";
        }
        return axis == Axis.Y ? "bits_n_tracks.tool_panel.axis.vertical" : "bits_n_tracks.tool_panel.axis.across";
    }

    static boolean axisAllowed(ClientLevel level, BlockPos pos, Axis blockAxis, Axis axis) {
        return axis != blockAxis || !BntSuspension.isBogie(level.getBlockEntity(pos));
    }

    @Nullable
    static Axis blockAxis(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.AXIS) ? state.getValue(BlockStateProperties.AXIS) : null;
    }

    static int axisRow(ClientLevel level, BlockPos pos) {
        Axis blockAxis = blockAxis(level, pos);
        if (blockAxis == null) {
            return 0;
        }
        Axis[] axes = axes(blockAxis);
        for (int i = 0; i < axes.length; i++) {
            int row = Math.floorMod(axisRow + i, axes.length);
            if (axisAllowed(level, pos, blockAxis, axes[row])) {
                return row;
            }
        }
        return axisRow;
    }

    static void cycleAxis(ClientLevel level, BlockPos pos, int direction) {
        Axis blockAxis = blockAxis(level, pos);
        if (blockAxis == null) {
            return;
        }
        Axis[] axes = axes(blockAxis);
        int row = axisRow(level, pos);
        for (int i = 1; i <= axes.length; i++) {
            int next = Math.floorMod(row + i * direction, axes.length);
            if (axisAllowed(level, pos, blockAxis, axes[next])) {
                axisRow = next;
                return;
            }
        }
    }

    static void selectLeverCog(@Nullable BlockPos pos) {
        leverCog = pos;
        pendingOffset = 0;
    }

    static void selectLeverChain(@Nullable BlockPos pos) {
        leverChain = pos;
        pendingTension = 0;
    }

    static void selectTunerCog(@Nullable BlockPos pos) {
        tunerCog = pos;
        pendingTuning = 0;
    }

    static boolean armReset(BlockPos pos) {
        long now = Util.getMillis();
        if (pos.equals(resetArmed) && now - resetArmedAt < RESET_WINDOW_MILLIS) {
            resetArmed = null;
            return true;
        }
        resetArmed = pos.immutable();
        resetArmedAt = now;
        return false;
    }

    static boolean resetArmedFor(BlockPos pos) {
        return pos.equals(resetArmed) && Util.getMillis() - resetArmedAt < RESET_WINDOW_MILLIS;
    }

    static void addOffsetSteps(int steps) {
        pendingOffset += steps;
    }

    static void addTensionSteps(int steps) {
        pendingTension += steps;
    }

    static void addTuningSteps(int steps) {
        pendingTuning += steps;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || level != lastLevel) {
            lastLevel = level;
            clearLever();
            selectTunerCog(null);
            BntToolMessages.clear();
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof CogAlignmentLeverItem) {
            BntLeverMode mode = CogAlignmentLeverItem.mode(stack);
            if (mode != BntLeverMode.OFFSET || !validCog(level, player, leverCog)) {
                selectLeverCog(null);
            }
            if (mode != BntLeverMode.TENSION || leverChain != null
                && (!HiddenCogwheelCompat.isOwnChain(level, leverChain) || !BntReach.track(level, player, leverChain))) {
                selectLeverChain(null);
            }
            if (mode != BntLeverMode.RESET) {
                resetArmed = null;
            }
            flushLever(stack);
        } else {
            clearLever();
        }

        if (stack.getItem() instanceof SuspensionToolItem) {
            if (tunerCog != null && (!BntSuspension.hasPiece(level.getBlockEntity(tunerCog)) || !BntReach.block(level, player, tunerCog))) {
                selectTunerCog(null);
            }
            if (tunerCog != null && pendingTuning != 0) {
                PacketDistributor.sendToServer(new BntTuningPayload(
                    tunerCog, SuspensionToolItem.setting(stack).ordinal(), pendingTuning, SuspensionToolItem.wholeTrack(stack)));
                pendingTuning = 0;
            }
        } else {
            selectTunerCog(null);
        }
    }

    private static void flushLever(ItemStack stack) {
        ClientLevel level = Minecraft.getInstance().level;
        if (leverCog != null && pendingOffset != 0 && level != null) {
            Axis blockAxis = blockAxis(level, leverCog);
            if (blockAxis != null) {
                Axis axis = axes(blockAxis)[axisRow(level, leverCog)];
                PacketDistributor.sendToServer(new BntLeverPayload(
                    leverCog, BntLeverPayload.OFFSET, axis.ordinal(), pendingOffset, CogAlignmentLeverItem.wholeTrack(stack)));
            }
            pendingOffset = 0;
        }
        if (leverChain != null && pendingTension != 0) {
            int steps = Math.min(Math.abs(pendingTension), TENSION_STEPS_PER_TICK);
            for (int i = 0; i < steps; i++) {
                PacketDistributor.sendToServer(new BntBeltTensionPayload(leverChain, pendingTension > 0));
            }
            pendingTension -= Integer.signum(pendingTension) * steps;
        }
    }

    private static void clearLever() {
        selectLeverCog(null);
        selectLeverChain(null);
        resetArmed = null;
    }

    private static boolean validCog(ClientLevel level, LocalPlayer player, @Nullable BlockPos pos) {
        return pos != null && HiddenCogwheelCompat.isOwnCogwheel(level, pos) && BntReach.block(level, player, pos);
    }

    @Nullable
    static BlockPos chainController(ClientLevel level, BlockPos pos) {
        BlockPos controller = BntBeltTension.controllerPos(level, pos);
        return HiddenCogwheelCompat.isOwnChain(level, controller) ? controller : null;
    }
}
