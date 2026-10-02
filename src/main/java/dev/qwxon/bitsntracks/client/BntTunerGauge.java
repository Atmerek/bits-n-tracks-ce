package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.physics.BntTuning;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntTunerGauge {
    public static final float REST_ANGLE = -180.0F;
    public static final float STEP_ANGLE = 30.0F;
    private static final float TURN_KICK = 8.0F;
    private static final float PITCH_KICK = 8.0F;
    private static final float LIFT_KICK = 260.0F;
    private static final float LIMIT_KICK = 120.0F;
    private static final double MAX_FRAME = 0.1;

    private static final BntTunerNeedle NEEDLE = new BntTunerNeedle();
    private static float target = REST_ANGLE;
    private static boolean measuring;
    private static boolean held;
    private static long lastNanos = -1L;
    private static float lastTurn;
    private static float lastPitchTurn;
    private static double lastLift;
    private static float staged = Float.NaN;

    private BntTunerGauge() {
    }

    public static float levelAngle(int level) {
        return REST_ANGLE + STEP_ANGLE * level;
    }

    public static float needleAngle(ItemStack stack) {
        if (!Float.isNaN(staged)) {
            return staged;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return held && player != null && stack == player.getMainHandItem() ? NEEDLE.angle() : REST_ANGLE;
    }

    public static void stage(float angle) {
        staged = angle;
    }

    public static void unstage() {
        staged = Float.NaN;
    }

    @Nullable
    public static BlockPos tunableTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return BntSuspension.hasPiece(minecraft.level.getBlockEntity(pos)) ? pos : null;
    }

    public static int readingAt(BlockPos pos, BntTuning setting) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? BntTuning.DEFAULT : setting.levelOf(minecraft.level.getBlockEntity(pos));
    }

    public static void kick(int direction) {
        NEEDLE.push(LIMIT_KICK * Integer.signum(direction));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof SuspensionToolItem)) {
            held = false;
            NEEDLE.rest();
            target = REST_ANGLE;
            measuring = false;
            return;
        }

        BlockPos pos = tunableTarget();
        measuring = pos != null;
        target = levelAngle(pos == null ? 0 : readingAt(pos, SuspensionToolItem.setting(player.getMainHandItem())));

        float turn = player.getYRot() - player.yRotO;
        float pitchTurn = player.getXRot() - player.xRotO;
        double lift = player.getDeltaMovement().y;
        if (held) {
            float radians = NEEDLE.angle() * Mth.DEG_TO_RAD;
            NEEDLE.push(TURN_KICK * (turn - lastTurn) * Mth.cos(radians)
                + PITCH_KICK * (pitchTurn - lastPitchTurn) * Mth.sin(radians)
                + LIFT_KICK * (float)(lift - lastLift) * Mth.sin(radians));
        }
        lastTurn = turn;
        lastPitchTurn = pitchTurn;
        lastLift = lift;
        held = true;
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        long now = Util.getNanos();
        double elapsed = lastNanos < 0L ? 0.0 : (now - lastNanos) / 1.0E9;
        lastNanos = now;
        if (!held || Minecraft.getInstance().isPaused()) {
            return;
        }
        NEEDLE.advance(Math.min(elapsed, MAX_FRAME), target, measuring);
    }
}
