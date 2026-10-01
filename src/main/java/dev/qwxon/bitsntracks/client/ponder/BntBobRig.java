package dev.qwxon.bitsntracks.client.ponder;

import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.physics.BntBeltHold;
import dev.qwxon.bitsntracks.physics.BntPonderPhysics;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

final class BntBobRig implements BntPonderPhysics.Stage, BntBobBody.Ground {
    static final int TILE = 8;
    static final double ROAD_FROM = -24.0;
    static final int ACCEL_TICKS = 20;

    private static final int TILES = 8;
    private static final double ROW_PERIOD = 4.0;
    private static final double CLEAR = 7.0;
    private static final double STEP_LENGTH = 2.0;
    private static final double STEP_PERIOD = 6.0;
    private static final double LOOP = TILE * TILES;
    private static final double ROAD_WEST = 0.0;
    private static final double ROAD_EAST = 9.0;
    private static final double SLAB = 0.5;
    private static final int SUBSTEPS = 2;
    private static final double TICK_SECONDS = 0.05;
    private static final int TILE_FADE = 10;
    private static final int TILE_STAGGER = 2;
    private static final Vec3 LIFT = new Vec3(0.0, -0.5, 0.0);
    private static final int ARC_COLOUR = 0xFFD94A;
    private static final float ARC_WIDTH = 1.0F / 32.0F;
    private static final int SETTLE_STEPS = 400;

    private enum Kind {
        ROW, STEP
    }

    private record Obstacle(Kind kind, double road) {
    }

    private record Trip(int start, double from, double distance, int ticks) {
        double at(int tick) {
            return from + travelled(distance, ticks, Mth.clamp(tick - start, 0, ticks));
        }
    }

    private final BntBobBody body;
    private final Map<BlockPos, BntBobBody.Wheel> wheels = new HashMap<>();
    private final List<BlockPos> eastWheels = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Selection tileSelection;
    private final Selection rowSelection;
    private final Selection stepSelection;
    private final Vec3 seat;
    private final double rest;
    private ElementLink<WorldSectionElement> bob;
    private ElementLink<WorldSectionElement> plate;
    private ElementLink<ParrotElement> parrot;

    private final List<WorldSectionElementImpl> tiles = new ArrayList<>();
    private final List<WorldSectionElementImpl> pieces = new ArrayList<>();
    private final int[] tileAge = new int[TILES];
    private final int[] tileState = new int[TILES];
    private final int[] tileDelay = new int[TILES];
    private final double[] tileAt = new double[TILES];
    private final List<List<Vec3>> arcs = new ArrayList<>();
    private double travelled;
    private double travelledBefore;
    private double heaveBefore;
    private double pitchBefore;
    private double rollBefore;
    private Trip trip;
    private int tick;
    private boolean roadOut;
    private boolean ticking;
    private int unrolledAt;
    private int[] pieceAge = new int[0];
    private int[] pieceDelay = new int[0];
    private boolean[] pieceShown = new boolean[0];
    private int holdStart;
    private int holdTicks;
    private double[] holdFrom;
    private int sweepStart = -1;
    private int arcUntil = -1;
    private double arcFace;

