package dev.qwxon.bitsntracks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntTunerNeedle {
    private static final float HIGH_STOP = 172.0F;
    private static final float SPRING = 300.0F;
    private static final float DAMPING = 12.5F;
    private static final float BOUNCE = 0.35F;
    private static final float TREMOR = 130.0F;
    private static final double SUBSTEP = 1.0 / 240.0;

    private float angle = BntTunerGauge.REST_ANGLE;
    private float velocity;
    private double clock;

    public float angle() {
        return angle;
    }

    public void push(float change) {
        velocity += change;
    }

    public void rest() {
        angle = BntTunerGauge.REST_ANGLE;
        velocity = 0.0F;
    }

    public void advance(double seconds, float target, boolean measuring) {
        double remaining = seconds;
        while (remaining > 0.0) {
            double step = Math.min(remaining, SUBSTEP);
            remaining -= step;
            clock += step;
            float tremor = measuring ? TREMOR * tremor(clock) : 0.0F;
            float acceleration = SPRING * (target - angle) - DAMPING * velocity + tremor;
            velocity += acceleration * (float)step;
            angle += velocity * (float)step;
            if (angle < BntTunerGauge.REST_ANGLE) {
                angle = BntTunerGauge.REST_ANGLE;
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
