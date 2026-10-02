package dev.qwxon.bitsntracks.client.ponder;

import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

final class BntBobBody {
    static final double GRAVITY = 9.81;
    static final double FLOOR = 1.0;

    private static final double PITCH_INERTIA = 3.0;
    private static final double ROLL_INERTIA = 2.0;
    private static final Matrix3dc INVERSE_INERTIA = new Matrix3d().scaling(1.0 / PITCH_INERTIA, 1.0, 1.0 / ROLL_INERTIA);
    private static final double STIFFNESS = BntTuning.STIFFNESS.scale(BntTuning.DEFAULT);
    private static final double DAMPING = BntTuning.DAMPING.scale(BntTuning.DEFAULT);
    private static final double EXTEND_RATE = 4.0;

    interface Ground {
        List<Box> under(double x);
    }

    record Box(double front, double back, double top) {
    }

    static final class Wheel {
        final BlockPos pos;
        final Vec3 centre;
        final double radius;
        DoubleFunction<Vec3> arm;
        double upCap;
        double downCap;
        double ride;
        double rise;
        double hold;
        double floor;
        double push;
        double shown;
        double shownBefore;

        Wheel(BlockPos pos, Vec3 centre, double radius) {
            this.pos = pos;
            this.centre = centre;
            this.radius = radius;
        }

        Vec3 at(double rise) {
            return arm == null ? centre : centre.add(arm.apply(rise));
        }
    }

    private final Vec3 pivot;
    private final List<List<Wheel>> tracks = new ArrayList<>();
    private double heave;
    private double heaveRate;
    private double pitch;
    private double pitchRate;
    private double roll;
    private double rollRate;
    private boolean free;

    BntBobBody(Vec3 pivot) {
        this.pivot = pivot;
    }

    void track(List<Wheel> wheels) {
        tracks.add(wheels);
    }

    Vec3 pivot() {
        return pivot;
    }

    void place(double heave, double pitch, double roll) {
        this.heave = heave;
        this.pitch = pitch;
        this.roll = roll;
        heaveRate = 0.0;
        pitchRate = 0.0;
        rollRate = 0.0;
    }

    void release() {
        free = true;
        heaveRate = 0.0;
        pitchRate = 0.0;
        rollRate = 0.0;
        for (List<Wheel> track : tracks) {
            for (Wheel wheel : track) {
                wheel.push = 0.0;
            }
        }
    }

    void hold() {
        free = false;
    }

    boolean free() {
        return free;
    }

    double heave() {
        return heave;
    }

    double pitch() {
        return pitch;
    }

    double roll() {
        return roll;
    }

    Vec3 world(Vec3 p) {
        return transform(p, pivot, heave, pitch, roll);
    }

    static Vec3 transform(Vec3 p, Vec3 pivot, double heave, double pitch, double roll) {
        double x = p.x - pivot.x;
        double y = p.y - pivot.y;
        double z = p.z - pivot.z;
        double cr = Math.cos(roll);
        double sr = Math.sin(roll);
        double x1 = x * cr - y * sr;
        double y1 = x * sr + y * cr;
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double y2 = y1 * cp - z * sp;
        double z2 = y1 * sp + z * cp;
        return new Vec3(pivot.x + x1, pivot.y + heave + y2, pivot.z + z2);
    }

    void step(double dt, Ground ground) {
        if (!free) {
            return;
        }

        List<BntPhysicsEvents.SupportContact> supports = new ArrayList<>();
        List<Wheel> order = new ArrayList<>();
        for (List<Wheel> track : tracks) {
            List<Box> boxes = ground.under(track.get(0).centre.x);
            for (Wheel wheel : track) {
                if (wheel.pos == null) {
                    continue;
                }
                Vec3 seat = world(wheel.centre);
                double rise = support(boxes, seat.z, wheel.radius) - seat.y;
                wheel.rise = wheel.arm == null ? 0.0 : Mth.clamp(rise, -wheel.downCap, wheel.upCap);
                Vec3 point = world(wheel.at(wheel.rise)).add(0.0, -wheel.radius, 0.0);
                double speed = heaveRate + rollRate * (point.x - pivot.x) - pitchRate * (point.z - pivot.z);
                supports.add(new BntPhysicsEvents.SupportContact(new Vector3d(point.x, point.y, point.z), speed,
                    wheel.arm != null, rise + wheel.floor - wheel.hold, wheel.upCap, wheel.downCap, wheel.ride, STIFFNESS, DAMPING, wheel.push));
                order.add(wheel);
            }
        }

        Vector3d centre = new Vector3d(pivot.x, pivot.y + heave, pivot.z);
        double[] pushes = BntPhysicsEvents.solveSupport(supports, 1.0, INVERSE_INERTIA, centre, GRAVITY, dt);
        heaveRate -= GRAVITY * dt;
        for (int i = 0; i < pushes.length; i++) {
            order.get(i).push = pushes[i];
            Vector3dc point = supports.get(i).point();
            heaveRate += pushes[i];
            pitchRate += pushes[i] * (pivot.z - point.z()) / PITCH_INERTIA;
            rollRate += pushes[i] * (point.x() - pivot.x) / ROLL_INERTIA;
        }
        heave += heaveRate * dt;
        pitch += pitchRate * dt;
        roll += rollRate * dt;
    }

    void settle(double dt, Ground ground, int steps) {
        for (int i = 0; i < steps; i++) {
            step(dt, ground);
        }
        heaveRate = 0.0;
        pitchRate = 0.0;
        rollRate = 0.0;
        for (List<Wheel> track : tracks) {
            for (Wheel wheel : track) {
                wheel.shown = wheel.arm == null ? 0.0 : -wheel.rise;
                wheel.shownBefore = wheel.shown;
            }
        }
    }

    void showWheels(double dt, boolean rested) {
        for (List<Wheel> track : tracks) {
            for (Wheel wheel : track) {
                wheel.shownBefore = wheel.shown;
                double drop = wheel.arm == null || rested ? 0.0 : -wheel.rise;
                wheel.shown = drop > wheel.shown ? Math.min(drop, wheel.shown + EXTEND_RATE * dt) : drop;
            }
        }
    }

    static double support(List<Box> boxes, double z, double radius) {
        double best = FLOOR + radius;
        for (Box box : boxes) {
            double gap = z < box.front() ? box.front() - z : z > box.back() ? z - box.back() : 0.0;
            if (gap < radius) {
                best = Math.max(best, box.top() + Math.sqrt(radius * radius - gap * gap));
            }
        }
        return best;
    }
}
