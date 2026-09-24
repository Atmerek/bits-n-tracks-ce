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
    private static final float HIGH_STOP = 172.0F;
    private static final float SPRING = 300.0F;
    private static final float DAMPING = 12.5F;
    private static final float BOUNCE = 0.35F;
    private static final float TREMOR = 130.0F;
    private static final float TURN_KICK = 8.0F;
    private static final float PITCH_KICK = 8.0F;
    private static final float LIFT_KICK = 260.0F;
    private static final float LIMIT_KICK = 120.0F;
    private static final double MAX_FRAME = 0.1;
    private static final double SUBSTEP = 1.0 / 240.0;

    private static float angle = REST_ANGLE;
    private static float velocity;
    private static float target = REST_ANGLE;
    private static boolean measuring;
    private static boolean held;
    private static long lastNanos = -1L;
    private static double clock;
    private static float lastTurn;
    private static float lastPitchTurn;
    private static double lastLift;

    private BntTunerGauge() {
    }

    public static float levelAngle(int level) {
        return REST_ANGLE + STEP_ANGLE * level;
    }

    public static float needleAngle(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        return held && player != null && stack == player.getMainHandItem() ? angle : REST_ANGLE;
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
        velocity += LIMIT_KICK * Integer.signum(direction);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof SuspensionToolItem)) {
            held = false;
            angle = REST_ANGLE;
            velocity = 0.0F;
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
            float radians = angle * Mth.DEG_TO_RAD;
            velocity += TURN_KICK * (turn - lastTurn) * Mth.cos(radians)
                + PITCH_KICK * (pitchTurn - lastPitchTurn) * Mth.sin(radians)
                + LIFT_KICK * (float)(lift - lastLift) * Mth.sin(radians);
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

        double remaining = Math.min(elapsed, MAX_FRAME);
        while (remaining > 0.0) {
            double step = Math.min(remaining, SUBSTEP);
            remaining -= step;
            clock += step;
            float tremor = measuring ? TREMOR * tremor(clock) : 0.0F;
            float acceleration = SPRING * (target - angle) - DAMPING * velocity + tremor;
            velocity += acceleration * (float)step;
            angle += velocity * (float)step;
            if (angle < REST_ANGLE) {
                angle = REST_ANGLE;
                velocity = velocity < 0.0F ? -velocity * BOUNCE : velocity;
            } else if (angle > HIGH_STOP) {
                angle = HIGH_STOP;
                velocity = velocity > 0.0F ? -velocity * BOUNCE : velocity;
            }
        }
    }

    private static float tremor(double time) {
        double tau = Math.PI * 2.0;
        return (float)(Math.sin(tau * 1.3 * time) + 0.6 * Math.sin(tau * 2.9 * time + 1.1) + 0.4 * Math.sin(tau * 5.3 * time + 2.3));
    }
}
