package dev.qwxon.bitsntracks.client.ponder;

final class BntTankBody {
    static final double GRAVITY = 9.81;
    static final double FLOOR = 1.0;

    static final double PIVOT_Y = 3.5;
    static final double PIVOT_Z = 6.0;
    static final double AXLE_Y = 2.5;
    static final double[] AXLES = {3.5, 5.5, 6.5, 8.5};
    static final double SPROCKET_Y = 3.5;
    static final double FRONT_Z = 2.5;
    static final double REAR_Z = 9.5;

    static final double WHEEL_RADIUS = 0.6;
    static final double SPROCKET_RADIUS = 0.79;
    static final double TRAVEL = 0.65;

    private static final double SPRING = 8.2;
    private static final double DAMPER = 1.0;
    private static final double BUMP_START = 0.55;
    private static final double BUMP_SPRING = 220.0;
    private static final double BUMP_DAMPER = 9.0;
    private static final double HARD_SPRING = 2600.0;
    private static final double HARD_DAMPER = 40.0;
    private static final double BELT_SPRING = 80.0;
    private static final double DAMPER_LIMIT = 10.0;
    private static final double INERTIA = 5.2;
    private static final double PITCH_DRAG = 30.0;
    private static final double EXTEND_RATE = 6.0;
    private static final int SOLVE_PASSES = 3;

    interface Ground {
        int count();

        double front(int box);

        double back(int box);

        double top(int box);
    }

    private double heave;
    private double heaveRate;
    private double pitch;
    private double pitchRate;
    private boolean free;
    private final double[] drop = new double[AXLES.length];
    private final double[] shown = new double[AXLES.length];
    private final double[] squeeze = new double[AXLES.length];
    private double frontPress;
    private double rearPress;

    void reset() {
        heave = 0.0;
        heaveRate = 0.0;
        pitch = 0.0;
        pitchRate = 0.0;
        free = false;
        frontPress = 0.0;
        rearPress = 0.0;
        for (int i = 0; i < AXLES.length; i++) {
            drop[i] = 0.0;
            shown[i] = 0.0;
            squeeze[i] = 0.0;
        }
    }

    void release() {
        free = true;
    }

    boolean released() {
        return free;
    }

    double heave() {
        return heave;
    }

    double pitch() {
        return pitch;
    }

    double shownDrop(int axle) {
        return shown[axle];
    }

    void step(double dt, double acceleration, double tension, Ground ground) {
        if (!free) {
            return;
        }

        double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        double[] target = new double[AXLES.length];
        double[] mountY = new double[AXLES.length];
        double[] mountZ = new double[AXLES.length];
        for (int i = 0; i < AXLES.length; i++) {
            mountY[i] = worldY(AXLE_Y, AXLES[i], cos, sin);
            mountZ[i] = worldZ(AXLE_Y, AXLES[i], cos, sin);
            double z = mountZ[i];
            double reach = (mountY[i] - support(ground, z, WHEEL_RADIUS)) / cos;
            z = mountZ[i] - reach * sin;
            target[i] = (mountY[i] - support(ground, z, WHEEL_RADIUS)) / cos;
        }

        double frontY = worldY(SPROCKET_Y, FRONT_Z, cos, sin);
        double frontZ = worldZ(SPROCKET_Y, FRONT_Z, cos, sin);
        double rearY = worldY(SPROCKET_Y, REAR_Z, cos, sin);
        double rearZ = worldZ(SPROCKET_Y, REAR_Z, cos, sin);
        double front = Math.max(0.0, support(ground, frontZ, SPROCKET_RADIUS) - frontY);
        double rear = Math.max(0.0, support(ground, rearZ, SPROCKET_RADIUS) - rearY);

        double beltSpring = BELT_SPRING * Math.pow(tension, 4.0);
        double[] groundSqueeze = new double[AXLES.length];
        double[] compressed = new double[AXLES.length];
        for (int i = 0; i < AXLES.length; i++) {
            groundSqueeze[i] = Math.max(0.0, TRAVEL - target[i]);
            compressed[i] = groundSqueeze[i];
        }

        double[] carried = new double[AXLES.length + 2];
        for (int pass = 0; pass < SOLVE_PASSES; pass++) {
            double[] cz = new double[AXLES.length + 2];
            double[] cy = new double[AXLES.length + 2];
            double[] cr = new double[AXLES.length + 2];
            cz[0] = frontZ;
            cy[0] = frontY;
            cr[0] = SPROCKET_RADIUS;
            for (int i = 0; i < AXLES.length; i++) {
                double e = TRAVEL - Math.min(compressed[i], TRAVEL);
                cz[i + 1] = mountZ[i] - e * sin;
                cy[i + 1] = mountY[i] - e * cos;
                cr[i + 1] = WHEEL_RADIUS;
            }
            cz[AXLES.length + 1] = rearZ;
            cy[AXLES.length + 1] = rearY;
            cr[AXLES.length + 1] = SPROCKET_RADIUS;

            double[] load = new double[AXLES.length + 2];
            for (int s = 0; s + 1 < cz.length; s++) {
                double[] span = spanDeficit(ground, cz[s], cy[s], cr[s], cz[s + 1], cy[s + 1], cr[s + 1]);
                if (span == null) {
                    continue;
                }
                double push = beltSpring * span[0];
                load[s] += push * (1.0 - span[1]);
                load[s + 1] += push * span[1];
            }
            for (int j = 0; j < load.length; j++) {
                carried[j] = pass == 0 ? load[j] : 0.5 * (carried[j] + load[j]);
            }
            for (int i = 0; i < AXLES.length; i++) {
                compressed[i] = Math.max(groundSqueeze[i], squeezeFor(carried[i + 1]));
            }
        }

        double force = 0.0;
        double torque = 0.0;
        for (int i = 0; i < AXLES.length; i++) {
            drop[i] = TRAVEL - Math.min(compressed[i], TRAVEL);
            shown[i] = drop[i] > shown[i] ? Math.min(drop[i], shown[i] + EXTEND_RATE * dt) : drop[i];

            double rate = (compressed[i] - squeeze[i]) / dt;
            squeeze[i] = compressed[i];
            if (compressed[i] <= 0.0) {
                continue;
            }
            double damping = DAMPER * rate;
            if (compressed[i] > BUMP_START) {
                damping += BUMP_DAMPER * rate;
            }
            if (groundSqueeze[i] > TRAVEL) {
                damping += HARD_DAMPER * rate;
            }
            double push = springForce(groundSqueeze[i]) + Math.max(-DAMPER_LIMIT, Math.min(DAMPER_LIMIT, damping));
            push = Math.max(Math.max(0.0, push), carried[i + 1]);
            force += push;
            torque += push * (PIVOT_Z - mountZ[i]);
        }

        double frontRate = (front - frontPress) / dt;
        double rearRate = (rear - rearPress) / dt;
        frontPress = front;
        rearPress = rear;
        double frontPush = Math.max(0.0, carried[0]
            + (front > 0.0 ? HARD_SPRING * front + HARD_DAMPER * frontRate : 0.0));
        double rearPush = Math.max(0.0, carried[AXLES.length + 1]
            + (rear > 0.0 ? HARD_SPRING * rear + HARD_DAMPER * rearRate : 0.0));
        force += frontPush + rearPush;
        torque += frontPush * (PIVOT_Z - frontZ) + rearPush * (PIVOT_Z - rearZ);

        double height = PIVOT_Y + heave - FLOOR;
        torque += acceleration * height;

        if (force > 0.0) {
            torque -= PITCH_DRAG * pitchRate;
        }
        heaveRate += (force - GRAVITY) * dt;
        heave += heaveRate * dt;
        pitchRate += torque / INERTIA * dt;
        pitch += pitchRate * dt;
    }

