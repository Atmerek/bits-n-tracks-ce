package dev.qwxon.bitsntracks.client.ponder;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainShape;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainWholeShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntChainShapeContext;
import dev.qwxon.bitsntracks.client.BntClientOutliner;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BntBobScenes {
    private static final float DEFAULT_YAW = 145.0F;
    private static final float EAST_YAW = 225.0F;
    private static final float SPROCKET_RPM = -32.0F;
    private static final double FLOOR = 1.0;
    private static final double SUNK = FLOOR - (2.5 - 6.0 / 16.0);
    private static final double DRIVE_DISTANCE = 9.0;
    private static final double ARRIVAL_DISTANCE = 8.0;
    private static final double GRID = 9.0;
    private static final int ARRIVAL_WAIT = 20;
    private static final int TELL_AT = 25;
    private static final int FADE_TICKS = 15;
    private static final double ZOOM = 3.5;
    private static final float ZOOM_TILT = 15.0F;
    private static final float ZOOM_TURN = -40.0F;
    private static final int ZONE_COLOUR = 0xFF00FF00;
    private static final float ZONE_LINE = 1.0F / 64.0F;
    private static final float SHIFT = 3.0F / 16.0F;
    private static final double TRACK_ZOOM = 2.0;
    private static final int TENSION_STEP_TICKS = 8;
    private static final int SWEEP_PERIOD = 60;
    static final Vec3 SEAT = new Vec3(4.5, 4.5, 4.5);
    private static final Vec3 TRACK_MIDDLE = new Vec3(2.5, 3.0, 4.5);
    private static final Vec3 RUN_MIDDLE = new Vec3(2.5, 3.44, 5.0);
    private static final Vec3 LIFT = Vec3.atLowerCornerOf(Direction.UP.getOpposite().getNormal()).scale(0.5);
    private static final List<BlockPos> TINIES = List.of(
        new BlockPos(2, 3, 3), new BlockPos(2, 3, 6), new BlockPos(6, 3, 3), new BlockPos(6, 3, 6));

    private BntBobScenes() {
    }

    public static void collisions(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cog_alignment_lever_collisions", "Using the C.A.L. to enable collisions");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9F);

        ItemStack lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.asStack();
        Selection controllers = util.select().position(2, 3, 2).add(util.select().position(6, 3, 2));
        List<BlockPos> westWheels = new ArrayList<>();
        List<BlockPos> eastWheels = new ArrayList<>();
        for (int z = 3; z <= 6; z++) {
            westWheels.add(util.grid().at(2, 2, z));
            eastWheels.add(util.grid().at(6, 2, z));
        }
        BlockState plainWheel = BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.getDefaultState()
            .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X);
        for (BlockPos wheel : westWheels) {
            scene.world().setBlock(wheel, plainWheel, false);
        }
        for (BlockPos wheel : eastWheels) {
            scene.world().setBlock(wheel, plainWheel, false);
        }
        BntFlangedCogwheelScenes.hideTread(scene, controllers);

        ElementLink<WorldSectionElement> floor = scene.world().showIndependentSection(util.select().position(4, 0, 4), Direction.UP);
        scene.idle(20);
        ItemEntity[] dropped = new ItemEntity[1];
        scene.world().createEntity(level -> {
            ItemEntity item = new ItemEntity(level, 4.5, 3.5, 4.5, lever, 0.0, 0.0, 0.0);
            item.setNoGravity(true);
            dropped[0] = item;
            return item;
        });
        scene.addInstruction(fall(dropped));
        scene.idle(25);
        scene.overlay().showText(100)
            .text("The Cog Alignment Lever is the main multitool for Bits 'n' Tracks. So much so that every possible keybind imaginable is mapped to it.")
            .placeNearTarget()
            .pointAt(util.vector().of(4.5, 1.15, 4.5));
        scene.addKeyframe();
        scene.idle(110);
        caption(scene, "Let's see it in action", 50);

        scene.world().modifyEntities(ItemEntity.class, Entity::discard);
        scene.world().showSectionAndMerge(util.select().layer(0).substract(util.select().position(4, 0, 4)), Direction.UP, floor);
        scene.idle(25);

        BlockPos chair = util.grid().at(4, 4, 4);
        List<BlockPos> parts = blocksOfBob(builder);
        TreeMap<Integer, Selection> rows = rowsOf(util, parts);
        Selection whole = wholeOf(rows);
        parts.remove(chair);
        parts.sort(Comparator.<BlockPos>comparingInt(pos -> (8 - pos.getX()) + pos.getZ()).thenComparingInt(BlockPos::getY));
        ElementLink<WorldSectionElement> bob = null;
        for (BlockPos pos : parts) {
            Selection part = util.select().position(pos);
            if (bob == null) {
                bob = scene.world().showIndependentSection(part, Direction.DOWN);
            } else {
                scene.world().showSectionAndMerge(part, Direction.DOWN, bob);
            }
            scene.idle(2);
        }
        scene.idle(8);
        scene.world().showSectionAndMerge(util.select().position(chair), Direction.DOWN, bob);
        scene.idle(15);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(SEAT.add(0.0, 3.5, 0.0), Flying::new);
        scene.special().moveParrot(parrot, new Vec3(0.0, -3.5, 0.0), 40);
        scene.idle(40);
        scene.special().changeBirbPose(parrot, Perched::new);
        scene.idle(15);
        caption(scene, "Meet Bob. He will be our candidate for the rest of the ponders regarding Bits 'n' Tracks", 90,
            util.vector().of(4.5, 5.0, 4.5));

        ItemStack belt = AllItems.BELT_CONNECTOR.asStack();
        int[][] loop = {{3, 2}, {3, 3}, {3, 6}, {3, 7}, {2, 6}, {2, 5}, {2, 4}, {2, 3}, {3, 2}};
        for (int[] cog : loop) {
            for (int x : new int[]{2, 6}) {
                scene.overlay().showControls(util.vector().topOf(x, cog[0], cog[1]), Pointing.DOWN, 10).rightClick().withItem(belt);
            }
            scene.idle(14);
        }
        scene.world().restoreBlocks(controllers);
        scene.idle(30);

        caption(scene, "There's one thing that Bob would want, and it's for his cogwheels to have collision", 90);

        ItemStack glue = honeyGlue();
        scene.overlay().showControls(util.vector().of(2.0, 2.0, 1.0), Pointing.DOWN, 20).rightClick().withItem(glue);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "honey_glue", new AABB(2.0, 2.0, 1.0, 2.05, 2.05, 1.05), 5);
        scene.idle(5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "honey_glue", new AABB(2.0, 2.0, 1.0, 7.0, 6.0, 8.0), 60);
        scene.idle(20);
        scene.overlay().showControls(util.vector().of(7.0, 6.0, 8.0), Pointing.DOWN, 20).rightClick().withItem(glue);
        scene.idle(45);

        BlockPos assembler = util.grid().at(4, 5, 7);
        clickAssembler(scene, util, assembler, true);
        move(scene, bob, parrot, new Vec3(0.0, SUNK, 0.0), 12);
        scene.idle(35);
        scene.overlay().showText(100)
            .colored(PonderPalette.RED)
            .text("In order for the belt to have contact with the ground, you need to enable the cogwheel it's wrapped on")
            .placeNearTarget()
            .pointAt(util.vector().of(2.0, FLOOR + 0.1, 4.5));
        scene.addKeyframe();
        scene.idle(110);

        move(scene, bob, parrot, new Vec3(0.0, -SUNK, 0.0), 20);
        scene.idle(30);
        clickAssembler(scene, util, assembler, false);
        scene.idle(30);
        scene.addKeyframe();

        for (BlockPos wheel : westWheels) {
            enable(scene, util, wheel, Direction.WEST, Pointing.RIGHT, lever, 20 * (westWheels.size() - westWheels.indexOf(wheel)) + 10);
            if (wheel.equals(westWheels.get(0))) {
                scene.overlay().showText(160)
                    .text(tint(ChatFormatting.GRAY, "You can do that by left-clicking a flanged cogwheel with the Cog Alignment Lever"))
                    .placeNearTarget()
                    .pointAt(util.vector().blockSurface(wheel, Direction.WEST));
            }
        }
        scene.idle(10);
        scene.rotateCameraY(EAST_YAW - DEFAULT_YAW);
        scene.idle(25);
        for (BlockPos wheel : eastWheels) {
            enable(scene, util, wheel, Direction.EAST, Pointing.LEFT, lever, 20 * (eastWheels.size() - eastWheels.indexOf(wheel)) + 10);
        }
        scene.idle(10);
        scene.rotateCameraY(DEFAULT_YAW - EAST_YAW);
        scene.idle(25);

        caption(scene, "After every cogwheel you want to have collision has been enabled, you can assemble your contraption and observe how it behaves!", 100);

        double settle = restingDrop();
        clickAssembler(scene, util, assembler, true);
        move(scene, bob, parrot, new Vec3(0.0, -settle, 0.0), 8);
        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "west_contact", new AABB(2.0, FLOOR, 3.0, 3.0, FLOOR + 0.1, 7.0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "east_contact", new AABB(6.0, FLOOR, 3.0, 7.0, FLOOR + 0.1, 7.0), 60);
        scene.idle(70);

        caption(scene, "Now when you power the track...", 60);

        flipLever(scene, util, new Vec3(0.0, -settle, 0.0));
        power(scene, util, true);
        spin(scene, util, SPROCKET_RPM, 1.0F);
        scene.idle(5);
        int ticks = ticksFor(DRIVE_DISTANCE);
        scene.addInstruction(new Departure(bob, parrot, whole, rows, chair.getZ(), ticks));
        scene.idle(TELL_AT);
        scene.overlay().showText(70).text("Your vehicle will move!");
        scene.idle(ticks + FADE_TICKS - TELL_AT);
        scene.markAsFinished();
    }

    public static void customizing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cog_alignment_lever_customizing", "Customizing tracks with the C.A.L.");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9F);

        PonderLevel world = builder.getScene().getWorld();
        ItemStack lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.asStack();
        BlockPos tiny = util.grid().at(2, 3, 3);
        List<BlockPos> otherTinies = List.of(util.grid().at(2, 3, 6), util.grid().at(6, 3, 3), util.grid().at(6, 3, 6));
        Arrived bob = arrive(builder, scene, util, false);
        Vec3 rest = bob.rest();

        caption(scene, "There are many things Bob likes about his tracks, and one of them is being able to do whatever he wants with them.", 100);
        caption(scene, "In fact. There's everything " + ChatFormatting.BOLD + "you" + ChatFormatting.RESET + " want to customize your tracks with.", 80);

        scene.addInstruction(BntPonderCamera.glide(Vec3.atCenterOf(tiny).add(rest), ZOOM, ZOOM_TILT, ZOOM_TURN, 30));
        scene.idle(40);
        caption(scene, "First thing you can do, is " + tint(ChatFormatting.YELLOW, "move already-placed cogwheels however you want them")
            + ChatFormatting.RESET + ".", 80);

        Direction[] everyZone = {null, Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH};
        scene.addInstruction(new Zones(tiny, Direction.WEST, rest, everyZone, 6, 120));
        scene.overlay().showText(110)
            .text(tint(ChatFormatting.GRAY, "You can shift cogwheels up to 16 pixels, in all directions"))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(tiny, Direction.WEST).add(rest));
        scene.addKeyframe();
        scene.idle(130);

        scene.addInstruction(new Zones(tiny, Direction.WEST, rest, new Direction[]{Direction.UP}, 1, 90));
        scene.idle(15);
        for (int px = 1; px <= 3; px++) {
            Vec3 target = zoneBox(world, tiny, Direction.WEST, Direction.UP).getCenter().add(0.0, (px - 1) / 16.0, 0.0).add(rest);
            scene.overlay().showControls(target, Pointing.RIGHT, 10).rightClick().withItem(lever);
            scene.idle(8);
            float offset = px / 16.0F;
            scene.world().modifyBlockEntity(tiny, KineticBlockEntity.class, be -> shift(be, offset));
            scene.idle(12);
        }
        scene.idle(25);

        caption(scene, "Secondly, you can decide " + tint(ChatFormatting.YELLOW, "what way the track wraps around it")
            + ChatFormatting.RESET + ".", 80);

        scene.addInstruction(new Zones(tiny, Direction.WEST, rest, new Direction[]{Direction.DOWN}, 1, 90));
        scene.overlay().showText(120)
            .text("By selecting what side the belts wraps around of, the routing is switched from "
                + ChatFormatting.GREEN + "automatic" + ChatFormatting.RESET + " to " + ChatFormatting.YELLOW + "manual");
        scene.addKeyframe();
        scene.idle(40);
        Vec3 bottom = zoneBox(world, tiny, Direction.WEST, Direction.DOWN).getCenter().add(0.0, SHIFT, 0.0).add(rest);
        scene.overlay().showControls(bottom, Pointing.RIGHT, 15).leftClick().whileSneaking().withItem(lever);
        scene.idle(10);
        scene.world().modifyBlockEntity(tiny, KineticBlockEntity.class, BntBobScenes::wrapUnder);
        scene.idle(90);

        for (BlockPos other : otherTinies) {
            scene.world().modifyBlockEntity(other, KineticBlockEntity.class, be -> {
                shift(be, SHIFT);
                wrapUnder(be);
            });
        }
        scene.addInstruction(BntPonderCamera.glideHome(-ZOOM_TILT, -ZOOM_TURN, 30));
        scene.idle(60);

        scene.special().changeBirbPose(bob.parrot(), Cheering::new);
        scene.overlay().showText(80)
            .text("Just look at how happy Bob is to have all this freedom.")
            .placeNearTarget()
            .pointAt(SEAT.add(rest).add(0.0, 0.6, 0.0));
        scene.addKeyframe();
        scene.idle(90);
        scene.special().changeBirbPose(bob.parrot(), Perched::new);
        scene.idle(15);

        int ticks = leave(scene, util, bob);
        scene.idle(ticks + FADE_TICKS);
        scene.markAsFinished();
    }

    public static void tension(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cog_alignment_lever_tension", "Adjusting track tension with the C.A.L.");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9F);

        ItemStack lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.asStack();
        BlockPos controller = util.grid().at(2, 3, 2);
        Arrived bob = arrive(builder, scene, util, true);
        Vec3 rest = bob.rest();
        Vec3 run = RUN_MIDDLE.add(rest);

        caption(scene, "There's one last thing you can use the Cog Alignment Lever for...", 80);

        scene.addInstruction(BntPonderCamera.glide(TRACK_MIDDLE.add(rest), TRACK_ZOOM, ZOOM_TILT, ZOOM_TURN, 30));
        scene.idle(40);
        caption(scene, "And it is adjusting the belt's tension to fit your needs, or looks.", 90);

        int highlight = 140 + 10 + 10 * TENSION_STEP_TICKS + 30 + 80 + 10 + 10 * TENSION_STEP_TICKS + 40;
        scene.addInstruction(new BeltHighlight(controller, rest, RUN_MIDDLE, highlight, 120));
        scene.overlay().showText(130)
            .text("By looking at any point on the track, and shift-right-clicking on it with the Cog Alignment Lever, you can "
                + ChatFormatting.RED + "slack" + ChatFormatting.RESET + " the track.");
        scene.addKeyframe();
        scene.idle(140);

        scene.overlay().showControls(run, Pointing.DOWN, 10 * TENSION_STEP_TICKS + 20).rightClick().whileSneaking().withItem(lever);
        scene.idle(10);
        List<TextWindowElement> slacker = new ArrayList<>();
        for (int step = 9; step >= 0; step--) {
            slacker.add(BntFlangedCogwheelScenes.percentLabel(builder, step * 10, run));
        }
        scene.addInstruction(new BntLabelSequenceInstruction(slacker, labelTimes(30)));
        for (int step = 9; step >= 0; step--) {
            setTension(scene, controller, step / 10.0F);
            scene.idle(TENSION_STEP_TICKS);
        }
        scene.idle(30);

        caption(scene, "And when instead right-clicking...", 70);

        scene.overlay().showControls(run, Pointing.DOWN, 10 * TENSION_STEP_TICKS + 20).rightClick().withItem(lever);
        scene.idle(10);
        List<TextWindowElement> tighter = new ArrayList<>();
        for (int step = 1; step <= 10; step++) {
            tighter.add(BntFlangedCogwheelScenes.percentLabel(builder, step * 10, run));
        }
        scene.addInstruction(new BntLabelSequenceInstruction(tighter, labelTimes(40)));
        for (int step = 1; step <= 10; step++) {
            setTension(scene, controller, step / 10.0F);
            if (step == 6) {
                scene.overlay().showText(80).text("You can " + ChatFormatting.GREEN + "tighten" + ChatFormatting.RESET + " the track.");
                scene.addKeyframe();
            }
            scene.idle(TENSION_STEP_TICKS);
        }
        scene.idle(40);

        scene.addInstruction(BntPonderCamera.glideHome(-ZOOM_TILT, -ZOOM_TURN, 30));
        scene.idle(40);
        caption(scene, "Choosing the right tension for your vehicle influences other stuff, such as track rigidity, terrain adaptability, and suspension limitations.", 120);

        int ticks = leave(scene, util, bob);
        int peel = (int)Math.ceil(ticks * (bob.rows().firstKey() + 0.5) / DRIVE_DISTANCE);
        scene.idle(peel);
        scene.overlay().showText(70).text("Wait what? " + ChatFormatting.YELLOW + "Suspension" + ChatFormatting.RESET + "?");
        scene.idle(Math.max(ticks + FADE_TICKS - peel, 80));
        scene.markAsFinished();
    }

    private static Arrived arrive(SceneBuilder builder, CreateSceneBuilder scene, SceneBuildingUtil util, boolean customized) {
        PonderLevel world = builder.getScene().getWorld();
        Vec3 rest = new Vec3(0.0, -restingDrop(), 0.0);
        BlockPos chair = util.grid().at(4, 4, 4);
        Selection controllers = util.select().position(2, 3, 2).add(util.select().position(6, 3, 2));
        Map<BlockPos, CompoundTag> belts = new LinkedHashMap<>();
        controllers.forEach(pos -> {
            BlockEntity be = world.getBlockEntity(pos);
            if (be != null) {
                CompoundTag saved = be.saveWithFullMetadata(world.registryAccess());
                CompoundTag belt = new CompoundTag();
                belt.put("Chain", saved.getCompound("Chain"));
                belt.putInt("ChainsToRefund", saved.getInt("ChainsToRefund"));
                belts.put(pos.immutable(), belt);
            }
        });
        TreeMap<Integer, Selection> rows = rowsOf(util, blocksOfBob(builder));
        Selection whole = wholeOf(rows);

        if (customized) {
            for (BlockPos tiny : TINIES) {
                scene.world().modifyBlockEntity(tiny, KineticBlockEntity.class, be -> {
                    shift(be, SHIFT);
                    wrapUnder(be);
                });
            }
        }
        power(scene, util, true);
        spin(scene, util, SPROCKET_RPM, customized ? -1.0F : 1.0F);
        scene.world().modifyBlockEntity(util.grid().at(4, 5, 7), BlockEntity.class, be -> flickLever(be, true));
        BntFlangedCogwheelScenes.hideTread(scene, controllers);
        ElementLink<WorldSectionElement> bob = scene.world().showIndependentSectionImmediately(whole);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(SEAT, Perched::new);
        int arrival = ticksFor(ARRIVAL_DISTANCE);
        scene.addInstruction(new Arrival(bob, parrot, rows, chair.getZ(), rest, belts, arrival));
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(ARRIVAL_WAIT + arrival - 10);
        flipLever(scene, util, rest);
        power(scene, util, false);
        spin(scene, util, 0.0F, 1.0F);
        scene.idle(FADE_TICKS + 20);
        return new Arrived(bob, parrot, rows, whole, rest, chair.getZ());
    }

    private static int leave(CreateSceneBuilder scene, SceneBuildingUtil util, Arrived bob) {
        flipLever(scene, util, bob.rest());
        power(scene, util, true);
        spin(scene, util, SPROCKET_RPM, -1.0F);
        scene.idle(5);
        int ticks = ticksFor(DRIVE_DISTANCE);
        scene.addInstruction(new Departure(bob.section(), bob.parrot(), bob.whole(), bob.rows(), bob.seatRow(), ticks));
        return ticks;
    }

    private static int[] labelTimes(int hold) {
        int[] times = new int[10];
        Arrays.fill(times, TENSION_STEP_TICKS);
        times[times.length - 1] += hold;
        return times;
    }

    private static void setTension(CreateSceneBuilder scene, BlockPos controller, float tension) {
        scene.world().modifyBlockEntity(controller, KineticBlockEntity.class, be -> {
            if (be instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setBeltTension(tension);
            }
        });
    }

    static List<BlockPos> blocksOfBob(SceneBuilder builder) {
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 8, 5, 8)) {
            if (!builder.getScene().getWorld().getBlockState(pos).isAir()) {
                blocks.add(pos.immutable());
            }
        }
        return blocks;
    }

    static TreeMap<Integer, Selection> rowsOf(SceneBuildingUtil util, List<BlockPos> blocks) {
        TreeMap<Integer, Selection> rows = new TreeMap<>();
        for (BlockPos pos : blocks) {
            rows.merge(pos.getZ(), util.select().position(pos), Selection::add);
        }
        return rows;
    }

    static Selection wholeOf(TreeMap<Integer, Selection> rows) {
        Selection whole = null;
        for (Selection row : rows.values()) {
            whole = whole == null ? row.copy() : whole.add(row);
        }
        return whole;
    }

    private static void move(CreateSceneBuilder scene, ElementLink<WorldSectionElement> bob, ElementLink<ParrotElement> parrot,
                             Vec3 offset, int ticks) {
        scene.world().moveSection(bob, offset, ticks);
        scene.special().moveParrot(parrot, offset, ticks);
    }

    static void caption(CreateSceneBuilder scene, String text, int ticks) {
        scene.overlay().showText(ticks).text(text);
        scene.addKeyframe();
        scene.idle(ticks + 10);
    }

    private static void caption(CreateSceneBuilder scene, String text, int ticks, Vec3 target) {
        scene.overlay().showText(ticks).text(text).placeNearTarget().pointAt(target);
        scene.addKeyframe();
        scene.idle(ticks + 10);
    }

    private static void enable(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos wheel, Direction face, Pointing pointing,
                               ItemStack lever, int outline) {
        scene.overlay().showControls(util.vector().blockSurface(wheel, face), pointing, 10).leftClick().withItem(lever);
        scene.idle(8);
        scene.world().restoreBlocks(util.select().position(wheel));
        scene.effects().indicateSuccess(wheel);
        scene.overlay().showOutline(PonderPalette.GREEN, wheel, util.select().position(wheel), outline);
        scene.idle(12);
    }

    static void clickAssembler(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos assembler, boolean assemble) {
        scene.overlay().showControls(util.vector().centerOf(assembler).add(0.0, 0.4, 0.0), Pointing.DOWN, 20).rightClick();
        scene.idle(10);
        scene.world().modifyBlockEntity(assembler, BlockEntity.class, be -> flickLever(be, assemble));
    }

    static void flipLever(CreateSceneBuilder scene, SceneBuildingUtil util, Vec3 rest) {
        scene.overlay().showControls(util.vector().topOf(4, 5, 6).add(rest), Pointing.DOWN, 20).rightClick();
        scene.idle(10);
    }

    static void power(CreateSceneBuilder scene, SceneBuildingUtil util, boolean on) {
        scene.world().modifyBlock(util.grid().at(4, 5, 6), state -> flag(state, "powered", on), false);
        scene.world().modifyBlock(util.grid().at(4, 5, 5), state -> flag(state, "powered", on), false);
        scene.world().modifyBlock(util.grid().at(4, 3, 1), state -> flag(state, "powered", on), false);
        for (int x : new int[]{3, 5}) {
            scene.world().modifyBlock(util.grid().at(x, 3, 1), state -> level(state, "power", on ? 15 : 0), false);
        }
    }

    static void spin(CreateSceneBuilder scene, SceneBuildingUtil util, float rpm, float rollerSense) {
        float tinyRpm = (float)(rollerSense * rpm * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get())
            / CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get()));
        Selection engine = util.select().fromTo(4, 4, 5, 4, 4, 7);
        Selection axle = util.select().fromTo(2, 3, 7, 6, 3, 7);
        Selection tracks = util.select().fromTo(2, 2, 3, 2, 2, 6).add(util.select().fromTo(6, 2, 3, 6, 2, 6))
            .add(util.select().position(2, 3, 2)).add(util.select().position(6, 3, 2));
        Selection rollers = util.select().position(2, 3, 3).add(util.select().position(2, 3, 6))
            .add(util.select().position(6, 3, 3)).add(util.select().position(6, 3, 6));
        scene.world().setKineticSpeed(engine, -rpm);
        scene.world().setKineticSpeed(axle, rpm);
        scene.world().setKineticSpeed(tracks, rpm);
        scene.world().setKineticSpeed(rollers, tinyRpm);
    }

    private static int ticksFor(double distance) {
        double beltSpeed = -SPROCKET_RPM / 60.0 * 2.0 * Math.PI
            * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get());
        return (int)Math.round(distance / beltSpeed * 20.0);
    }

    static double restingDrop() {
        Block wheel = BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get();
        return 2.5 + CogwheelSizeHelper.getVisualVerticalOffset(wheel) - CogwheelSizeHelper.getTrackRadius(wheel) - FLOOR;
    }

    private static AABB zoneBox(Level level, BlockPos pos, Direction face, Direction zone) {
        BlockState state = level.getBlockState(pos);
        double radius = CogwheelSizeHelper.getToolHighlightRadius(state.getBlock());
        Vec3 hit = zone == null ? Vec3.ZERO : Vec3.atLowerCornerOf(zone.getNormal()).scale(0.75 * radius);
        return BntClientOutliner.getHighlightAABB(pos, face, hit.x, hit.y, hit.z, state.getValue(BlockStateProperties.AXIS), radius)
            .move(HiddenCogwheelCompat.getModelTranslation(level.getBlockEntity(pos), 1.0F));
    }

    private static void shift(KineticBlockEntity be, float offset) {
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setAlignmentOffsetY(offset);
        }
    }

    private static void wrapUnder(KineticBlockEntity be) {
        if (be instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$setTrackRouteSide(Direction.DOWN.ordinal());
        }
    }

    private static PonderInstruction fall(ItemEntity[] item) {
        return new TickingInstruction(false, 40) {
            private double speed;

            @Override
            protected void firstTick(PonderScene ponder) {
                super.firstTick(ponder);
                speed = 0.0;
            }

            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                ItemEntity entity = item[0];
                if (entity == null || entity.getY() <= FLOOR) {
                    return;
                }
                speed = (speed + 0.04) * 0.98;
                entity.setPos(entity.getX(), Math.max(FLOOR, entity.getY() - speed), entity.getZ());
            }
        };
    }

    private static BlockState flag(BlockState state, String name, boolean value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property instanceof BooleanProperty flag ? state.setValue(flag, value) : state;
    }

    private static BlockState level(BlockState state, String name, int value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property instanceof IntegerProperty power && power.getPossibleValues().contains(value) ? state.setValue(power, value) : state;
    }

    private static ItemStack honeyGlue() {
        Item glue = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("simulated", "honey_glue"));
        return glue == Items.AIR ? AllItems.SUPER_GLUE.asStack() : new ItemStack(glue);
    }

    static void flickLever(BlockEntity assembler, boolean on) {
        try {
            assembler.getClass().getMethod("clientFlickLeverTo", boolean.class).invoke(assembler, on);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static String tint(ChatFormatting format, String text) {
        return format + text.replace(" ", " " + format);
    }

    private static Parrot grey(Parrot parrot) {
        parrot.setVariant(Parrot.Variant.GRAY);
        return parrot;
    }

    private record Arrived(ElementLink<WorldSectionElement> section, ElementLink<ParrotElement> parrot, TreeMap<Integer, Selection> rows,
                           Selection whole, Vec3 rest, int seatRow) {
    }

    private static final class BeltHighlight extends TickingInstruction {
        private static final int LINE_COLOUR = 0x40FF60;
        private static final int POINTER_COLOUR = 0xFFFFFF;
        private static final float LINE_WIDTH = 1.0F / 48.0F;
        private static final float POINTER_SIZE = 1.0F / 6.0F;
        private static Method drawOutline;

        private final BlockPos controller;
        private final Vec3 rest;
        private final Vec3 middle;
        private final int sweep;

        BeltHighlight(BlockPos controller, Vec3 rest, Vec3 middle, int ticks, int sweep) {
            super(false, ticks);
            this.controller = controller;
            this.rest = rest;
            this.middle = middle;
            this.sweep = sweep;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            PonderLevel level = scene.getWorld();
            if (!(level.getBlockEntity(controller) instanceof KineticBlockEntity kinetic)
                || !(kinetic.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour)
                || behaviour.getControlledChain() == null) {
                return;
            }
            CogwheelChain chain = behaviour.getControlledChain();
            CogwheelChainWholeShape shape;
            BntChainShapeContext.set(level, controller);
            try {
                shape = CogwheelChainWholeShape.buildShape(chain);
            } finally {
                BntChainShapeContext.clear();
            }
            if (shape == null) {
                return;
            }

            Vec3 base = Vec3.atLowerCornerOf(controller).add(rest);
            List<Vec3> ends = edges(shape);
            for (int i = 0; i + 1 < ends.size(); i += 2) {
                scene.getOutliner().showLine(List.of(this, i), ends.get(i).add(base), ends.get(i + 1).add(base))
                    .colored(LINE_COLOUR).lineWidth(LINE_WIDTH).disableLineNormals();
            }

            int elapsed = totalTicks - remainingTicks;
            double swing = elapsed < sweep ? Math.sin(2.0 * Math.PI * elapsed / SWEEP_PERIOD) : 0.0;
            float centre = shape.getChainPosition(middle.subtract(Vec3.atLowerCornerOf(controller)));
            double side = Math.max(chain.getChainType().getRenderType().getWidth(), chain.getChainType().getRenderType().getHeight()) / 32.0
                + 1.0 / 16.0;
            Vec3 point = shape.getLocalVec(centre + (float)swing).add(base).add(-side, 0.0, 0.0);
            scene.getOutliner().chaseAABB(this, new AABB(point, point))
                .colored(POINTER_COLOUR).lineWidth(POINTER_SIZE).disableLineNormals();
        }

        private static List<Vec3> edges(CogwheelChainShape shape) {
            List<Vec3> ends = new ArrayList<>();
            VertexConsumer collector = new VertexConsumer() {
                @Override
                public VertexConsumer addVertex(float x, float y, float z) {
                    ends.add(new Vec3(x, y, z));
                    return this;
                }

                @Override
                public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                    return this;
                }

                @Override
                public VertexConsumer setUv(float u, float v) {
                    return this;
                }

                @Override
                public VertexConsumer setUv1(int u, int v) {
                    return this;
                }

                @Override
                public VertexConsumer setUv2(int u, int v) {
                    return this;
                }

                @Override
                public VertexConsumer setNormal(float x, float y, float z) {
                    return this;
                }
            };
            try {
                if (drawOutline == null) {
                    drawOutline = CogwheelChainShape.class.getDeclaredMethod("drawOutline", PoseStack.class, VertexConsumer.class, UnaryOperator.class);
                    drawOutline.setAccessible(true);
                }
                drawOutline.invoke(shape, new PoseStack(), collector, UnaryOperator.<Vec3>identity());
            } catch (ReflectiveOperationException ignored) {
                ends.clear();
            }
            return ends;
        }
    }

    private static final class Arrival extends TickingInstruction {
        private final ElementLink<WorldSectionElement> bob;
        private final ElementLink<ParrotElement> parrot;
        private final TreeMap<Integer, Selection> rows;
        private final int seatRow;
        private final Vec3 rest;
        private final Map<BlockPos, CompoundTag> belts;
        private final int drive;
        private final List<AnimatedSceneElement> entering = new ArrayList<>();
        private final List<Selection> sections = new ArrayList<>();
        private final List<Integer> ages = new ArrayList<>();
        private WorldSectionElement original;
        private WorldSectionElementImpl body;
        private ParrotElement bird;
        private Vec3 last;
        private boolean wrapped;
        private Iterator<Map.Entry<Integer, Selection>> next;
        private Map.Entry<Integer, Selection> pending;

        Arrival(ElementLink<WorldSectionElement> bob, ElementLink<ParrotElement> parrot, TreeMap<Integer, Selection> rows,
                int seatRow, Vec3 rest, Map<BlockPos, CompoundTag> belts, int drive) {
            super(false, ARRIVAL_WAIT + drive + FADE_TICKS);
            this.bob = bob;
            this.parrot = parrot;
            this.rows = rows;
            this.seatRow = seatRow;
            this.rest = rest;
            this.belts = belts;
            this.drive = drive;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            entering.clear();
            sections.clear();
            ages.clear();
            body = null;
            wrapped = false;
            original = scene.resolve(bob);
            if (original != null) {
                original.setVisible(false);
            }
            bird = scene.resolve(parrot);
            if (bird != null) {
                bird.setVisible(false);
            }
            last = rest.add(0.0, 0.0, ARRIVAL_DISTANCE);
            next = rows.entrySet().iterator();
            pending = next.hasNext() ? next.next() : null;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            int elapsed = totalTicks - remainingTicks;
            double travelled = ARRIVAL_DISTANCE * Mth.clamp((elapsed - ARRIVAL_WAIT) / (double)drive, 0.0, 1.0);
            Vec3 offset = rest.add(0.0, 0.0, ARRIVAL_DISTANCE - travelled);
            while (pending != null && elapsed > ARRIVAL_WAIT && travelled >= ARRIVAL_DISTANCE + pending.getKey() + 0.5 - GRID) {
                enter(scene, pending, offset);
                pending = next.hasNext() ? next.next() : null;
            }
            for (int i = entering.size() - 1; i >= 0; i--) {
                AnimatedSceneElement element = entering.get(i);
                int age = ages.get(i) + 1;
                ages.set(i, age);
                if (element instanceof WorldSectionElement row) {
                    row.setAnimatedOffset(offset, false);
                }
                float left = Math.max(0.0F, 1.0F - age / (float)FADE_TICKS);
                element.setFade(1.0F - left * left);
                if (left <= 0.0F || remainingTicks == 0) {
                    settle(scene, i);
                }
            }
            if (pending == null && entering.isEmpty() && !wrapped) {
                wrap(scene);
            }
            if (body != null) {
                body.setAnimatedOffset(offset, false);
            }
            if (bird != null) {
                bird.setPositionOffset(offset, false);
            }
            last = offset;
            if (remainingTicks == 0) {
                if (original != null) {
                    original.setAnimatedOffset(rest, true);
                    original.setVisible(true);
                }
                if (body != null) {
                    body.setVisible(false);
                }
            }
        }

        private void enter(PonderScene scene, Map.Entry<Integer, Selection> row, Vec3 offset) {
            WorldSectionElementImpl element = new WorldSectionElementImpl(row.getValue());
            scene.addElement(element);
            element.setVisible(true);
            element.forceApplyFade(0.0F);
            element.setFadeVec(LIFT);
            element.setAnimatedOffset(last, true);
            element.setAnimatedOffset(offset, false);
            entering.add(element);
            sections.add(row.getValue());
            ages.add(0);
            if (row.getKey() == seatRow && bird != null) {
                bird.setVisible(true);
                bird.forceApplyFade(0.0F);
                bird.setFadeVec(LIFT);
                entering.add(bird);
                sections.add(null);
                ages.add(0);
            }
        }

        private void settle(PonderScene scene, int index) {
            AnimatedSceneElement element = entering.remove(index);
            Selection section = sections.remove(index);
            ages.remove(index);
            element.setFade(1.0F);
            if (section == null) {
                return;
            }
            element.setVisible(false);
            if (body == null) {
                body = new WorldSectionElementImpl(section);
                scene.addElement(body);
                body.setVisible(true);
                body.forceApplyFade(1.0F);
                body.setAnimatedOffset(last, true);
            } else {
                body.add(section);
            }
        }

        private void wrap(PonderScene scene) {
            wrapped = true;
            PonderLevel level = scene.getWorld();
            belts.forEach((pos, belt) -> {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
                    tag.merge(belt.copy());
                    be.loadWithComponents(tag, level.registryAccess());
                }
            });
        }
    }

    private static final class Departure extends TickingInstruction {
        private final ElementLink<WorldSectionElement> bob;
        private final ElementLink<ParrotElement> parrot;
        private final Selection whole;
        private final TreeMap<Integer, Selection> rows;
        private final int seatRow;
        private final int drive;
        private final List<AnimatedSceneElement> leaving = new ArrayList<>();
        private final List<Integer> ages = new ArrayList<>();
        private WorldSectionElementImpl body;
        private ParrotElement bird;
        private Vec3 start;
        private Vec3 birdStart;
        private Iterator<Map.Entry<Integer, Selection>> next;
        private Map.Entry<Integer, Selection> pending;

        Departure(ElementLink<WorldSectionElement> bob, ElementLink<ParrotElement> parrot, Selection whole,
                  TreeMap<Integer, Selection> rows, int seatRow, int drive) {
            super(false, drive + FADE_TICKS);
            this.bob = bob;
            this.parrot = parrot;
            this.whole = whole;
            this.rows = rows;
            this.seatRow = seatRow;
            this.drive = drive;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            leaving.clear();
            ages.clear();
            WorldSectionElement original = scene.resolve(bob);
            start = original == null ? Vec3.ZERO : original.getAnimatedOffset();
            body = new WorldSectionElementImpl(whole);
            scene.addElement(body);
            body.setVisible(true);
            body.forceApplyFade(1.0F);
            body.setAnimatedOffset(start, true);
            if (original != null) {
                original.setVisible(false);
            }
            bird = scene.resolve(parrot);
            birdStart = bird == null ? Vec3.ZERO : bird.getPositionOffset();
            next = rows.entrySet().iterator();
            pending = next.hasNext() ? next.next() : null;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            double travelled = DRIVE_DISTANCE * Math.min(1.0, (totalTicks - remainingTicks) / (double)drive);
            Vec3 offset = start.add(0.0, 0.0, -travelled);
            body.setAnimatedOffset(offset, false);
            if (bird != null) {
                bird.setPositionOffset(birdStart.add(0.0, 0.0, -travelled), false);
            }
            while (pending != null && travelled >= pending.getKey() + 0.5) {
                WorldSectionElementImpl row = new WorldSectionElementImpl(pending.getValue());
                scene.addElement(row);
                row.setVisible(true);
                row.forceApplyFade(1.0F);
                row.setFadeVec(LIFT);
                row.setAnimatedOffset(offset, true);
                body.erase(pending.getValue());
                body.queueRedraw();
                leaving.add(row);
                ages.add(0);
                if (pending.getKey() == seatRow && bird != null) {
                    bird.setFadeVec(LIFT);
                    leaving.add(bird);
                    ages.add(0);
                }
                pending = next.hasNext() ? next.next() : null;
            }
            for (int i = 0; i < leaving.size(); i++) {
                AnimatedSceneElement element = leaving.get(i);
                int age = ages.get(i) + 1;
                ages.set(i, age);
                if (element instanceof WorldSectionElement row) {
                    row.setAnimatedOffset(offset, false);
                }
                float left = Math.max(0.0F, 1.0F - age / (float)FADE_TICKS);
                element.setFade(1.0F - (1.0F - left) * (1.0F - left));
                if (left <= 0.0F) {
                    element.setVisible(false);
                    element.setFade(0.0F);
                }
            }
        }
    }

    private static final class Zones extends TickingInstruction {
        private final BlockPos cog;
        private final Direction face;
        private final Vec3 rest;
        private final Direction[] cycle;
        private final int step;

        Zones(BlockPos cog, Direction face, Vec3 rest, Direction[] cycle, int step, int ticks) {
            super(false, ticks);
            this.cog = cog;
            this.face = face;
            this.rest = rest;
            this.cycle = cycle;
            this.step = step;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            Direction zone = cycle[(totalTicks - remainingTicks - 1) / step % cycle.length];
            AABB box = zoneBox(scene.getWorld(), cog, face, zone).move(rest);
            scene.getOutliner().showAABB(this, box).highlightFace(face).colored(ZONE_COLOUR).lineWidth(ZONE_LINE);
        }
    }

    private static final class Flying extends ParrotPose.FlappyPose {
        @Override
        public Parrot create(PonderLevel level) {
            return grey(super.create(level));
        }
    }

    static final class Perched extends ParrotPose {
        @Override
        public void tick(PonderScene scene, Parrot parrot, Vec3 location) {
            parrot.setOnGround(true);
            parrot.setInSittingPose(true);
        }

        @Override
        public Parrot create(PonderLevel level) {
            return grey(super.create(level));
        }
    }

    static final class Cheering extends ParrotPose {
        @Override
        public void tick(PonderScene scene, Parrot parrot, Vec3 location) {
            parrot.setInSittingPose(false);
            parrot.flapSpeed = 1.0F + Mth.sin(parrot.tickCount * 1.2F);
        }

        @Override
        public Parrot create(PonderLevel level) {
            return grey(super.create(level));
        }
    }
}
