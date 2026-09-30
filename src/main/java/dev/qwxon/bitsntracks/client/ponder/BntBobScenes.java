package dev.qwxon.bitsntracks.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final int TELL_AT = 25;
    private static final int FADE_TICKS = 15;
    private static final Vec3 SEAT = new Vec3(4.5, 4.5, 4.5);

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
        List<BlockPos> parts = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 8, 5, 8)) {
            if (!pos.equals(chair) && !builder.getScene().getWorld().getBlockState(pos).isAir()) {
                parts.add(pos.immutable());
            }
        }
        parts.sort(Comparator.<BlockPos>comparingInt(pos -> (8 - pos.getX()) + pos.getZ()).thenComparingInt(BlockPos::getY));
        ElementLink<WorldSectionElement> bob = null;
        Selection whole = util.select().position(chair);
        TreeMap<Integer, Selection> rows = new TreeMap<>();
        rows.put(chair.getZ(), util.select().position(chair));
        for (BlockPos pos : parts) {
            Selection part = util.select().position(pos);
            if (bob == null) {
                bob = scene.world().showIndependentSection(part, Direction.DOWN);
            } else {
                scene.world().showSectionAndMerge(part, Direction.DOWN, bob);
            }
            whole = whole.add(util.select().position(pos));
            rows.merge(pos.getZ(), util.select().position(pos), Selection::add);
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
                    .text(gray("You can do that by left-clicking a flanged cogwheel with the Cog Alignment Lever"))
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

        BlockPos switchLever = util.grid().at(4, 5, 6);
        scene.overlay().showControls(util.vector().topOf(switchLever).add(0.0, -settle, 0.0), Pointing.DOWN, 20).rightClick();
        scene.idle(10);
        scene.world().modifyBlock(switchLever, state -> flag(state, "powered", true), false);
        scene.world().modifyBlock(util.grid().at(4, 5, 5), state -> flag(state, "powered", true), false);
        scene.world().modifyBlock(util.grid().at(4, 3, 1), state -> flag(state, "powered", true), false);
        for (int x : new int[]{3, 5}) {
            scene.world().modifyBlock(util.grid().at(x, 3, 1), state -> level(state, "power", 15), false);
        }
        drive(scene, util);
        scene.idle(5);
        int ticks = driveTicks();
        scene.addInstruction(new Departure(bob, parrot, whole, rows, chair.getZ(), ticks));
        scene.idle(TELL_AT);
        scene.overlay().showText(70).text("Your vehicle will move!");
        scene.idle(ticks + FADE_TICKS - TELL_AT);
        scene.markAsFinished();
    }

    private static void move(CreateSceneBuilder scene, ElementLink<WorldSectionElement> bob, ElementLink<ParrotElement> parrot,
                             Vec3 offset, int ticks) {
        scene.world().moveSection(bob, offset, ticks);
        scene.special().moveParrot(parrot, offset, ticks);
    }

    private static void caption(CreateSceneBuilder scene, String text, int ticks) {
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

    private static void clickAssembler(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos assembler, boolean assemble) {
        scene.overlay().showControls(util.vector().centerOf(assembler).add(0.0, 0.4, 0.0), Pointing.DOWN, 20).rightClick();
        scene.idle(10);
        scene.world().modifyBlockEntity(assembler, BlockEntity.class, be -> flickLever(be, assemble));
    }

    private static void drive(CreateSceneBuilder scene, SceneBuildingUtil util) {
        float tinyRpm = (float)(SPROCKET_RPM * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get())
            / CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get()));
        Selection engine = util.select().fromTo(4, 4, 5, 4, 4, 7);
        Selection axle = util.select().fromTo(2, 3, 7, 6, 3, 7);
        Selection tracks = util.select().fromTo(2, 2, 3, 2, 2, 6).add(util.select().fromTo(6, 2, 3, 6, 2, 6))
            .add(util.select().position(2, 3, 2)).add(util.select().position(6, 3, 2));
        Selection rollers = util.select().position(2, 3, 3).add(util.select().position(2, 3, 6))
            .add(util.select().position(6, 3, 3)).add(util.select().position(6, 3, 6));
        scene.world().setKineticSpeed(engine, -SPROCKET_RPM);
        scene.world().setKineticSpeed(axle, SPROCKET_RPM);
        scene.world().setKineticSpeed(tracks, SPROCKET_RPM);
        scene.world().setKineticSpeed(rollers, tinyRpm);
    }

    private static int driveTicks() {
        double beltSpeed = -SPROCKET_RPM / 60.0 * 2.0 * Math.PI
            * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get());
        return (int)Math.round(DRIVE_DISTANCE / beltSpeed * 20.0);
    }

    private static double restingDrop() {
        Block wheel = BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get();
        return 2.5 + CogwheelSizeHelper.getVisualVerticalOffset(wheel) - CogwheelSizeHelper.getTrackRadius(wheel) - FLOOR;
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

    private static void flickLever(BlockEntity assembler, boolean on) {
        try {
            assembler.getClass().getMethod("clientFlickLeverTo", boolean.class).invoke(assembler, on);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static String gray(String text) {
        return ChatFormatting.GRAY + text.replace(" ", " " + ChatFormatting.GRAY);
    }

    private static Parrot grey(Parrot parrot) {
        parrot.setVariant(Parrot.Variant.GRAY);
        return parrot;
    }

    private static final class Departure extends TickingInstruction {
        private static final Vec3 LIFT = Vec3.atLowerCornerOf(Direction.UP.getOpposite().getNormal()).scale(0.5);

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

    private static final class Flying extends ParrotPose.FlappyPose {
        @Override
        public Parrot create(PonderLevel level) {
            return grey(super.create(level));
        }
    }

    private static final class Perched extends ParrotPose {
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
}
