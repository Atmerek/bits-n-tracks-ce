package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BntTreadView {
    private static final double LINK_REACH_SQR = 32.0 * 32.0;
    private static final ThreadLocal<BntTreadView> CURRENT = ThreadLocal.withInitial(BntTreadView::new);

    private boolean seen;
    private boolean shadow;
    private double eyeX;
    private double eyeY;
    private double eyeZ;

    private BntTreadView() {
    }

    public static void begin(Matrix4f pose, Level level) {
        BntTreadView view = CURRENT.get();
        view.seen = false;
        view.shadow = BntShaderHand.shadowPass();
        if (view.shadow || level == null || level != Minecraft.getInstance().level
            || Math.abs(RenderSystem.getProjectionMatrix().m23()) < 1.0E-3F) {
            return;
        }

        Matrix4f inverse = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose).invert();
        view.eyeX = inverse.m30();
        view.eyeY = inverse.m31();
        view.eyeZ = inverse.m32();
        view.seen = Double.isFinite(view.eyeX) && Double.isFinite(view.eyeY) && Double.isFinite(view.eyeZ);
    }

    public static void end() {
        BntTreadView view = CURRENT.get();
        view.seen = false;
        view.shadow = false;
    }

    public static Vec3 eyeFrom(Vec3 point) {
        BntTreadView view = CURRENT.get();
        return view.seen ? new Vec3(view.eyeX - point.x, view.eyeY - point.y, view.eyeZ - point.z) : null;
    }

    public static boolean links(Vec3 point) {
        BntTreadView view = CURRENT.get();
        if (view.shadow) {
            return false;
        }
        if (!view.seen) {
            return true;
        }

        double dx = view.eyeX - point.x;
        double dy = view.eyeY - point.y;
        double dz = view.eyeZ - point.z;
        return dx * dx + dy * dy + dz * dz < LINK_REACH_SQR;
    }
}
