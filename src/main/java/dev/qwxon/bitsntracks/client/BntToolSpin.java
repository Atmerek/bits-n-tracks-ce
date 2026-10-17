package dev.qwxon.bitsntracks.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@OnlyIn(Dist.CLIENT)
public final class BntToolSpin {
    private static final float IDLE_SPEED = 160.0F;
    private static final float STEP_KICK = 720.0F;
    private static final float MAX_EXTRA = 2160.0F;
    private static final float SETTLE_RATE = 4.0F;
    private static final double MAX_FRAME = 0.1;

    private static float angle;
    private static float speed = IDLE_SPEED;
    private static long lastNanos = -1L;

    private BntToolSpin() {
    }

    public static float angle() {
        return angle;
    }

    public static void kick(int steps) {
        speed = IDLE_SPEED + Mth.clamp(speed - IDLE_SPEED + STEP_KICK * steps, -MAX_EXTRA, MAX_EXTRA);
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        long now = Util.getNanos();
        float elapsed = lastNanos < 0L ? 0.0F : (float)Math.min((now - lastNanos) / 1.0E9, MAX_FRAME);
        lastNanos = now;
        angle = (angle + speed * elapsed) % 360.0F;
        speed = IDLE_SPEED + (speed - IDLE_SPEED) * (float)Math.exp(-SETTLE_RATE * elapsed);
    }
}