    BntBobRig(SceneBuildingUtil util, Vec3 seat, double rest) {
        this.seat = seat;
        this.rest = rest;
        tileSelection = util.select().fromTo(0, 0, 0, 8, 0, TILE - 1);
        rowSelection = util.select().fromTo(0, 1, 0, 8, 1, 0);
        stepSelection = util.select().position(6, 1, 2).add(util.select().position(2, 1, 3));
        body = new BntBobBody(new Vec3(4.5, 3.0, 5.0));

        Block hidden = BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get();
        double wheelRadius = CogwheelSizeHelper.getTrackRadius(hidden);
        double sprocketRadius = CogwheelSizeHelper.getTrackRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get());
        double wheelY = 2.5 + CogwheelSizeHelper.getVisualVerticalOffset(hidden);
        for (int x : new int[]{2, 6}) {
            List<BntBobBody.Wheel> track = new ArrayList<>();
            track.add(new BntBobBody.Wheel(null, new Vec3(x + 0.5, 3.5, 2.5), sprocketRadius));
            for (int z = 3; z <= 6; z++) {
                BlockPos pos = new BlockPos(x, 2, z);
                BntBobBody.Wheel wheel = new BntBobBody.Wheel(pos, new Vec3(x + 0.5, wheelY, z + 0.5), wheelRadius);
                wheels.put(pos, wheel);
                track.add(wheel);
                if (x == 6) {
                    eastWheels.add(pos);
                }
            }
            track.add(new BntBobBody.Wheel(null, new Vec3(x + 0.5, 3.5, 7.5), sprocketRadius));
            body.track(track);
        }
    }

    void bind(ElementLink<WorldSectionElement> bob, ElementLink<WorldSectionElement> plate, ElementLink<ParrotElement> parrot) {
        this.bob = bob;
        this.plate = plate;
        this.parrot = parrot;
    }

    static double travelled(double distance, int ticks, int tick) {
        int accel = Math.min(ACCEL_TICKS, ticks / 2);
        if (ticks <= 0 || accel <= 0) {
            return distance;
        }
        double speed = distance / (ticks - accel);
        if (tick <= accel) {
            return 0.5 * speed * tick * tick / accel;
        }
        if (tick >= ticks - accel) {
            double left = ticks - tick;
            return distance - 0.5 * speed * left * left / accel;
        }
        return speed * (tick - accel * 0.5);
    }

    static int ticksFor(double distance, double speed) {
        return (int)Math.ceil(distance / speed) + ACCEL_TICKS;
    }

    static int tickReaching(double distance, int ticks, double reach) {
        for (int t = 0; t <= ticks; t++) {
            if (travelled(distance, ticks, t) >= reach) {
                return t;
            }
        }
        return ticks;
    }

    static double alignedDistance(double atLeast) {
        return Math.ceil(atLeast / TILE) * TILE;
    }

    double course(double firstRow, int rows, int steps) {
        for (int i = 0; i < rows; i++) {
            obstacles.add(new Obstacle(Kind.ROW, rowFront(firstRow, i)));
        }
        for (int j = 0; j < steps; j++) {
            obstacles.add(new Obstacle(Kind.STEP, stepFront(firstRow, rows, j) - 2.0));
        }
        return stepFront(firstRow, rows, steps - 1);
    }

    static double rowFront(double firstRow, int row) {
        return firstRow - ROW_PERIOD * row;
    }

    static double stepFront(double firstRow, int rows, int step) {
        return rowFront(firstRow, rows - 1) - CLEAR - STEP_LENGTH - STEP_PERIOD * step;
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

    PonderInstruction trip(double distance, int ticks) {
        return PonderInstruction.simple(scene -> trip = new Trip(tick, travelled, distance, ticks));
    }

    PonderInstruction unroll() {
        return PonderInstruction.simple(scene -> {
            roadOut = true;
            unrolledAt = tick;
            for (int k = 0; k < TILES; k++) {
                double at = tileAt[k];
                tileState[k] = 1;
                tileAge[k] = 0;
                tileDelay[k] = at == 0.0 || at == TILE ? 0 : (int)(Math.abs(at) / TILE) * TILE_STAGGER;
                if (tileDelay[k] == 0) {
                    WorldSectionElementImpl tile = tiles.get(k);
                    tile.setVisible(true);
                    tile.forceApplyFade(1.0F);
                    tileState[k] = 2;
                }
            }
            WorldSectionElement base = scene.resolve(plate);
            if (base != null) {
                base.setVisible(false);
            }
        });
    }

    PonderInstruction lift(int ticks) {
        return PonderInstruction.simple(scene -> {
            body.hold();
            holdStart = tick;
            holdTicks = ticks;
            holdFrom = new double[]{body.heave(), body.pitch(), body.roll()};
        });
    }

    PonderInstruction release() {
        return PonderInstruction.simple(scene -> {
            holdFrom = null;
            body.release();
        });
    }

    PonderInstruction suspend(BlockPos pos) {
        return PonderInstruction.simple(scene -> {
            BntBobBody.Wheel wheel = wheels.get(pos);
            BlockEntity be = scene.getWorld().getBlockEntity(pos);
            BntSuspension.Arm arm = be == null ? null : BntSuspension.arm(be);
            if (wheel == null || arm == null) {
                return;
            }
            wheel.arm = rise -> BntSuspension.displacement(be, -rise);
            wheel.upCap = arm.upCap();
            wheel.downCap = arm.downCap();
            wheel.ride = arm.ride();
        });
    }

    PonderInstruction unsuspend(BlockPos pos) {
        return PonderInstruction.simple(scene -> {
            BntBobBody.Wheel wheel = wheels.get(pos);
            if (wheel != null) {
                wheel.arm = null;
                wheel.ride = 0.0;
            }
        });
    }

    PonderInstruction settle() {
        return PonderInstruction.simple(scene -> {
            ticking = true;
            readHolds(scene.getWorld());
            body.settle(TICK_SECONDS / SUBSTEPS, this, SETTLE_STEPS);
            heaveBefore = body.heave();
            pitchBefore = body.pitch();
            rollBefore = body.roll();
            pose(scene, true);
            ticking = false;
        });
    }

    PonderInstruction sweep(int keep, double face) {
        return PonderInstruction.simple(scene -> {
            sweepStart = tick;
            arcUntil = tick + keep;
            arcFace = face;
            arcs.clear();
            for (int i = 0; i < eastWheels.size(); i++) {
                arcs.add(new ArrayList<>());
            }
        });
    }

    static int sweepTicks() {
        return SWEEP_DROP + SWEEP_TRACE + SWEEP_BACK;
    }

    private static final int SWEEP_DROP = 12;
    private static final int SWEEP_TRACE = 40;
    private static final int SWEEP_BACK = 16;

    private void begin(PonderScene scene) {
        BntPonderPhysics.setStage(scene.getWorld(), this);
        tick = 0;
        trip = null;
        travelled = 0.0;
        travelledBefore = 0.0;
        holdFrom = null;
        sweepStart = -1;
        arcUntil = -1;
        arcs.clear();
        for (BntBobBody.Wheel wheel : wheels.values()) {
            wheel.arm = null;
            wheel.ride = 0.0;
            wheel.rise = 0.0;
            wheel.shown = 0.0;
            wheel.shownBefore = 0.0;
        }
        body.place(-rest, 0.0, 0.0);
        body.release();
        heaveBefore = body.heave();
        pitchBefore = 0.0;
        rollBefore = 0.0;

        tiles.clear();
        for (int k = 0; k < TILES; k++) {
            WorldSectionElementImpl tile = new WorldSectionElementImpl(tileSelection);
            scene.addElement(tile);
            tile.setVisible(false);
            tile.forceApplyFade(1.0F);
            tiles.add(tile);
            tileState[k] = 0;
            tileAt[k] = tilePosition(k, 0.0);
            tile.setAnimatedOffset(new Vec3(0.0, 0.0, tileAt[k]), true);
        }
        pieces.clear();
        roadOut = false;
        pieceAge = new int[obstacles.size()];
        pieceDelay = new int[obstacles.size()];
        pieceShown = new boolean[obstacles.size()];
        for (Obstacle obstacle : obstacles) {
            WorldSectionElementImpl piece = new WorldSectionElementImpl(obstacle.kind() == Kind.ROW ? rowSelection : stepSelection);
            scene.addElement(piece);
            piece.setVisible(false);
            piece.forceApplyFade(1.0F);
            pieces.add(piece);
        }

        WorldSectionElement hull = scene.resolve(bob);
        if (hull != null) {
            hull.setCenterOfRotation(body.pivot());
        }
        pose(scene, true);
    }

    private void readHolds(PonderLevel level) {
        for (BntBobBody.Wheel wheel : wheels.values()) {
            wheel.hold = level.getBlockEntity(wheel.pos) instanceof KineticBlockEntity kinetic ? BntBeltHold.at(level, kinetic) : 0.0;
        }
    }

    private void pose(PonderScene scene, boolean jump) {
        WorldSectionElement hull = scene.resolve(bob);
        if (hull != null) {
            hull.setAnimatedOffset(new Vec3(0.0, body.heave(), 0.0), jump);
            hull.setAnimatedRotation(new Vec3(Math.toDegrees(body.pitch()), 0.0, Math.toDegrees(body.roll())), jump);
        }
        ParrotElement bird = scene.resolve(parrot);
        if (bird != null) {
            bird.setPositionOffset(body.world(seat).subtract(seat), jump);
        }
    }

    private void step(PonderScene scene) {
        tick++;
        ticking = true;
        readHolds(scene.getWorld());
        travelledBefore = travelled;
        heaveBefore = body.heave();
        pitchBefore = body.pitch();
        rollBefore = body.roll();

        double target = trip == null ? travelled : trip.at(tick);
        double from = travelled;
        double dt = TICK_SECONDS / SUBSTEPS;
        for (int sub = 1; sub <= SUBSTEPS; sub++) {
            travelled = Mth.lerp(sub / (double)SUBSTEPS, from, target);
            body.step(dt, this);
        }
        travelled = target;

        if (holdFrom != null) {
            double t = holdTicks <= 0 ? 1.0 : Mth.clamp((tick - holdStart) / (double)holdTicks, 0.0, 1.0);
            double eased = t * t * (3.0 - 2.0 * t);
            body.place(holdFrom[0] * (1.0 - eased), holdFrom[1] * (1.0 - eased), holdFrom[2] * (1.0 - eased));
        }
        body.showWheels(TICK_SECONDS, !body.free());
        if (sweepStart >= 0) {
            sweep();
        }
        drawArcs(scene);
        pose(scene, false);
        moveRoad();
        moveObstacles();
        ticking = false;
    }

    private void sweep() {
        int age = tick - sweepStart;
        for (BlockPos pos : eastWheels) {
            BntBobBody.Wheel wheel = wheels.get(pos);
            BntBobBody.Wheel twin = wheels.get(new BlockPos(2, pos.getY(), pos.getZ()));
            if (wheel.arm == null) {
                continue;
            }
            double drop;
            if (age <= SWEEP_DROP) {
                drop = Mth.lerp(ease(age / (double)SWEEP_DROP), 0.0, wheel.downCap);
            } else if (age <= SWEEP_DROP + SWEEP_TRACE) {
                drop = Mth.lerp(ease((age - SWEEP_DROP) / (double)SWEEP_TRACE), wheel.downCap, -wheel.upCap);
            } else {
                drop = Mth.lerp(ease(Math.min(1.0, (age - SWEEP_DROP - SWEEP_TRACE) / (double)SWEEP_BACK)), -wheel.upCap, 0.0);
            }
            for (BntBobBody.Wheel each : new BntBobBody.Wheel[]{wheel, twin}) {
                if (each != null && each.arm != null) {
                    each.shown = drop;
                }
            }
            if (age >= SWEEP_DROP && age <= SWEEP_DROP + SWEEP_TRACE) {
                Vec3 centre = body.world(wheel.at(-drop));
                arcs.get(eastWheels.indexOf(pos)).add(new Vec3(arcFace, centre.y, centre.z));
            }
        }
        if (age > sweepTicks()) {
            sweepStart = -1;
        }
    }

    private void drawArcs(PonderScene scene) {
        if (tick > arcUntil) {
            return;
        }
        for (int w = 0; w < arcs.size(); w++) {
            List<Vec3> arc = arcs.get(w);
            for (int i = 0; i + 1 < arc.size(); i++) {
                scene.getOutliner().showLine(List.of(this, w, i), arc.get(i), arc.get(i + 1))
                    .colored(ARC_COLOUR).lineWidth(ARC_WIDTH).disableLineNormals();
            }
        }
    }

    private void moveRoad() {
        for (int k = 0; k < TILES; k++) {
            WorldSectionElementImpl tile = tiles.get(k);
            double at = tilePosition(k, travelled);
            boolean wrapped = at < tileAt[k] - TILE;
            tileAt[k] = at;
            tile.setAnimatedOffset(new Vec3(0.0, 0.0, at), wrapped);
            fadeTile(k, tile);
        }
    }

    private void fadeTile(int k, WorldSectionElementImpl tile) {
        int state = tileState[k];
        if (state != 1) {
            return;
        }
        if (tileDelay[k] > 0) {
            tileDelay[k]--;
            return;
        }
        int age = ++tileAge[k];
        float left = Math.max(0.0F, 1.0F - age / (float)TILE_FADE);
        if (age == 1) {
            tile.setVisible(true);
            tile.forceApplyFade(0.0F);
            tile.setFadeVec(LIFT);
        }
        tile.setFade(1.0F - left * left);
        if (left <= 0.0F) {
            tile.setFade(1.0F);
            tileState[k] = 2;
        }
    }

    private void moveObstacles() {
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            WorldSectionElementImpl piece = pieces.get(i);
            double shift = obstacle.road() + travelled;
            boolean inside = roadOut && shift + 4.0 > ROAD_FROM && shift < ROAD_FROM + LOOP;
            if (inside && !pieceShown[i]) {
                pieceShown[i] = true;
                pieceAge[i] = 0;
                pieceDelay[i] = tick <= unrolledAt + 1 ? (int)(Math.abs(shift) / TILE) * TILE_STAGGER + TILE_FADE / 2 : 0;
                piece.setAnimatedOffset(new Vec3(0.0, 0.0, shift), true);
            } else if (!inside && pieceShown[i]) {
                pieceShown[i] = false;
                piece.setVisible(false);
            }
            if (!pieceShown[i]) {
                continue;
            }
            piece.setAnimatedOffset(new Vec3(0.0, 0.0, shift), false);
            if (pieceDelay[i] > 0) {
                pieceDelay[i]--;
                continue;
            }
            int age = ++pieceAge[i];
            if (age == 1) {
                piece.setVisible(true);
                piece.forceApplyFade(0.0F);
                piece.setFadeVec(LIFT);
            }
            if (age <= TILE_FADE) {
                float left = 1.0F - age / (float)TILE_FADE;
                piece.setFade(1.0F - left * left);
            }
        }
    }

    private static double tilePosition(int k, double travelled) {
        return ROAD_FROM + Mth.positiveModulo(k * TILE - ROAD_FROM + travelled, LOOP);
    }

    private static double ease(double t) {
        double clamped = Mth.clamp(t, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    @Override
    public List<BntBobBody.Box> under(double x) {
        return boxes(x, travelled);
    }

    private List<BntBobBody.Box> boxes(double x, double road) {
        List<BntBobBody.Box> boxes = new ArrayList<>();
        double top = BntBobBody.FLOOR + SLAB;
        for (Obstacle obstacle : obstacles) {
            double shift = obstacle.road() + road;
            if (obstacle.kind() == Kind.ROW) {
                if (x >= ROAD_WEST && x < ROAD_EAST) {
                    boxes.add(new BntBobBody.Box(shift, shift + 1.0, top));
                }
            } else if (x >= 6.0 && x < 7.0) {
                boxes.add(new BntBobBody.Box(shift + 2.0, shift + 3.0, top));
            } else if (x >= 2.0 && x < 3.0) {
                boxes.add(new BntBobBody.Box(shift + 3.0, shift + 4.0, top));
            }
        }
        return boxes;
    }

    private float partial() {
        return ticking ? 1.0F : PonderUI.getPartialTicks();
    }

    @Override
    public double wheelDrop(BlockPos pos) {
        BntBobBody.Wheel wheel = wheels.get(pos);
        return wheel == null ? Double.NaN : Mth.lerp(partial(), wheel.shownBefore, wheel.shown);
    }

    @Override
    public Vec3 toWorld(Vec3 levelPos) {
        float pt = partial();
        return BntBobBody.transform(levelPos, body.pivot(), Mth.lerp(pt, heaveBefore, body.heave()),
            Mth.lerp(pt, pitchBefore, body.pitch()), Mth.lerp(pt, rollBefore, body.roll()));
    }

    @Override
    public double groundAt(double x, double z) {
        if (x < ROAD_WEST || x >= ROAD_EAST) {
            return Double.NaN;
        }
        double top = BntBobBody.FLOOR;
        for (BntBobBody.Box box : boxes(x, Mth.lerp(partial(), travelledBefore, travelled))) {
            if (z >= box.front() && z < box.back()) {
                top = Math.max(top, box.top());
            }
        }
        return top;
    }

    @Override
    public double epoch() {
        return tick + partial();
    }

    @Override
    public long tick() {
        return tick;
    }
}
