package dev.qwxon.bitsntracks.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class BntVehicleScenes {
    private static final float SPROCKET_RPM = -32.0F;
    private static final float WHEEL_RPM = -48.0F;
    private static final double REST_HEAVE = -0.55;
    private static final double REST_DROP = 0.35;
    private static final int PLACEMENT_GREEN = 0x95CD41;
    private static final DustParticleOptions PLACEMENT_DUST = new DustParticleOptions(
        new Vector3f(0x95 / 255.0F, 0xCD / 255.0F, 0x41 / 255.0F), 1.0F);
    private static final float DEFAULT_YAW = 145.0F;
    private static final float EAST_YAW = 225.0F;
    private static final float DRIVE_YAW = 250.0F;
    private static final String ENABLE = ChatFormatting.GREEN + "enable" + ChatFormatting.RESET;

    private BntVehicleScenes() {
    }

    public static void vehicle(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cog_alignment_lever_vehicle", "Using the Bits 'n' the Tracks with the Cog Alignment Lever");
        scene.configureBasePlate(0, 0, 11);
        scene.scaleSceneView(0.85F);
        scene.removeShadow();

        BntTankRig rig = new BntTankRig();
        scene.addInstruction(rig.driver());

        ItemStack lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.asStack();
        ItemStack belt = BitsNTracksItems.INDUSTRIAL_BELT.asStack();
        Selection controllers = util.select().position(2, 3, 2).add(util.select().position(8, 3, 2));
        Selection sprockets = controllers.copy().add(util.select().position(2, 3, 9)).add(util.select().position(8, 3, 9));
        BlockPos westRoller = util.grid().at(2, 3, 7);
        BlockPos eastRoller = util.grid().at(8, 3, 7);
        Selection rollers = util.select().position(westRoller).add(util.select().position(eastRoller));
        List<BlockPos> westWheels = new ArrayList<>();
        List<BlockPos> eastWheels = new ArrayList<>();
        Selection wheels = util.select().fromTo(2, 2, 3, 2, 2, 8).add(util.select().fromTo(8, 2, 3, 8, 2, 8));
        for (int z : new int[]{3, 5, 6, 8}) {
            westWheels.add(util.grid().at(2, 2, z));
            eastWheels.add(util.grid().at(8, 2, z));
        }

        BlockState plainWheel = BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.getDefaultState()
            .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X);
        for (BlockPos wheel : westWheels) {
            scene.world().setBlock(wheel, plainWheel, false);
        }
        for (BlockPos wheel : eastWheels) {
            scene.world().setBlock(wheel, plainWheel, false);
        }
        BntFlangedCogwheelScenes.hideTread(scene, controllers);

        ElementLink<WorldSectionElement> floor = scene.world().showIndependentSection(util.select().position(5, 0, 5), Direction.UP);
        rig.floor(floor);
        scene.idle(20);
        ItemEntity[] dropped = new ItemEntity[1];
        scene.world().createEntity(level -> {
            ItemEntity item = new ItemEntity(level, 5.5, 4.5, 5.5, lever, 0.0, 0.0, 0.0);
            item.setNoGravity(true);
            dropped[0] = item;
            return item;
        });
        scene.addInstruction(fall(dropped, BntTankBody.FLOOR));
        scene.idle(25);
        scene.overlay().showText(110)
            .text("In order to use the mod's track as a way to propel your vehicle across the world, you will need to use the Cog Alignment Lever")
            .placeNearTarget()
            .pointAt(util.vector().of(5.5, 1.15, 5.5));
        scene.addKeyframe();
        scene.idle(120);

        scene.world().modifyEntities(ItemEntity.class, Entity::discard);
        scene.world().showSectionAndMerge(util.select().layer(0).substract(util.select().position(5, 0, 5)), Direction.UP, floor);
        scene.idle(25);
        caption(scene, "So let's see it used on a test vehicle!", 70);

        Selection tracks = util.select().fromTo(2, 2, 2, 2, 3, 9).add(util.select().fromTo(8, 2, 2, 8, 3, 9)).substract(rollers);
        ElementLink<WorldSectionElement> vehicle = scene.world().showIndependentSection(tracks, Direction.DOWN);
        rig.vehicle(vehicle);
        scene.world().configureCenterOfRotation(vehicle, rig.pivot());
        scene.idle(10);
        scene.world().showSectionAndMerge(util.select().fromTo(3, 2, 2, 7, 3, 9), Direction.DOWN, vehicle);
        scene.idle(10);
        scene.world().showSectionAndMerge(util.select().fromTo(2, 4, 2, 8, 5, 9), Direction.DOWN, vehicle);
        scene.idle(25);
        caption(scene, "In order to allow your cogwheels to tread the ground without sinking in it, you will need to " + ENABLE + " them", 90);

        for (BlockPos wheel : westWheels) {
            enable(scene, util, wheel, Direction.WEST, Pointing.RIGHT, lever);
        }
        scene.rotateCameraY(EAST_YAW - DEFAULT_YAW);
        scene.idle(25);
        for (BlockPos wheel : eastWheels) {
            enable(scene, util, wheel, Direction.EAST, Pointing.LEFT, lever);
        }
        scene.rotateCameraY(DEFAULT_YAW - EAST_YAW);
        scene.idle(25);
        caption(scene, "After you have enabled the needed cogwheels, wrap your track around them", 70);

        for (int z : new int[]{2, 9, 8, 6, 5, 3, 2}) {
            for (int x : new int[]{2, 8}) {
                boolean sprocket = z == 2 || z == 9;
                Vec3 rim = util.vector().centerOf(x, sprocket ? 3 : 2, z).add(0.0, sprocket ? 0.9 : 0.7, 0.0);
                scene.overlay().showControls(rim, Pointing.DOWN, 8).rightClick().withItem(belt);
            }
            scene.idle(16);
        }
        scene.world().restoreBlocks(controllers);
        scene.world().modifyBlockEntityNBT(controllers, KineticBlockEntity.class, BntVehicleScenes::withoutRollers, false);
        scene.idle(25);
        caption(scene, "Remember to always add your belt after you've done all modifications needed to your trackset before assembling your contraption", 90);
        caption(scene, "If you happen to forget something or want to add another cog to the system, right-click the run of track with the cog you want to include, and a placement hint will appear", 110);

        ItemStack roller = BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.asStack();
        scene.overlay().showControls(util.vector().of(2.5, 4.3, 6.5), Pointing.DOWN, 60).rightClick().withItem(roller);
        scene.idle(10);
        scene.addInstruction(placementHint(westRoller, 50));
        scene.addInstruction(placementHint(eastRoller, 50));
        scene.idle(50);
        scene.world().showSectionAndMerge(rollers, Direction.DOWN, vehicle);
        scene.idle(10);
        scene.world().restoreBlocks(controllers);
        scene.idle(25);
        caption(scene, "If at any point your track failed to assemble properly, or you changed it by adding or breaking a cogwheel, or you assembled it in the air, worry not! You can refresh it by right-clicking on it with the Cog Alignment Lever", 110);
        caption(scene, "After you've checked everything is in order, you can assemble your contraption!", 70);

        ItemStack glue = honeyGlue();
        scene.overlay().showControls(util.vector().of(2.0, 2.0, 2.0), Pointing.DOWN, 20).rightClick().withItem(glue);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "honey_glue", new AABB(2.0, 2.0, 2.0, 2.05, 2.05, 2.05), 5);
        scene.idle(5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "honey_glue", new AABB(2.0, 2.0, 2.0, 9.0, 6.0, 10.0), 90);
        scene.idle(20);
        scene.overlay().showControls(util.vector().of(9.0, 6.0, 10.0), Pointing.DOWN, 20).rightClick().withItem(glue);
        scene.idle(30);
        BlockPos assembler = util.grid().at(5, 5, 9);
        scene.overlay().showControls(util.vector().centerOf(assembler).add(0.0, 0.4, 0.0), Pointing.DOWN, 20).rightClick();
        scene.idle(10);
        scene.world().modifyBlockEntity(assembler, BlockEntity.class, BntVehicleScenes::pullLever);
        scene.addInstruction(ponder -> rig.release());
        scene.idle(60);
        caption(scene, "As you can see, the cogwheels aren't sinking into the ground, because they had their collisions enabled", 90,
            util.vector().of(2.0, BntTankBody.AXLE_Y + REST_HEAVE - REST_DROP, 8.5));
        scene.rotateCameraY(DRIVE_YAW - DEFAULT_YAW);
        scene.idle(25);

        Selection runway = util.select().fromTo(0, 0, 0, 10, 0, 9);
        int home = (int)(-BntTankRig.RUNWAY_START / BntTankRig.PIECE_LENGTH);
        placePiece(scene, rig, home, scene.world().showIndependentSectionImmediately(runway));
        placePiece(scene, rig, home + 1, scene.world().showIndependentSection(runway, Direction.DOWN));
        for (int step = 1; step < BntTankRig.PIECES; step++) {
            int north = home - step;
            int south = home + 1 + step;
            if (north >= 0) {
                placePiece(scene, rig, north, scene.world().showIndependentSection(runway, Direction.DOWN));
            }
            if (south < BntTankRig.PIECES) {
                placePiece(scene, rig, south, scene.world().showIndependentSection(runway, Direction.DOWN));
            }
            scene.idle(4);
        }
        scene.addInstruction(rig::sendFloorAway);
        scene.idle(15);
        drive(scene, util, rig, vehicle, sprockets, wheels, rollers, true);
        scene.idle(30);
        caption(scene, "Speed is determined by the RPM of the driving cogwheel", 70,
            util.vector().of(9.0, BntTankBody.SPROCKET_Y + REST_HEAVE, 9.5));

        Vec3 lowerRun = util.vector().of(9.0, 1.1, 6.0);
        placeSteps(scene, util, rig);
        scene.idle(40);
        caption(scene, "The tracks can deform based on the ground they are treading, and how much they deform is determined by their tension", 100, lowerRun);
        scene.idle(75);

        Vec3 upperRun = util.vector().of(8.5, 3.8, 6.0);
        scene.addKeyframe();
        sweepTension(builder, scene, rig, controllers, 100, 60, lowerRun, upperRun, lever);
        placeSteps(scene, util, rig);
        scene.idle(40);
        caption(scene, "A looser track means it can deform more, and it gains a slight grip buff based on the track links wrapping around an obstacle.", 90);
        scene.idle(85);

        sweepTension(builder, scene, rig, controllers, 60, 100, lowerRun, upperRun, lever);
        caption(scene, "Bits 'n' Tracks tracks also have the special feature of having a determined amount of length given to them based on their tension", 90);

        scene.addInstruction(ponder -> rig.mark());
        ElementLink<WorldSectionElement> log = scene.world().showIndependentSection(util.select().fromTo(1, 1, 1, 9, 1, 1), Direction.DOWN);
        int logIndex = rig.obstacle(log, 1.0, 1.0, 1.0);
        scene.addInstruction(ponder -> rig.placeObstacle(ponder, logIndex, -3.0));
        scene.idle(200);
        drive(scene, util, rig, vehicle, sprockets, wheels, rollers, false);
        scene.idle(50);
        scene.markAsFinished();
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
                               ItemStack lever) {
        scene.overlay().showControls(util.vector().blockSurface(wheel, face), pointing, 8).leftClick().withItem(lever);
        scene.idle(6);
        scene.world().restoreBlocks(util.select().position(wheel));
        scene.effects().indicateSuccess(wheel);
        scene.overlay().showOutline(PonderPalette.GREEN, wheel, util.select().position(wheel), 12);
        scene.idle(14);
    }

    private static void placePiece(CreateSceneBuilder scene, BntTankRig rig, int index, ElementLink<WorldSectionElement> link) {
        rig.piece(index, link);
        scene.addInstruction(ponder -> rig.placePiece(ponder, index));
    }

    private static void placeSteps(CreateSceneBuilder scene, SceneBuildingUtil util, BntTankRig rig) {
        Selection step = util.select().fromTo(1, 1, 0, 9, 1, 0);
        scene.addInstruction(ponder -> rig.mark());
        for (int i = 0; i < 5; i++) {
            ElementLink<WorldSectionElement> link = scene.world().showIndependentSection(step, Direction.DOWN);
            int index = rig.obstacle(link, 0.0, 0.5, 0.5);
            double front = -2.0 - 4.0 * i;
            scene.addInstruction(ponder -> rig.placeObstacle(ponder, index, front));
            scene.idle(2);
        }
    }

    private static void drive(CreateSceneBuilder scene, SceneBuildingUtil util, BntTankRig rig, ElementLink<WorldSectionElement> vehicle,
                              Selection sprockets, Selection wheels, Selection rollers, boolean forward) {
        List<BlockPos> powered = List.of(util.grid().at(4, 4, 3), util.grid().at(4, 4, 2), util.grid().at(4, 4, 4),
            util.grid().at(3, 2, 9), util.grid().at(7, 2, 9));
        BlockPos leftShift = util.grid().at(3, 3, 9);
        BlockPos rightShift = util.grid().at(7, 3, 9);
        scene.addInstruction(ponder -> {
            PonderLevel level = ponder.getWorld();
            for (BlockPos pos : powered) {
                level.setBlockAndUpdate(pos, flag(level.getBlockState(pos), "powered", forward));
            }
            level.setBlockAndUpdate(leftShift, flag(level.getBlockState(leftShift), "left_powered", forward));
            level.setBlockAndUpdate(rightShift, flag(level.getBlockState(rightShift), "right_powered", forward));
            WorldSectionElement hull = ponder.resolve(vehicle);
            if (hull != null) {
                hull.queueRedraw();
            }
        });
        float rollerRpm = (float)(SPROCKET_RPM * CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())
            / CogwheelSizeHelper.getChainRadius(BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get()));
        scene.world().setKineticSpeed(sprockets, forward ? SPROCKET_RPM : 0.0F);
        scene.world().setKineticSpeed(wheels, forward ? WHEEL_RPM : 0.0F);
        scene.world().setKineticSpeed(rollers, forward ? rollerRpm : 0.0F);
        scene.addInstruction(ponder -> rig.drive(forward ? BntTankRig.BELT_SPEED : 0.0));
    }

    private static PonderInstruction fall(ItemEntity[] item, double floor) {
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
                if (entity == null || entity.getY() <= floor) {
                    return;
                }
                speed = (speed + 0.04) * 0.98;
                entity.setPos(entity.getX(), Math.max(floor, entity.getY() - speed), entity.getZ());
            }
        };
    }

    private static void withoutRollers(CompoundTag tag) {
        CompoundTag chain = tag.getCompound("Chain");
        int count = chain.getInt("cogwheel_pos_count");
        List<CompoundTag> kept = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompoundTag node = chain.getCompound("cogwheel_pos_" + i);
            chain.remove("cogwheel_pos_" + i);
            if (!node.getBoolean("IsLarge") || node.getBoolean("OffsetForSmallCogwheel")) {
                kept.add(node);
            }
        }
        for (int i = 0; i < kept.size(); i++) {
            chain.put("cogwheel_pos_" + i, kept.get(i));
        }
        chain.putInt("cogwheel_pos_count", kept.size());
        chain.remove("BNT_PathOrder");
        chain.remove("BNT_PathSides");
    }

    private static PonderInstruction placementHint(BlockPos pos, int ticks) {
        return new TickingInstruction(false, ticks) {
            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                PonderLevel level = ponder.getWorld();
                BlockState state = level.getBlockState(pos);
                Vec3 corner = Vec3.atLowerCornerOf(pos);
                int[] edge = {0};
                state.getShape(level, pos).forAllEdges((x1, y1, z1, x2, y2, z2) -> ponder.getOutliner()
                    .showLine("placement_" + pos + "_" + edge[0]++, corner.add(x1, y1, z1), corner.add(x2, y2, z2))
                    .colored(PLACEMENT_GREEN)
                    .lineWidth(1.0F / 16.0F));
                Vec3 top = Vec3.atCenterOf(pos).add(0.0, BntPhysicsTuning.getLargeTrackRadius(), 0.0);
                double sprocketTop = BntPhysicsTuning.getMediumTrackRadius();
                dust(level, Vec3.atCenterOf(pos.north(5)).add(0.0, sprocketTop, 0.0), top);
                dust(level, top, Vec3.atCenterOf(pos.south(2)).add(0.0, sprocketTop, 0.0));
            }
        };
    }

    private static void dust(PonderLevel level, Vec3 from, Vec3 to) {
        Vec3 along = to.subtract(from);
        double length = along.length();
        Vec3 step = along.normalize();
        for (double t = 0.0; t <= length; t += 0.25) {
            if (level.random.nextFloat() < 0.1F) {
                Vec3 at = from.add(step.scale(t));
                level.addParticle(PLACEMENT_DUST, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void sweepTension(SceneBuilder builder, CreateSceneBuilder scene, BntTankRig rig, Selection controllers,
                                     int from, int to, Vec3 labelAt, Vec3 leverAt, ItemStack lever) {
        int direction = from > to ? -10 : 10;
        int count = Math.abs(to - from) / 10 + 1;
        List<TextWindowElement> labels = new ArrayList<>();
        int[] durations = new int[count];
        for (int i = 0; i < count; i++) {
            labels.add(BntFlangedCogwheelScenes.percentLabel(builder, from + i * direction, labelAt));
            durations[i] = i == 0 ? 10 : i == count - 1 ? 30 : 8;
        }
        scene.addInstruction(new BntLabelSequenceInstruction(labels, durations));
        InputElementBuilder hint = scene.overlay().showControls(leverAt, Pointing.DOWN, 8 * count).rightClick().withItem(lever);
        if (direction < 0) {
            hint.whileSneaking();
        }
        scene.idle(10);
        for (int i = 1; i < count; i++) {
            float tension = (from + i * direction) / 100.0F;
            BntFlangedCogwheelScenes.tension(scene, controllers, tension);
            scene.addInstruction(ponder -> rig.tension(tension));
            scene.idle(8);
        }
        scene.idle(22);
    }

    private static BlockState flag(BlockState state, String name, boolean value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property instanceof BooleanProperty flag ? state.setValue(flag, value) : state;
    }

    private static ItemStack honeyGlue() {
        Item glue = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("simulated", "honey_glue"));
        return glue == Items.AIR ? AllItems.SUPER_GLUE.asStack() : new ItemStack(glue);
    }

    private static void pullLever(BlockEntity assembler) {
        try {
            assembler.getClass().getMethod("clientFlickLeverTo", boolean.class).invoke(assembler, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
