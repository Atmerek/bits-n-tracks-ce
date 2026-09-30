package dev.qwxon.bitsntracks.client.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BntPonderCamera {
    private static final Map<Object, BntPonderCamera> CAMERAS = new WeakHashMap<>();

    private final Vec3 pivot;
    private Vec3 focus;
    private Vec3 previousFocus;
    private double zoom = 1.0;
    private double previousZoom = 1.0;

    private BntPonderCamera(Vec3 pivot) {
        this.pivot = pivot;
        focus = pivot;
        previousFocus = pivot;
    }

    public static void frame(Object transform, PoseStack ms, float pt) {
        BntPonderCamera camera = CAMERAS.get(transform);
        if (camera == null) {
            return;
        }
        Vec3 focus = camera.focus(pt);
        float zoom = (float)camera.zoom(pt);
        ms.translate(camera.pivot.x, camera.pivot.y, camera.pivot.z);
        ms.scale(zoom, zoom, zoom);
        ms.translate(-focus.x, -focus.y, -focus.z);
    }

    public static Vec3 unframe(Object transform, Vec3 vec, float pt) {
        BntPonderCamera camera = CAMERAS.get(transform);
        if (camera == null) {
            return vec;
        }
        return vec.subtract(camera.pivot).scale(1.0 / camera.zoom(pt)).add(camera.focus(pt));
    }

    static PonderInstruction glide(Vec3 focus, double zoom, float tilt, float turn, int ticks) {
        return new Glide(focus, zoom, tilt, turn, ticks);
    }

    static PonderInstruction glideHome(float tilt, float turn, int ticks) {
        return new Glide(null, 1.0, tilt, turn, ticks);
    }

    private static Vec3 pivot(PonderScene scene) {
        double half = scene.getBasePlateSize() / 2.0;
        return new Vec3(half + scene.getBasePlateOffsetX(), 1.0 - scene.getYOffset(), half + scene.getBasePlateOffsetZ());
    }

    private Vec3 focus(float pt) {
        return previousFocus.lerp(focus, pt);
    }

    private double zoom(float pt) {
        return Mth.lerp(pt, previousZoom, zoom);
    }

    private static void turn(LerpedFloat angle, double value) {
        angle.setValue(value);
        angle.chase(value, 1.0, LerpedFloat.Chaser.EXP);
    }

    private static final class Glide extends TickingInstruction {
        private final Vec3 target;
        private final double targetZoom;
        private final float tilt;
        private final float turn;
        private Vec3 start;
        private double startZoom;
        private float startTilt;
        private float startTurn;

        Glide(Vec3 target, double targetZoom, float tilt, float turn, int ticks) {
            super(false, ticks);
            this.target = target;
            this.targetZoom = targetZoom;
            this.tilt = tilt;
            this.turn = turn;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            CAMERAS.remove(scene.getTransform());
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            BntPonderCamera camera = CAMERAS.computeIfAbsent(scene.getTransform(), ignored -> new BntPonderCamera(pivot(scene)));
            start = camera.focus;
            startZoom = camera.zoom;
            startTilt = scene.getTransform().xRotation.getValue();
            startTurn = scene.getTransform().yRotation.getValue();
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            BntPonderCamera camera = CAMERAS.get(scene.getTransform());
            if (camera == null) {
                return;
            }
            double t = totalTicks == 0 ? 1.0 : (totalTicks - remainingTicks) / (double)totalTicks;
            double eased = t * t * (3.0 - 2.0 * t);
            camera.previousFocus = camera.focus;
            camera.previousZoom = camera.zoom;
            camera.focus = start.lerp(target == null ? camera.pivot : target, eased);
            camera.zoom = Math.exp(Mth.lerp(eased, Math.log(startZoom), Math.log(targetZoom)));
            turn(scene.getTransform().xRotation, startTilt + tilt * eased);
            turn(scene.getTransform().yRotation, startTurn + turn * eased);
            if (remainingTicks == 0) {
                camera.previousFocus = camera.focus;
                camera.previousZoom = camera.zoom;
            }
        }
    }
}
