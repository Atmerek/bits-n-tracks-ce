package dev.qwxon.bitsntracks.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntOverspeedEffects;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntWideSide;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainStops;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import java.util.List;
import java.util.Locale;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class BntFlangedCogwheelScenes {
    private static final float MEDIUM_SPEED = 30.0F;
    private static final float FAST_SPEED = 100.0F;
    private static final float INTRODUCTION_YAW = 235.0F;
    private static final int INTRODUCTION_PLATE = 13;
    private static final int[] INTRODUCTION_COLUMNS = {1, 4, 6, 8, 10, 12};
    private static final int WOODEN_ROW = 8;
    private static final int INDUSTRIAL_ROW = 4;
    private static final int POWERING_PLATE = 13;
    private static final int LABEL_TICKS = 90;
    private static final int LABEL_DELAY = 10;
    private static final int SAVED_TICKS = 55;
    private static final float SPEED_LIMIT = 256.0F;

    private BntFlangedCogwheelScenes() {
    }

    public static void introduction(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("flanged_cogwheel_introduction", "Introduction to Physical Flanged Cogwheels");
        scene.configureBasePlate(0, 0, INTRODUCTION_PLATE);
        scene.scaleSceneView(0.65F);
        scene.addInstruction(startFacing(INTRODUCTION_YAW));

        for (int pair = 0; pair < INTRODUCTION_COLUMNS.length; pair += 2) {
            hideTread(scene, util.select().position(INTRODUCTION_COLUMNS[pair], 1, WOODEN_ROW));
            hideTread(scene, util.select().position(INTRODUCTION_COLUMNS[pair], 1, INDUSTRIAL_ROW));
        }
        scene.world().modifyBlocks(util.select().fromTo(0, 1, INDUSTRIAL_ROW, INTRODUCTION_PLATE - 1, 1, INDUSTRIAL_ROW),
            state -> withWideSide(state, BntWideSide.NONE), false);

        scene.showBasePlate();
        scene.idle(20);
        caption(scene, "Physical Flanged Cogwheels are the heart of Bits 'n' Tracks");
        scene.addKeyframe();

        for (int i = INTRODUCTION_COLUMNS.length - 1; i >= 0; i--) {
            scene.world().showSection(util.select().position(INTRODUCTION_COLUMNS[i], 1, WOODEN_ROW), Direction.DOWN);
            scene.idle(5);
        }
        scene.idle(10);
        caption(scene, "They come in 6 sizes...");

        List<String> sizes = List.of("Large", "Medium-Large", "Medium", "Small", "Intermediate", "Tiny");
        for (int i = 0; i < sizes.size(); i++) {
            label(scene, sizes.get(i), util.vector().topOf(INTRODUCTION_COLUMNS[i], 1, WOODEN_ROW));
        }
        scene.idle(70);

        caption(scene, "... and 2 variants.");
        scene.addKeyframe();

        for (int i = INTRODUCTION_COLUMNS.length - 1; i >= 0; i--) {
            scene.world().showSection(util.select().position(INTRODUCTION_COLUMNS[i], 1, INDUSTRIAL_ROW), Direction.DOWN);
            scene.idle(5);
        }
        scene.idle(10);

        List<ItemStack> industrial = List.of(
            BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.MEDIUM_LARGE_INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.INTERMEDIATE_INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.asStack());
        for (int i = industrial.size() - 1; i >= 0; i--) {
            BlockPos lower = util.grid().at(INTRODUCTION_COLUMNS[i], 1, INDUSTRIAL_ROW);
            BlockPos upper = lower.above();
            scene.overlay().showControls(util.vector().topOf(lower), Pointing.DOWN, 20).rightClick().withItem(industrial.get(i));
            scene.idle(15);
            scene.world().modifyBlock(lower, state -> withWideSide(state, BntWideSide.POSITIVE), false);
            scene.world().modifyBlock(upper, state -> withWideSide(state, BntWideSide.NEGATIVE), false);
            scene.world().showSection(util.select().position(upper), Direction.DOWN);
            scene.idle(10);
        }
        caption(scene, "Two cogwheels of the same size, placed end-to-end, create their wide version",
            util.vector().topOf(INTRODUCTION_COLUMNS[0], 2, INDUSTRIAL_ROW));
        scene.addKeyframe();

        ItemStack mechanicalBelt = AllItems.BELT_CONNECTOR.asStack();
        ItemStack industrialBelt = BitsNTracksItems.INDUSTRIAL_BELT.asStack();
        for (int pair = 0; pair < INTRODUCTION_COLUMNS.length; pair += 2) {
            int first = INTRODUCTION_COLUMNS[pair];
            int second = INTRODUCTION_COLUMNS[pair + 1];
            beltHints(scene, util.vector().topOf(first, 1, WOODEN_ROW), util.vector().topOf(second, 1, WOODEN_ROW), mechanicalBelt);
            beltHints(scene, util.vector().topOf(first, 2, INDUSTRIAL_ROW), util.vector().topOf(second, 2, INDUSTRIAL_ROW), industrialBelt);
        }
        caption(scene, "By right-clicking on a set number of cogwheels...");

        for (int row : new int[]{WOODEN_ROW, INDUSTRIAL_ROW}) {
            for (int pair = 0; pair < INTRODUCTION_COLUMNS.length; pair += 2) {
                scene.world().restoreBlocks(util.select().fromTo(INTRODUCTION_COLUMNS[pair], 1, row, INTRODUCTION_COLUMNS[pair + 1], 1, row));
                scene.idle(10);
            }
        }
        scene.idle(10);
        caption(scene, "You can create a track!");
        scene.markAsFinished();
    }

    public static void powering(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("flanged_cogwheel_powering", "Powering Tracks");
        scene.configureBasePlate(0, 0, POWERING_PLATE);
        scene.scaleSceneView(0.7F);
        scene.setSceneOffsetY(-1.0F);

        Wheel large = new Wheel(util.grid().at(2, 1, 6), CogwheelSize.LARGE);
        Wheel mediumLarge = new Wheel(util.grid().at(4, 1, 2), CogwheelSize.MEDIUM_LARGE);
        Wheel medium = new Wheel(util.grid().at(8, 1, 2), CogwheelSize.MEDIUM);
        Wheel small = new Wheel(util.grid().at(10, 1, 6), CogwheelSize.SMALL);
        Wheel intermediate = new Wheel(util.grid().at(8, 1, 10), CogwheelSize.INTERMEDIATE);
        Wheel tiny = new Wheel(util.grid().at(4, 1, 10), CogwheelSize.TINY);
        List<Wheel> track = List.of(large, mediumLarge, medium, small, intermediate, tiny);
        Selection largeDrive = util.select().fromTo(2, 2, 6, 2, 3, 6);
        Selection tinyDrive = util.select().fromTo(4, 2, 10, 4, 3, 10);
        Vec3 largeGauge = util.vector().blockSurface(util.grid().at(2, 2, 6), Direction.NORTH);
        Vec3 tinyGauge = util.vector().blockSurface(util.grid().at(4, 2, 10), Direction.NORTH);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 0, POWERING_PLATE - 1, 1, POWERING_PLATE - 1), Direction.DOWN);
        scene.idle(20);
        caption(scene, "A track runs over all of its cogwheels at the same speed");
        scene.addKeyframe();

        scene.world().showSection(largeDrive, Direction.DOWN);
        scene.idle(20);
        drive(scene, util, largeDrive, large, 16.0F, track);
        note(scene, "So the smaller a cogwheel is, the faster it turns: a tiny cogwheel turns four times as fast as a large one", LABEL_TICKS);
        rpmLabels(scene, util, large, 16.0F, largeGauge, track, LABEL_TICKS);
        scene.idle(10);
        scene.addKeyframe();

        note(scene, "A bigger cogwheel has a bigger stress impact...", LABEL_TICKS);
        for (Wheel wheel : track) {
            label(scene, LABEL_TICKS, PonderPalette.WHITE, util.vector().topOf(wheel.pos()), impactText(wheel.impact()));
        }
        scene.idle(LABEL_TICKS + 10);
        note(scene, "... but it turns slower, so on a track every cogwheel costs the same stress", LABEL_TICKS);
        for (Wheel wheel : track) {
            label(scene, LABEL_TICKS, PonderPalette.GREEN, util.vector().topOf(wheel.pos()), "%s su",
                Math.round(wheel.impact() * wheel.speed(large, 16.0F)));
        }
        scene.idle(LABEL_TICKS + 10);
        scene.addKeyframe();

        drive(scene, util, largeDrive, large, 0.0F, track);
        scene.world().hideSection(largeDrive, Direction.UP);
        scene.idle(15);
        scene.world().showSection(tinyDrive, Direction.DOWN);
        scene.idle(20);
        drive(scene, util, tinyDrive, tiny, 16.0F, track);
        note(scene, "Power can go into any cogwheel of the track, and driving a small one gears the bigger ones down", LABEL_TICKS);
        rpmLabels(scene, util, tiny, 16.0F, tinyGauge, track, LABEL_TICKS);
        scene.idle(10);
        scene.addKeyframe();

        drive(scene, util, tinyDrive, tiny, 0.0F, track);
        scene.world().hideSection(tinyDrive, Direction.UP);
        scene.idle(15);
        scene.world().showSection(largeDrive, Direction.DOWN);
        scene.idle(20);
        drive(scene, util, largeDrive, large, 64.0F, track);
        note(scene, "The speed limit counts for the fastest cogwheel, not for the one you power", LABEL_TICKS);
        rpmLabels(scene, util, large, 64.0F, largeGauge, track, LABEL_TICKS);
        scene.idle(10);
        scene.addKeyframe();

        drive(scene, util, largeDrive, large, 128.0F, track);
        overspeed(scene, large, 128.0F, track, SAVED_TICKS);
        note(scene, "Past the speed limit, a cogwheel throws sparks and smokes", SAVED_TICKS);
        rpmLabels(scene, util, large, 128.0F, largeGauge, track, SAVED_TICKS);
        drive(scene, util, largeDrive, large, 64.0F, track);
        note(scene, "Slow it down within three seconds, and nothing breaks", LABEL_TICKS);
        rpmLabels(scene, util, large, 64.0F, largeGauge, track, LABEL_TICKS);
        scene.idle(10);
        scene.addKeyframe();

        int grace = (int)BntChainStops.OVERSPEED_GRACE;
        drive(scene, util, largeDrive, large, 128.0F, track);
        overspeed(scene, large, 128.0F, track, grace);
        note(scene, "If it keeps running too fast...", grace);
        rpmLabels(scene, util, large, 128.0F, largeGauge, track, grace);
        scene.world().destroyBlock(tiny.pos());
        hideTread(scene, util.select().position(large.pos()));
        for (Wheel wheel : track) {
            if (wheel != large && wheel != tiny) {
                scene.world().setKineticSpeed(util.select().position(wheel.pos()), 0.0F);
            }
        }
        scene.idle(20);
        caption(scene, "... the fastest cogwheel breaks, and the track comes off");
        scene.markAsFinished();
    }

    private static void rpmLabels(CreateSceneBuilder scene, SceneBuildingUtil util, Wheel driven, float rpm, Vec3 gauge,
                                  List<Wheel> track, int ticks) {
        rpmLabel(scene, rpm, gauge, ticks);
        scene.idle(LABEL_DELAY);
        for (Wheel wheel : track) {
            if (wheel != driven) {
                rpmLabel(scene, wheel.speed(driven, rpm), util.vector().topOf(wheel.pos()), ticks - LABEL_DELAY);
            }
        }
        scene.idle(ticks - LABEL_DELAY);
    }

    private static void overspeed(CreateSceneBuilder scene, Wheel driven, float rpm, List<Wheel> track, int ticks) {
        List<BlockPos> sparking = track.stream()
            .filter(wheel -> Math.abs(wheel.speed(driven, rpm)) > SPEED_LIMIT)
            .map(Wheel::pos)
            .toList();
        scene.addInstruction(new Overspeed(sparking, ticks));
    }

    private static String impactText(double impact) {
        return impact == Math.rint(impact) ? (long)impact + "x" : String.format(Locale.ROOT, "%.1fx", impact);
    }

    public static void tracks(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("track_introduction", "Introduction to Tracks");
        scene.configureBasePlate(0, 0, 9);

        BlockPos left = util.grid().at(2, 2, 5);
        BlockPos right = util.grid().at(6, 2, 5);
        Selection controller = util.select().position(left);
        Selection wheels = util.select().position(left).add(util.select().position(right));
        Selection partners = util.select().position(left.north()).add(util.select().position(right.north()));
        List<Vec3> loop = List.of(rim(util, left), rim(util, right), rim(util, left));
        Vec3 run = util.vector().of(4.5, 3.6, 5.5);

        hideTread(scene, controller);
        hideTread(scene, util.select().position(left.north()));
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 6, 7, 2, 6), Direction.DOWN);
        scene.idle(20);
        caption(scene, "Belts put the \"Tracks\" in Bits 'n' Tracks's name");
        scene.addKeyframe();

        scene.world().showSection(controller, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(right), Direction.DOWN);
        scene.idle(20);
        caption(scene, "You can place a track on as many cogwheels as you want, starting from 2");
        scene.addKeyframe();

        placeBelt(scene, loop, AllItems.BELT_CONNECTOR.asStack());
        scene.world().restoreBlocks(controller);
        scene.idle(20);
        caption(scene, "Belts also come in different styles, with 3 variants...");
        scene.addKeyframe();

        swapBelt(scene, controller, loop, run, BitsNTracksItems.INDUSTRIAL_BELT.asStack(),
            BntCogwheelChainTypes.INDUSTRIAL_BELT_CHAIN.getId());
        scene.idle(20);
        swapBelt(scene, controller, loop, run, BitsNTracksItems.TANK_TREAD.asStack(),
            BntCogwheelChainTypes.TANK_TREAD_CHAIN.getId());
        scene.idle(20);
        caption(scene, "... and 2 sizes");
        scene.addKeyframe();

        ItemStack cogwheel = BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.asStack();
        scene.overlay().showControls(util.vector().blockSurface(left, Direction.NORTH), Pointing.DOWN, 30).rightClick().withItem(cogwheel);
        scene.overlay().showControls(util.vector().blockSurface(right, Direction.NORTH), Pointing.DOWN, 30).rightClick().withItem(cogwheel);
        scene.idle(15);
        scene.world().modifyBlocks(wheels, state -> withWideSide(state, BntWideSide.NEGATIVE), false);
        scene.world().modifyBlocks(partners, state -> withWideSide(state, BntWideSide.POSITIVE), false);
        scene.world().showSection(partners, Direction.SOUTH);
        scene.idle(25);
        scene.markAsFinished();
    }

    public static void grousers(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("track_grousers", "Fitting Grousers to Tracks");
        scene.configureBasePlate(0, 0, 9);

        BlockPos left = util.grid().at(2, 2, 5);
        BlockPos right = util.grid().at(6, 2, 5);
        Selection controller = util.select().position(left);
        Selection wheels = util.select().position(left).add(util.select().position(right));
        Vec3 run = util.vector().of(4.5, 3.6, 5.5);

        setChainType(scene, controller, BitsNTracksItems.INDUSTRIAL_BELT.asStack(), BntCogwheelChainTypes.INDUSTRIAL_BELT_CHAIN.getId());
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 6, 7, 2, 6), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(wheels, Direction.DOWN);
        scene.idle(20);
        caption(scene, "On snow, ice or mud, a belt slips easily");
        scene.addKeyframe();

        scene.overlay().showControls(run, Pointing.DOWN, 30).rightClick().withItem(BitsNTracksItems.GROUSER.asStack());
        scene.idle(15);
        setGrousers(scene, List.of(left, right), 1);
        scene.idle(20);
        caption(scene, "Right-click it with grousers to fit them, using one grouser for every block of track", run);
        caption(scene, "Grousers dig into soft and slippery ground, and grip a little worse on hard ground");
        scene.addKeyframe();

        scene.overlay().showControls(run, Pointing.DOWN, 30).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(15);
        setGrousers(scene, List.of(left, right), 0);
        scene.idle(20);
        caption(scene, "A wrench takes the grousers off again, and a second use removes the track", run);
        scene.markAsFinished();
    }

    private static void setGrousers(CreateSceneBuilder scene, List<BlockPos> wheels, int grousers) {
        for (BlockPos wheel : wheels) {
            scene.world().modifyBlockEntity(wheel, KineticBlockEntity.class, be -> {
                if (be instanceof KineticBlockEntityPhysicsAccess access) {
                    access.bnt$setGrousers(grousers);
                }
            });
        }
    }

    private static void caption(CreateSceneBuilder scene, String text) {
        scene.overlay().showText(70).text(text);
        scene.idle(80);
    }

    private static void caption(CreateSceneBuilder scene, String text, Vec3 target) {
        scene.overlay().showText(70).text(text).placeNearTarget().pointAt(target);
        scene.idle(80);
    }

    private static void label(CreateSceneBuilder scene, String text, Vec3 target) {
        scene.overlay().showText(60).text(text).placeNearTarget().pointAt(target);
    }

    private static void note(CreateSceneBuilder scene, String text, int ticks) {
        scene.overlay().showText(ticks).text(text);
    }

    private static void label(CreateSceneBuilder scene, int ticks, PonderPalette colour, Vec3 target, String text, Object... args) {
        scene.overlay().showText(ticks).colored(colour).text(text, args).placeNearTarget().pointAt(target);
    }

    private static void rpmLabel(CreateSceneBuilder scene, float rpm, Vec3 target, int ticks) {
        String value = rpm == Math.round(rpm) ? Integer.toString(Math.round(rpm)) : String.format(Locale.ROOT, "%.1f", rpm);
        label(scene, ticks, speedColour(rpm), target, "%s RPM", value);
    }

    static TextWindowElement percentLabel(SceneBuilder builder, int percent, Vec3 target) {
        TextWindowElement label = new TextWindowElement();
        label.builder(builder.getScene())
            .colored(percent > 50 ? PonderPalette.GREEN : PonderPalette.RED)
            .text("%s", percent + "%")
            .placeNearTarget()
            .pointAt(target);
        return label;
    }

    private static PonderPalette speedColour(float rpm) {
        return rpm >= FAST_SPEED ? PonderPalette.FAST : rpm >= MEDIUM_SPEED ? PonderPalette.MEDIUM : PonderPalette.SLOW;
    }

    private static void drive(CreateSceneBuilder scene, SceneBuildingUtil util, Selection train, Wheel driven, float rpm,
                              List<Wheel> belt) {
        scene.world().setKineticSpeed(train, rpm);
        for (Wheel wheel : belt) {
            scene.world().setKineticSpeed(util.select().position(wheel.pos()), wheel.speed(driven, rpm));
        }
    }

    private static void beltHints(CreateSceneBuilder scene, Vec3 first, Vec3 second, ItemStack belt) {
        scene.overlay().showControls(first, Pointing.DOWN, 50).rightClick().withItem(belt);
        scene.overlay().showControls(second, Pointing.DOWN, 50).rightClick().withItem(belt);
        scene.idle(20);
    }

    private static Vec3 rim(SceneBuildingUtil util, BlockPos pos) {
        return util.vector().centerOf(pos).add(0.0, 1.2, 0.0);
    }

    private static void placeBelt(CreateSceneBuilder scene, List<Vec3> clicks, ItemStack belt) {
        for (Vec3 click : clicks) {
            scene.overlay().showControls(click, Pointing.DOWN, 20).rightClick().withItem(belt);
            scene.idle(12);
        }
        scene.idle(5);
    }

    private static void swapBelt(CreateSceneBuilder scene, Selection controller, List<Vec3> clicks, Vec3 run, ItemStack belt,
                                 ResourceLocation type) {
        scene.overlay().showControls(run, Pointing.DOWN, 25).rightClick().whileSneaking().withItem(AllItems.WRENCH.asStack());
        scene.idle(15);
        hideTread(scene, controller);
        scene.idle(15);
        placeBelt(scene, clicks, belt);
        scene.world().restoreBlocks(controller);
        setChainType(scene, controller, belt, type);
    }

    private static void setChainType(CreateSceneBuilder scene, Selection controller, ItemStack belt, ResourceLocation type) {
        scene.world().modifyBlockEntityNBT(controller, KineticBlockEntity.class, tag -> {
            CompoundTag chain = tag.getCompound("Chain");
            chain.putString("chain_type", type.toString());
            chain.putString("returned_item", BuiltInRegistries.ITEM.getKey(belt.getItem()).toString());
        }, true);
    }

    static PonderInstruction startFacing(float yaw) {
        return new PonderInstruction() {
            @Override
            public boolean isComplete() {
                return true;
            }

            @Override
            public void onScheduled(PonderScene ponder) {
                ponder.getTransform().yRotation.startWithValue(yaw);
            }

            @Override
            public void tick(PonderScene ponder) {
            }
        };
    }

    private static BlockState withWideSide(BlockState state, BntWideSide side) {
        return state.hasProperty(BntCogwheelPairing.WIDE) ? state.setValue(BntCogwheelPairing.WIDE, side) : state;
    }

    static void hideTread(CreateSceneBuilder scene, Selection selection) {
        scene.world().modifyBlockEntityNBT(selection, KineticBlockEntity.class, tag -> tag.remove("Chain"), true);
    }

    private record Wheel(BlockPos pos, CogwheelSize size) {
        float speed(Wheel driven, float rpm) {
            return (float)(rpm * driven.size.chainRadius() / this.size.chainRadius());
        }

        double impact() {
            return BntPhysicsTuning.defaultStressImpact(this.size);
        }
    }

    private static final class Overspeed extends TickingInstruction {
        private final List<BlockPos> wheels;

        private Overspeed(List<BlockPos> wheels, int ticks) {
            super(false, ticks);
            this.wheels = wheels;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            PonderLevel world = scene.getWorld();
            double heat = BntOverspeedEffects.heat(this.totalTicks - this.remainingTicks);
            for (BlockPos pos : this.wheels) {
                BlockState state = world.getBlockState(pos);
                if (state.hasProperty(BlockStateProperties.AXIS) && world.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
                    BntOverspeedEffects.spray(world, null, state, Vec3.atCenterOf(pos), kinetic.getSpeed(), heat);
                }
            }
        }
    }
}
