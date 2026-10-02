package dev.qwxon.bitsntracks.client.ponder;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.BntTuning;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BntSuspensionScenes {
    private static final float DRIVE_RPM = -32.0F;
    private static final float DRIVE_TURN = 105.0F;
    private static final float ZOOM_TILT = 25.0F;
    private static final float ZOOM_TURN = 20.0F;
    private static final double TRACK_ZOOM = 2.4;
    private static final double FIRST_ROW = -4.0;
    private static final int ROWS = 3;
    private static final int STEPS = 3;
    private static final double OUT_OF_PICTURE = 18.0;
    private static final double CONTACT = 3.0;
    private static final double CLEARED = 8.1;
    private static final double ARM_FACE = 7.03;
    private static final double BOGIE_FACE = 7.13;
    private static final Vec3 EAST_TRACK = new Vec3(6.5, 2.75, 5.0);
    private static final Vec3 WEST_TRACK = new Vec3(2.5, 2.75, 4.5);
    private static final float WEST_TURN = -55.0F;
    private static final float ACROSS_TURN = 180.0F;
    private static final int MODE_TICKS = 30;
    private static final int TUNE_TICKS = 8;

    private BntSuspensionScenes() {
    }

    public static void singleArm(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("suspension_piece_single_arm", "Adding suspension on physified cogwheels - SRDS System");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9F);

        PonderLevel world = builder.getScene().getWorld();
        ItemStack piece = BitsNTracksItems.SUSPENSION_PIECE.asStack();
        double rest = BntBobScenes.restingDrop();
        Vec3 resting = new Vec3(0.0, -rest, 0.0);
        double seat = CogwheelSizeHelper.getVisualVerticalOffset(BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get());
        BlockPos assembler = util.grid().at(4, 5, 7);
        BlockPos eastController = util.grid().at(6, 3, 2);
        CompoundTag eastBelt = beltOf(world, eastController);
        double speed = -DRIVE_RPM / 60.0 * 2.0 * Math.PI
            * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get()) / 20.0;

        BntBobRig rig = new BntBobRig(util, BntBobScenes.SEAT, rest);
        double firstEnd = rig.course(FIRST_ROW, ROWS, STEPS);
        double firstLap = BntBobRig.alignedDistance(OUT_OF_PICTURE - firstEnd);
        double secondEnd = rig.course(FIRST_ROW - firstLap, ROWS, STEPS) + firstLap;
        double secondLap = CLEARED - secondEnd;

        Selection whole = BntBobScenes.wholeOf(BntBobScenes.rowsOf(util, BntBobScenes.blocksOfBob(builder)));
        ElementLink<WorldSectionElement> plate = scene.world().showIndependentSectionImmediately(util.select().fromTo(0, 0, 0, 8, 0, 8));
        ElementLink<WorldSectionElement> bob = scene.world().showIndependentSectionImmediately(whole);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(BntBobScenes.SEAT, BntBobScenes.Perched::new);
        rig.bind(bob, plate, parrot);
        scene.world().modifyBlockEntity(assembler, BlockEntity.class, be -> BntBobScenes.flickLever(be, true));
        scene.addInstruction(rig.driver());
        scene.idle(20);

        BntBobScenes.caption(scene, "Suspension in this mod does not come for granted like other mods do.", 80);
        BntBobScenes.caption(scene, "In order to allow for different designs, you can " + ChatFormatting.YELLOW + "select"
            + ChatFormatting.RESET + " whether you want your tracks to have suspension.", 90);

        BntBobScenes.flipLever(scene, util, resting);
        BntBobScenes.power(scene, util, true);
        BntBobScenes.spin(scene, util, DRIVE_RPM, -1.0F);
        scene.addInstruction(BntPonderCamera.glideHome(0.0F, DRIVE_TURN, 40));
        scene.addInstruction(rig.unroll());
        int firstTicks = BntBobRig.ticksFor(firstLap, speed);
        scene.addInstruction(rig.trip(firstLap, firstTicks));
        int bump = BntBobRig.tickReaching(firstLap, firstTicks, CONTACT - (BntBobRig.rowFront(FIRST_ROW, 0) + 1.0));
        scene.idle(bump);
        scene.overlay().showText(120)
            .text(BntBobScenes.tint(ChatFormatting.GRAY, "By not having suspension, the whole vehicle moves as one entire body"));
        scene.addKeyframe();
        scene.idle(firstTicks - bump - 20);

        stop(scene, util, resting);
        scene.addInstruction(rig.lift(25));
        scene.idle(45);

        BntBobScenes.caption(scene, "So let's give Bob a smoother ride to go over the bumps with.", 80);

        BntFlangedCogwheelScenes.hideTread(scene, util.select().position(eastController));
        scene.addInstruction(BntPonderCamera.glide(EAST_TRACK, TRACK_ZOOM, ZOOM_TILT, ZOOM_TURN, 35));
        scene.idle(45);

        for (int z = 3; z <= 6; z++) {
            BlockPos wheel = util.grid().at(6, 2, z);
            Vec3 face = new Vec3(7.0, 2.5 + seat, z + 0.5);
            scene.overlay().showControls(face, Pointing.LEFT, 12).rightClick().withItem(piece);
            scene.idle(8);
            scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "suspension_" + z, new AABB(wheel), 22);
            scene.idle(10);
            scene.overlay().showControls(face.add(0.0, 0.25, -0.4), Pointing.LEFT, 12).rightClick().withItem(piece);
            scene.idle(8);
            scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> mount(be, 1, -1));
            scene.addInstruction(rig.suspend(wheel));
            scene.idle(14);
        }
        scene.idle(10);

        String marked = ChatFormatting.YELLOW.toString() + ChatFormatting.UNDERLINE;
        scene.overlay().showText(100)
            .text("The first type of suspension is a " + styled(marked, "Single-arm Rotary Damper Suspension"))
            .placeNearTarget()
            .pointAt(new Vec3(6.5, 3.0 + seat, 3.0));
        scene.addKeyframe();
        scene.idle(110);

        for (int z = 3; z <= 6; z++) {
            BlockPos wheel = util.grid().at(2, 2, z);
            scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> mount(be, 1, 1));
            scene.addInstruction(rig.suspend(wheel));
        }
        scene.addInstruction(rig.sweep(BntBobRig.sweepTicks() + 110, ARM_FACE));
        scene.idle(BntBobRig.sweepTicks() / 2);
        scene.overlay().showText(110)
            .text("It moves in an " + ChatFormatting.YELLOW + "arc" + ChatFormatting.RESET + ", its pivot should be placed "
                + styled(ChatFormatting.UNDERLINE.toString(), "towards the front of your vehicle"));
        scene.addKeyframe();
        scene.idle(120);

        scene.addInstruction(restoreBelt(eastController, eastBelt));
        scene.addInstruction(BntPonderCamera.glideHome(-ZOOM_TILT, -ZOOM_TURN, 35));
        scene.idle(45);
        scene.addInstruction(rig.release());
        scene.idle(60);

        BntBobScenes.flipLever(scene, util, resting);
        BntBobScenes.power(scene, util, true);
        BntBobScenes.spin(scene, util, DRIVE_RPM, -1.0F);
        int secondTicks = BntBobRig.ticksFor(secondLap, speed);
        scene.addInstruction(rig.trip(secondLap, secondTicks));
        int smooth = BntBobRig.tickReaching(secondLap, secondTicks, CONTACT - (BntBobRig.rowFront(FIRST_ROW, ROWS * 3 / 4) + 1.0));
        int midway = BntBobRig.tickReaching(secondLap, secondTicks, CONTACT - (BntBobRig.stepFront(FIRST_ROW, ROWS, STEPS / 2) + 2.0));
        scene.idle(smooth);
        scene.special().changeBirbPose(parrot, BntBobScenes.Cheering::new);
        scene.overlay().showText(midway - smooth)
            .text(BntBobScenes.tint(ChatFormatting.GRAY, "As you can see, with the suspension the ride is much more pleasant for Bob"));
        scene.addKeyframe();
        scene.idle(midway - smooth);
        scene.special().changeBirbPose(parrot, BntBobScenes.Perched::new);
        scene.idle(secondTicks - midway - 20);

        stop(scene, util, resting);
        scene.idle(10);
        scene.markAsFinished();
    }

    public static void bogie(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("suspension_piece_bogie", "Adding suspension on physified cogwheels - VDSS system");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9F);

        PonderLevel world = builder.getScene().getWorld();
        ItemStack piece = BitsNTracksItems.SUSPENSION_PIECE.asStack();
        double rest = BntBobScenes.restingDrop();
        Vec3 resting = new Vec3(0.0, -rest, 0.0);
        double seat = CogwheelSizeHelper.getVisualVerticalOffset(BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get());
        BlockPos assembler = util.grid().at(4, 5, 7);
        BlockPos eastController = util.grid().at(6, 3, 2);
        CompoundTag eastBelt = beltOf(world, eastController);
        double speed = -DRIVE_RPM / 60.0 * 2.0 * Math.PI
            * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get()) / 20.0;

        BntBobRig rig = new BntBobRig(util, BntBobScenes.SEAT, rest);
        double lap = CLEARED - rig.course(FIRST_ROW, ROWS, STEPS);

        Selection whole = BntBobScenes.wholeOf(BntBobScenes.rowsOf(util, BntBobScenes.blocksOfBob(builder)));
        ElementLink<WorldSectionElement> plate = scene.world().showIndependentSectionImmediately(util.select().fromTo(0, 0, 0, 8, 0, 8));
        ElementLink<WorldSectionElement> bob = scene.world().showIndependentSectionImmediately(whole);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(BntBobScenes.SEAT, BntBobScenes.Perched::new);
        rig.bind(bob, plate, parrot);
        scene.world().modifyBlockEntity(assembler, BlockEntity.class, be -> BntBobScenes.flickLever(be, true));
        scene.addInstruction(rig.driver());
        scene.addInstruction(BntPonderCamera.glideHome(0.0F, DRIVE_TURN, 0));
        for (int z = 3; z <= 6; z++) {
            for (int x : new int[]{2, 6}) {
                BlockPos wheel = util.grid().at(x, 2, z);
                int facing = x == 6 ? -1 : 1;
                scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> mount(be, 1, facing));
                scene.addInstruction(rig.suspend(wheel));
            }
        }
        scene.addInstruction(rig.settle());
        scene.idle(20);

        BntBobScenes.caption(scene, "If the SRDS is not quite your type of movement, there are other kinds of suspension systems you can use in Bits 'n' Tracks.", 100);

        scene.addInstruction(rig.lift(25));
        BntFlangedCogwheelScenes.hideTread(scene, util.select().position(eastController));
        scene.addInstruction(BntPonderCamera.glide(EAST_TRACK, TRACK_ZOOM, ZOOM_TILT, ZOOM_TURN, 35));
        scene.idle(45);

        BntBobScenes.caption(scene, "To remove a suspension piece, you can do so by shift-right-clicking with an empty hand on the cogwheel it is placed on.", 90);

        for (int z = 3; z <= 6; z++) {
            BlockPos wheel = util.grid().at(6, 2, z);
            scene.overlay().showControls(face(z, seat), Pointing.LEFT, 8).rightClick().whileSneaking();
            scene.idle(6);
            scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> mount(be, 0, 0));
            scene.addInstruction(rig.unsuspend(wheel));
            scene.idle(6);
        }
        for (int z = 3; z <= 6; z++) {
            BlockPos wheel = util.grid().at(2, 2, z);
            scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> mount(be, 0, 0));
            scene.addInstruction(rig.unsuspend(wheel));
        }
        scene.idle(20);

        BntBobScenes.caption(scene, "The second suspension system involves mounting 2 cogwheels together to form a "
            + ChatFormatting.YELLOW + "bogie" + ChatFormatting.RESET + ".", 90);

        for (int lead = 4; lead <= 6; lead += 2) {
            BlockPos first = util.grid().at(6, 2, lead - 1);
            BlockPos second = util.grid().at(6, 2, lead);
            scene.overlay().showControls(face(lead - 1, seat), Pointing.LEFT, 12).rightClick().withItem(piece);
            scene.idle(8);
            scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "bogie_" + lead, new AABB(first), 24);
            scene.idle(10);
            scene.overlay().showControls(face(lead, seat), Pointing.LEFT, 12).rightClick().withItem(piece);
            scene.idle(8);
            scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "bogie_" + lead, new AABB(first).minmax(new AABB(second)), 12);
            pair(scene, rig, second, first, -1);
            scene.idle(16);
        }
        for (int lead = 4; lead <= 6; lead += 2) {
            pair(scene, rig, util.grid().at(2, 2, lead), util.grid().at(2, 2, lead - 1), 1);
        }
        scene.idle(10);

        String marked = ChatFormatting.YELLOW.toString() + ChatFormatting.UNDERLINE;
        scene.overlay().showText(90)
            .text("This is the " + styled(marked, "Vertical Dual Spring Suspension"))
            .placeNearTarget()
            .pointAt(new Vec3(7.2, 3.4 + seat, 4.0));
        scene.addKeyframe();
        scene.idle(100);

        scene.addInstruction(rig.sweep(BntBobRig.sweepTicks() + 130, BOGIE_FACE));
        scene.idle(BntBobRig.sweepTicks() / 2);
        scene.overlay().showText(130)
            .text("Differently than the SRDS, the VDSS is stiffer, and offers a similar motion to a " + ChatFormatting.YELLOW + "linear"
                + ChatFormatting.RESET + " than a " + ChatFormatting.YELLOW + "rotary" + ChatFormatting.RESET
                + " system, with less pushback and shorter travel.");
        scene.addKeyframe();
        scene.idle(140);

        scene.addInstruction(restoreBelt(eastController, eastBelt));
        scene.addInstruction(BntPonderCamera.glideHome(-ZOOM_TILT, -ZOOM_TURN, 35));
        scene.idle(45);
        scene.addInstruction(rig.release());
        scene.idle(60);

        BntBobScenes.flipLever(scene, util, resting);
        BntBobScenes.power(scene, util, true);
        BntBobScenes.spin(scene, util, DRIVE_RPM, -1.0F);
        scene.addInstruction(rig.unroll());
        int ticks = BntBobRig.ticksFor(lap, speed);
        scene.addInstruction(rig.trip(lap, ticks));
        double start = CONTACT - (BntBobRig.rowFront(FIRST_ROW, 0) + 1.0);
        int halfway = BntBobRig.tickReaching(lap, ticks, (start + lap) / 2.0);
        scene.idle(halfway);
        scene.overlay().showText(ticks - halfway - 20)
            .text("You might want to consider this over the SRDS when building heavy tracked vehicles with suspension, as the bogie'd setup "
                + "allows for the load to be split evenly across 2 cogwheels instead of individually");
        scene.addKeyframe();
        scene.idle(ticks - halfway - 20);

        stop(scene, util, resting);
        scene.idle(10);
        scene.markAsFinished();
    }

    public static void configuring(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("suspension_tool_configuring", "Configuring suspension with the Suspension Tool");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9F);

        ItemStack tool = BitsNTracksItems.SUSPENSION_TOOL.asStack();
        double rest = BntBobScenes.restingDrop();
        Vec3 resting = new Vec3(0.0, -rest, 0.0);
        double seat = CogwheelSizeHelper.getVisualVerticalOffset(BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get());
        BlockPos assembler = util.grid().at(4, 5, 7);
        BlockPos first = util.grid().at(2, 2, 3);
        List<BlockPos> bogie = List.of(util.grid().at(6, 2, 3), util.grid().at(6, 2, 4));
        Vec3 westTop = new Vec3(2.0, 3.0 + seat, 3.5).add(resting);
        Vec3 eastTop = new Vec3(7.0, 3.0 + seat, 4.0).add(resting);

        BntBobRig rig = new BntBobRig(util, BntBobScenes.SEAT, rest);
        BntToolDial dial = new BntToolDial();
        Selection whole = BntBobScenes.wholeOf(BntBobScenes.rowsOf(util, BntBobScenes.blocksOfBob(builder)));
        ElementLink<WorldSectionElement> plate = scene.world().showIndependentSectionImmediately(util.select().fromTo(0, 0, 0, 8, 0, 8));
        ElementLink<WorldSectionElement> bob = scene.world().showIndependentSectionImmediately(whole);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(BntBobScenes.SEAT, BntBobScenes.Perched::new);
        rig.bind(bob, plate, parrot);
        scene.world().modifyBlockEntity(assembler, BlockEntity.class, be -> BntBobScenes.flickLever(be, true));
        scene.addInstruction(rig.driver());
        for (int z = 3; z <= 6; z++) {
            BlockPos wheel = util.grid().at(2, 2, z);
            scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> mount(be, 1, 1));
            scene.addInstruction(rig.suspend(wheel));
        }
        for (int lead = 4; lead <= 6; lead += 2) {
            pair(scene, rig, util.grid().at(6, 2, lead), util.grid().at(6, 2, lead - 1), -1);
        }
        scene.addInstruction(rig.settle());
        scene.addInstruction(dial.place());
        scene.idle(20);

        BntBobScenes.caption(scene, "Any mod that has suspension of course has to add a way to tune it based on your needs, "
            + "and this is what the Suspension Tool is useful for", 110);

        scene.addInstruction(BntPonderCamera.glide(WEST_TRACK.add(resting), TRACK_ZOOM, ZOOM_TILT, WEST_TURN, 35));
        scene.idle(45);
        BntBobScenes.caption(scene, "The Suspension Tool has " + BntBobScenes.tint(ChatFormatting.YELLOW, "4 different modes") + ChatFormatting.RESET
            + " and " + BntBobScenes.tint(ChatFormatting.GOLD, "different groupings") + ChatFormatting.RESET + ".", 100);

        scene.addInstruction(dial.raise(true));
        scene.idle(45);
        scene.addInstruction(rig.outline(new AABB(first)));
        scene.addInstruction(dial.look(first));
        scene.idle(40);

        BntBobScenes.caption(scene, "Visual aid on the tool's gauge will tell you the magnitude shift for each setting.", 90);

        for (int i = 1; i <= BntTuning.values().length; i++) {
            scene.overlay().showControls(westTop, Pointing.DOWN, 15).leftClick().whileSneaking().withItem(tool);
            scene.idle(6);
            scene.addInstruction(dial.mode(BntTuning.byIndex(i)));
            scene.idle(MODE_TICKS);
        }
        scene.idle(10);

        scene.overlay().showText(120).text("By right-clicking you increase, shift-right-click to decrease the value.");
        scene.addKeyframe();
        scene.idle(30);
        tuneThrough(scene, dial, List.of(first), westTop, tool);

        scene.addInstruction(rig.outline(null));
        scene.addInstruction(dial.look(null));
        scene.addInstruction(BntPonderCamera.glide(EAST_TRACK.add(resting), TRACK_ZOOM, 0.0F, ACROSS_TURN, 50));
        scene.idle(60);
        scene.addInstruction(rig.outline(new AABB(bogie.get(0)).minmax(new AABB(bogie.get(1)))));
        scene.addInstruction(dial.look(bogie.get(0)));
        scene.idle(30);
        scene.overlay().showText(120).text("Changing the values of a VDSS, changes the individual values of BOTH cogwheels.");
        scene.addKeyframe();
        scene.idle(30);
        tuneThrough(scene, dial, bogie, eastTop, tool);

        scene.addInstruction(rig.outline(null));
        scene.addInstruction(dial.look(null));
        scene.idle(20);
        scene.addInstruction(dial.raise(false));
        scene.addInstruction(BntPonderCamera.glideHome(-ZOOM_TILT, -(WEST_TURN + ACROSS_TURN), 40));
        scene.idle(50);

        BntBobScenes.caption(scene, "Suspension values are saved on the cogwheel, even after a suspension piece has been removed", 100);
        scene.markAsFinished();
    }

    private static void tuneThrough(CreateSceneBuilder scene, BntToolDial dial, List<BlockPos> cogs, Vec3 at, ItemStack tool) {
        tune(scene, dial, cogs, at, tool, BntTuning.DEFAULT, BntTuning.MAX);
        scene.idle(20);
        tune(scene, dial, cogs, at, tool, BntTuning.MAX, BntTuning.MIN);
        scene.idle(20);
        tune(scene, dial, cogs, at, tool, BntTuning.MIN, BntTuning.DEFAULT);
        scene.idle(30);
    }

    private static void tune(CreateSceneBuilder scene, BntToolDial dial, List<BlockPos> cogs, Vec3 at, ItemStack tool, int from, int to) {
        int step = Integer.signum(to - from);
        InputElementBuilder controls = scene.overlay().showControls(at, Pointing.DOWN, Math.abs(to - from) * TUNE_TICKS + 10)
            .rightClick().withItem(tool);
        if (step < 0) {
            controls.whileSneaking();
        }
        scene.idle(6);
        for (int value = from + step; value != to + step; value += step) {
            scene.addInstruction(dial.tune(cogs, value));
            scene.idle(TUNE_TICKS);
        }
    }

    private static void stop(CreateSceneBuilder scene, SceneBuildingUtil util, Vec3 resting) {
        BntBobScenes.flipLever(scene, util, resting);
        BntBobScenes.power(scene, util, false);
        BntBobScenes.spin(scene, util, 0.0F, -1.0F);
        scene.idle(10);
    }

    private static void mount(KineticBlockEntity be, int side, int facing) {
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setSuspension(side, facing);
        }
    }

    private static void pair(CreateSceneBuilder scene, BntBobRig rig, BlockPos lead, BlockPos partner, int facing) {
        scene.world().modifyBlockEntity(lead, KineticBlockEntity.class, be -> mount(be, BntSuspension.BOGIE, facing));
        scene.world().modifyBlockEntity(partner, KineticBlockEntity.class, be -> mount(be, -BntSuspension.BOGIE, facing));
        scene.addInstruction(rig.suspend(lead));
        scene.addInstruction(rig.suspend(partner));
    }

    private static Vec3 face(int z, double seat) {
        return new Vec3(7.0, 2.5 + seat, z + 0.5);
    }

    private static String styled(String codes, String text) {
        return codes + text.replace(" ", " " + codes);
    }

    private static CompoundTag beltOf(PonderLevel world, BlockPos controller) {
        CompoundTag belt = new CompoundTag();
        BlockEntity be = world.getBlockEntity(controller);
        if (be != null) {
            CompoundTag saved = be.saveWithFullMetadata(world.registryAccess());
            belt.put("Chain", saved.getCompound("Chain"));
            belt.putInt("ChainsToRefund", saved.getInt("ChainsToRefund"));
        }
        return belt;
    }

    private static PonderInstruction restoreBelt(BlockPos controller, CompoundTag belt) {
        return PonderInstruction.simple(scene -> {
            PonderLevel level = scene.getWorld();
            BlockEntity be = level.getBlockEntity(controller);
            if (be != null) {
                CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
                tag.merge(belt.copy());
                be.loadWithComponents(tag, level.registryAccess());
            }
        });
    }
}