    private static double springForce(double squeeze) {
        double push = SPRING * squeeze;
        if (squeeze > BUMP_START) {
            push += BUMP_SPRING * (squeeze - BUMP_START);
        }
        if (squeeze > TRAVEL) {
            push += HARD_SPRING * (squeeze - TRAVEL);
        }
        return push;
    }

    private static double squeezeFor(double load) {
        if (load <= 0.0) {
            return 0.0;
        }
        double squeeze = load <= SPRING * BUMP_START
            ? load / SPRING
            : (load + BUMP_SPRING * BUMP_START) / (SPRING + BUMP_SPRING);
        return Math.min(squeeze, TRAVEL);
    }

    private double worldY(double y, double z, double cos, double sin) {
        return PIVOT_Y + heave + (y - PIVOT_Y) * cos - (z - PIVOT_Z) * sin;
    }

    private double worldZ(double y, double z, double cos, double sin) {
        return PIVOT_Z + (y - PIVOT_Y) * sin + (z - PIVOT_Z) * cos;
    }

    static double support(Ground ground, double z, double radius) {
        double best = FLOOR + radius;
        for (int b = 0; b < ground.count(); b++) {
            double front = ground.front(b);
            double back = ground.back(b);
            double top = ground.top(b);
            double gap = z < front ? front - z : z > back ? z - back : 0.0;
            if (gap < radius) {
                best = Math.max(best, top + Math.sqrt(radius * radius - gap * gap));
            }
        }
        return best;
    }

    private static double[] spanDeficit(Ground ground, double z1, double y1, double r1, double z2, double y2, double r2) {
        double dz = z2 - z1;
        double dy = y2 - y1;
        double length = Math.sqrt(dz * dz + dy * dy);
        if (length < 1.0E-6) {
            return null;
        }
        double uz = dz / length;
        double uy = dy / length;
        double along = (r1 - r2) / length;
        double across = Math.sqrt(Math.max(0.0, 1.0 - along * along));
        double nz = along * uz - across * uy;
        double ny = along * uy + across * uz;
        if (ny > 0.0) {
            nz = along * uz + across * uy;
            ny = along * uy - across * uz;
        }
        double az = z1 + nz * r1;
        double ay = y1 + ny * r1;
        double bz = z2 + nz * r2;
        double by = y2 + ny * r2;
        if (bz - az < 1.0E-6) {
            return null;
        }

        double worst = 0.0;
        double at = 0.0;
        for (int b = 0; b < ground.count(); b++) {
            double from = Math.max(ground.front(b), az);
            double to = Math.min(ground.back(b), bz);
            if (to <= from) {
                continue;
            }
            for (double z : new double[]{from, to}) {
                double t = (z - az) / (bz - az);
                double belt = ay + (by - ay) * t;
                double poke = ground.top(b) - belt;
                if (poke > worst) {
                    worst = poke;
                    at = t;
                }
            }
        }
        return worst > 0.0 ? new double[]{worst, at} : null;
    }
}
