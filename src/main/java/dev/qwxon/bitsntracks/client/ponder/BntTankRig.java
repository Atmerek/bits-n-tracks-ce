package dev.qwxon.bitsntracks.client.ponder;

import dev.qwxon.bitsntracks.physics.BntPonderPhysics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class BntTankRig implements BntPonderPhysics.Stage, BntTankBody.Ground {
    static final double BELT_SPEED = 32.0 / 60.0 * 2.0 * Math.PI * 0.75;
    static final double RUNWAY_START = -50.0;
    static final double PIECE_LENGTH = 10.0;
    static final int PIECES = 11;

    private static final int SUBSTEPS = 10;
    private static final double TICK_SECONDS = 0.05;
    private static final double ACCELERATION = 6.0;
    private static final double TAKEN_AWAY_Z = 40.0;
    private static final double RUNWAY_WEST = 0.0;
    private static final double RUNWAY_EAST = 11.0;
    private static final double OBSTACLE_WEST = 1.0;
    private static final double OBSTACLE_EAST = 10.0;
    private static final Vec3 AWAY = new Vec3(0.0, -512.0, 0.0);
    private static final Vec3 PIVOT = new Vec3(5.5, BntTankBody.PIVOT_Y, BntTankBody.PIVOT_Z);

    private final BntTankBody body = new BntTankBody();
    private final Map<BlockPos, Integer> wheels = new HashMap<>();
    private final List<ElementLink<WorldSectionElement>> pieces = new ArrayList<>(Collections.nCopies(PIECES, null));
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Obstacle> active = new ArrayList<>();
    private ElementLink<WorldSectionElement> vehicle;
    private ElementLink<WorldSectionElement> floor;

    private final boolean[] piecePlaced = new boolean[PIECES];
    private final double[] pieceOffset = new double[PIECES];
    private final double[] dropBefore = new double[BntTankBody.AXLES.length];
    private final double[] dropNow = new double[BntTankBody.AXLES.length];
    private double travelled;
    private double travelledBefore;
    private double mark;
    private double speed;
    private double targetSpeed;
    private double tension;
    private double heaveBefore;
    private double pitchBefore;
    private boolean floorAway;
    private int ticks;

    private static final class Obstacle {
        final ElementLink<WorldSectionElement> link;
        final double baseZ;
        final double depth;
        final double height;
        boolean placed;
        boolean gone;
        double at;

        Obstacle(ElementLink<WorldSectionElement> link, double baseZ, double depth, double height) {
            this.link = link;
            this.baseZ = baseZ;
            this.depth = depth;
            this.height = height;
        }
    }

    BntTankRig() {
        for (int x : new int[]{2, 8}) {
            for (int axle = 0; axle < BntTankBody.AXLES.length; axle++) {
                wheels.put(new BlockPos(x, 2, (int)Math.floor(BntTankBody.AXLES[axle])), axle);
            }
        }
    }

    void vehicle(ElementLink<WorldSectionElement> link) {
        vehicle = link;
    }

    void floor(ElementLink<WorldSectionElement> link) {
        floor = link;
    }

    void piece(int index, ElementLink<WorldSectionElement> link) {
        pieces.set(index, link);
    }

    int obstacle(ElementLink<WorldSectionElement> link, double baseZ, double depth, double height) {
        obstacles.add(new Obstacle(link, baseZ, depth, height));
        return obstacles.size() - 1;
    }

    Vec3 pivot() {
        return PIVOT;
    }

    PonderInstruction driver() {
        return new TickingInstruction(false, 100000) {
            @Override
            protected void firstTick(PonderScene scene) {
                super.firstTick(scene);
                begin(scene);
            }

            @Override
            public void tick(PonderScene scene) {
                super.tick(scene);
                step(scene);
            }
        };
    }

    void release() {
        body.release();
    }

    void drive(double beltSpeed) {
        targetSpeed = beltSpeed;
    }

    void tension(double value) {
        tension = value;
    }

    void mark() {
        mark = travelled;
    }

    void placePiece(PonderScene scene, int index) {
        piecePlaced[index] = true;
        pieceOffset[index] = pieceZ(index);
        WorldSectionElement element = scene.resolve(pieces.get(index));
        if (element != null) {
            element.setAnimatedOffset(new Vec3(0.0, 0.0, pieceOffset[index]), true);
        }
    }

    void sendFloorAway(PonderScene scene) {
        floorAway = true;
        WorldSectionElement element = scene.resolve(floor);
        if (element != null) {
            element.setAnimatedOffset(AWAY, true);
        }
    }

    void placeObstacle(PonderScene scene, int index, double frontAtMark) {
        Obstacle obstacle = obstacles.get(index);
        obstacle.placed = true;
        obstacle.gone = false;
        obstacle.at = frontAtMark - mark;
        WorldSectionElement element = scene.resolve(obstacle.link);
        if (element != null) {
            element.setAnimatedOffset(new Vec3(0.0, 0.0, obstacle.at + travelled - obstacle.baseZ), true);
        }
    }

    private void begin(PonderScene scene) {
        body.reset();
        travelled = 0.0;
        travelledBefore = 0.0;
        mark = 0.0;
        speed = 0.0;
        targetSpeed = 0.0;
        tension = 1.0;
        heaveBefore = 0.0;
        pitchBefore = 0.0;
        floorAway = false;
        ticks = 0;
        Arrays.fill(piecePlaced, false);
        Arrays.fill(dropBefore, 0.0);
        Arrays.fill(dropNow, 0.0);
        for (Obstacle obstacle : obstacles) {
            obstacle.placed = false;
            obstacle.gone = false;
        }
        BntPonderPhysics.setStage(scene.getWorld(), this);
    }

    private void step(PonderScene scene) {
        ticks++;
        travelledBefore = travelled;
        heaveBefore = body.heave();
        pitchBefore = body.pitch();
        System.arraycopy(dropNow, 0, dropBefore, 0, dropNow.length);

        double dt = TICK_SECONDS / SUBSTEPS;
        for (int sub = 0; sub < SUBSTEPS; sub++) {
            double before = speed;
            double change = Mth.clamp(targetSpeed - speed, -ACCELERATION * dt, ACCELERATION * dt);
            speed += change;
            travelled += (before + speed) * 0.5 * dt;
            refreshActive();
            body.step(dt, (speed - before) / dt, tension, this);
        }
        for (int axle = 0; axle < dropNow.length; axle++) {
            dropNow[axle] = body.shownDrop(axle);
        }

        if (body.released()) {
            WorldSectionElement hull = vehicle == null ? null : scene.resolve(vehicle);
            if (hull != null) {
                hull.setAnimatedOffset(new Vec3(0.0, body.heave(), 0.0), false);
                hull.setAnimatedRotation(new Vec3(Math.toDegrees(body.pitch()), 0.0, 0.0), false);
            }
        }

        for (int index = 0; index < pieces.size(); index++) {
            if (!piecePlaced[index]) {
                continue;
            }
            double offset = pieceZ(index);
            WorldSectionElement element = scene.resolve(pieces.get(index));
            if (element != null) {
                element.setAnimatedOffset(new Vec3(0.0, 0.0, offset), offset < pieceOffset[index]);
            }
            pieceOffset[index] = offset;
        }

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.placed || obstacle.gone) {
                continue;
            }
            WorldSectionElement element = scene.resolve(obstacle.link);
            double front = obstacle.at + travelled;
            if (front > TAKEN_AWAY_Z) {
                obstacle.gone = true;
                if (element != null) {
                    element.setAnimatedOffset(AWAY, true);
                }
                continue;
            }
            if (element != null) {
                element.setAnimatedOffset(new Vec3(0.0, 0.0, front - obstacle.baseZ), false);
            }
        }

        if (floorAway) {
            WorldSectionElement element = scene.resolve(floor);
            if (element != null) {
                element.setAnimatedOffset(AWAY, true);
            }
        }
    }

    private void refreshActive() {
        active.clear();
        for (Obstacle obstacle : obstacles) {
            if (obstacle.placed && !obstacle.gone) {
                active.add(obstacle);
            }
        }
    }

    private double pieceZ(int index) {
        double loop = pieces.size() * PIECE_LENGTH;
        return RUNWAY_START + Mth.positiveModulo(index * PIECE_LENGTH + travelled, loop);
    }

    private static float partial() {
        return PonderUI.getPartialTicks();
    }

    @Override
    public double wheelDrop(BlockPos pos) {
        Integer axle = wheels.get(pos);
        if (axle == null) {
            return Double.NaN;
        }
        return Mth.lerp(partial(), dropBefore[axle], dropNow[axle]);
    }

    @Override
    public Vec3 toWorld(Vec3 levelPos) {
        float pt = partial();
        double heave = Mth.lerp(pt, heaveBefore, body.heave());
        double pitch = Mth.lerp(pt, pitchBefore, body.pitch());
        double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        double dy = levelPos.y - PIVOT.y;
        double dz = levelPos.z - PIVOT.z;
        return new Vec3(levelPos.x, PIVOT.y + heave + dy * cos - dz * sin, PIVOT.z + dy * sin + dz * cos);
    }

    @Override
    public double groundAt(double x, double z) {
        if (x < RUNWAY_WEST || x >= RUNWAY_EAST) {
            return Double.NaN;
        }
        double top = BntTankBody.FLOOR;
        if (x < OBSTACLE_WEST || x >= OBSTACLE_EAST) {
            return top;
        }
        double runway = Mth.lerp(partial(), travelledBefore, travelled);
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.placed || obstacle.gone) {
                continue;
            }
            double front = obstacle.at + runway;
            if (z >= front && z < front + obstacle.depth) {
                top = Math.max(top, BntTankBody.FLOOR + obstacle.height);
            }
        }
        return top;
    }

    @Override
    public double epoch() {
        return ticks + partial();
    }

    @Override
    public int count() {
        return active.size();
    }

    @Override
    public double front(int box) {
        return active.get(box).at + travelled;
    }

    @Override
    public double back(int box) {
        Obstacle obstacle = active.get(box);
        return obstacle.at + travelled + obstacle.depth;
    }

    @Override
    public double top(int box) {
        return BntTankBody.FLOOR + active.get(box).height;
    }
}
