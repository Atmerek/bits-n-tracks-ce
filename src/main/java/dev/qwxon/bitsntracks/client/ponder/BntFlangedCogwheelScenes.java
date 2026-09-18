package dev.qwxon.bitsntracks.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntWideSide;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BntFlangedCogwheelScenes {
    private static final float MEDIUM_SPEED = 30.0F;
    private static final float FAST_SPEED = 100.0F;
    private static final float INTRODUCTION_YAW = 325.0F;

    private BntFlangedCogwheelScenes() {
    }

    public static void introduction(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("flanged_cogwheel_introduction", "Introduction to Physical Flanged Cogwheels");
        scene.configureBasePlate(0, 0, 10);
        scene.scaleSceneView(0.8F);
        scene.addInstruction(startFacing(INTRODUCTION_YAW));

        List<BlockPos> beltStarts = List.of(
            util.grid().at(2, 1, 6), util.grid().at(6, 1, 6), util.grid().at(2, 1, 3), util.grid().at(6, 1, 3));
        for (BlockPos start : beltStarts) {
            hideTread(scene, util.select().position(start));
        }
        scene.world().modifyBlocks(util.select().fromTo(2, 1, 3, 8, 1, 3), state -> withWideSide(state, BntWideSide.NONE), false);

        scene.showBasePlate();
        scene.idle(20);
        caption(scene, "Physical Flanged Cogwheels are the heart of Bits 'n' Tracks");
        scene.addKeyframe();

        for (int x = 8; x >= 2; x -= 2) {
            scene.world().showSection(util.select().position(x, 1, 6), Direction.DOWN);
            scene.idle(5);
        }
        scene.idle(10);
        caption(scene, "They come in 4 sizes...");

        label(scene, "Large", util.vector().topOf(2, 1, 6));
        label(scene, "Medium", util.vector().topOf(4, 1, 6));
        label(scene, "Small", util.vector().topOf(6, 1, 6));
        label(scene, "Tiny", util.vector().topOf(8, 1, 6));
        scene.idle(70);

        caption(scene, "... and 2 variants.");
        scene.addKeyframe();

        for (int x = 8; x >= 2; x -= 2) {
            scene.world().showSection(util.select().position(x, 1, 3), Direction.DOWN);
            scene.idle(5);
        }
        scene.idle(10);

        List<ItemStack> industrial = List.of(
            BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.asStack(),
            BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.asStack());
        for (int i = 0; i < industrial.size(); i++) {
            BlockPos lower = util.grid().at(8 - i * 2, 1, 3);
            BlockPos upper = lower.above();
            scene.overlay().showControls(util.vector().topOf(lower), Pointing.DOWN, 20).rightClick().withItem(industrial.get(i));
            scene.idle(15);
            scene.world().modifyBlock(lower, state -> withWideSide(state, BntWideSide.POSITIVE), false);
            scene.world().modifyBlock(upper, state -> withWideSide(state, BntWideSide.NEGATIVE), false);
            scene.world().showSection(util.select().position(upper), Direction.DOWN);
            scene.idle(10);
        }
        caption(scene, "Two cogwheels of the same size, placed end-to-end, create their wide version",
            util.vector().topOf(2, 2, 3));
        scene.addKeyframe();

        ItemStack mechanicalBelt = AllItems.BELT_CONNECTOR.asStack();
        ItemStack industrialBelt = BitsNTracksItems.INDUSTRIAL_BELT.asStack();
        beltHints(scene, util.vector().topOf(2, 1, 6), util.vector().topOf(4, 1, 6), mechanicalBelt);
        beltHints(scene, util.vector().topOf(6, 1, 6), util.vector().topOf(8, 1, 6), mechanicalBelt);
        beltHints(scene, util.vector().topOf(2, 2, 3), util.vector().topOf(4, 2, 3), industrialBelt);
        beltHints(scene, util.vector().topOf(6, 2, 3), util.vector().topOf(8, 2, 3), industrialBelt);
        caption(scene, "By right-clicking on a set number of cogwheels...");

        for (BlockPos start : beltStarts) {
            scene.world().restoreBlocks(util.select().fromTo(start, start.east(2)));
            scene.idle(10);
        }
        scene.idle(10);
        caption(scene, "You can create a track!");
        scene.markAsFinished();
    }

    public static void powering(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("flanged_cogwheel_powering", "Powering and managing Physical Flanged Cogwheels");
        scene.configureBasePlate(1, 1, 10);
        scene.scaleSceneView(0.75F);

        Wheel tiny = new Wheel(util.grid().at(7, 1, 7), BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get());
        Wheel small = new Wheel(util.grid().at(7, 1, 4), BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get());
        Wheel medium = new Wheel(util.grid().at(4, 1, 7), BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get());
        Wheel large = new Wheel(util.grid().at(4, 1, 4), BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get());
        List<Wheel> bySize = List.of(tiny, small, medium, large);
        List<Wheel> aroundLoop = List.of(tiny, small, large, medium);
        Selection tinyDrive = util.select().fromTo(11, 0, 7, 11, 2, 7).add(util.select().position(7, 2, 7));
        Selection largeDrive = util.select().fromTo(0, 0, 4, 0, 2, 4).add(util.select().position(4, 2, 4));
        Vec3 tinyGauge = util.vector().blockSurface(util.grid().at(11, 1, 7), Direction.NORTH);
        Vec3 largeGauge = util.vector().blockSurface(util.grid().at(0, 1, 4), Direction.NORTH);

        hideTread(scene, util.select().position(medium.pos()));
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(4, 1, 4, 7, 1, 7), Direction.DOWN);
        scene.idle(20);
        caption(scene, "Before powering your track, you need to be aware of how it behaves");
        caption(scene, "Physical Flanged Cogwheels have distinct rotation ratios, and you will need to keep that in mind when creating a run of track");
        scene.addKeyframe();

        BlockState shaft = AllBlocks.ANDESITE_ENCASED_SHAFT.getDefaultState()
            .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y);
        for (Wheel wheel : bySize) {
            scene.world().setBlock(wheel.pos().below(), shaft, false);
            scene.world().setKineticSpeed(util.select().fromTo(wheel.pos().below(), wheel.pos()), 16.0F);
        }
        scene.idle(20);
        caption(scene, "Each cogwheel size has a different impact on stress. The bigger the cogwheel, the bigger the impact");

        label(scene, "2x", PonderPalette.SLOW, util.vector().topOf(tiny.pos()));
        label(scene, "4x", PonderPalette.GREEN, util.vector().topOf(small.pos()));
        label(scene, "6x", PonderPalette.OUTPUT, util.vector().topOf(medium.pos()));
        label(scene, "8x", PonderPalette.RED, util.vector().topOf(large.pos()));
        scene.idle(70);
        scene.addKeyframe();

        for (Wheel wheel : bySize) {
            scene.world().setKineticSpeed(util.select().fromTo(wheel.pos().below(), wheel.pos()), 0.0F);
            scene.world().restoreBlocks(util.select().position(wheel.pos().below()));
        }
        scene.idle(15);
        ItemStack mechanicalBelt = AllItems.BELT_CONNECTOR.asStack();
        for (Wheel wheel : aroundLoop) {
            scene.overlay().showControls(util.vector().topOf(wheel.pos()), Pointing.DOWN, 20).rightClick().withItem(mechanicalBelt);
            scene.idle(12);
        }
        scene.overlay().showControls(util.vector().topOf(tiny.pos()), Pointing.DOWN, 20).rightClick().withItem(mechanicalBelt);
        scene.idle(15);
        scene.world().restoreBlocks(util.select().position(medium.pos()));
        scene.idle(20);
        caption(scene, "When connected together, the cogwheels will spin relative to the size of the powered cogwheel");
        scene.addKeyframe();

        scene.world().showSection(tinyDrive, Direction.UP);
        scene.idle(20);
        drive(scene, util, tinyDrive, tiny, 16.0F, bySize);
        rpmLabel(scene, 16.0F, tinyGauge);
        scene.idle(40);
        for (Wheel wheel : bySize) {
            rpmLabel(scene, wheel.speed(tiny, 16.0F), util.vector().topOf(wheel.pos()));
        }
        scene.idle(70);
        caption(scene, "And this is where you have to watch out");
        scene.addKeyframe();

        drive(scene, util, tinyDrive, tiny, 0.0F, bySize);
        scene.world().hideSection(tinyDrive, Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(largeDrive, Direction.UP);
        scene.idle(20);
        caption(scene, "Because if you spin a larger gear at a speed too high...");

        drive(scene, util, largeDrive, large, 64.0F, bySize);
        rpmLabel(scene, 64.0F, largeGauge);
        scene.idle(40);
        for (Wheel wheel : bySize) {
            rpmLabel(scene, wheel.speed(large, 64.0F), util.vector().topOf(wheel.pos()));
        }
        scene.idle(70);
        scene.addKeyframe();

        drive(scene, util, largeDrive, large, 256.0F, bySize);
        rpmLabel(scene, 256.0F, largeGauge);
        scene.idle(30);
        for (Wheel wheel : List.of(tiny, small, medium)) {
            scene.world().destroyBlock(wheel.pos());
        }
        scene.idle(20);
        caption(scene, "You can make the smaller cogwheels break!");
        scene.markAsFinished();
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
        caption(scene, "By using the Cog Alignment Lever on a run of track...");
        scene.addKeyframe();

        Vec3 wideRun = run.add(0.0, 0.0, -0.5);
        ItemStack lever = BitsNTracksItems.COG_ALIGNMENT_LEVER.asStack();
        Vec3 lowerRun = util.vector().of(4.5, 1.4, 4.5);
        List<TextWindowElement> labels = new ArrayList<>();
        int[] durations = new int[19];
        for (int i = 0; i < durations.length; i++) {
            int percent = Math.abs(i - 9) * 10 + 10;
            labels.add(percentLabel(builder, percent, lowerRun));
            durations[i] = i == 0 ? 10 : i == 9 ? 38 : i == 18 ? 33 : 8;
        }
        scene.addInstruction(new BntLabelSequenceInstruction(labels, durations));
        scene.overlay().showControls(wideRun, Pointing.DOWN, 95).rightClick().whileSneaking().withItem(lever);
        scene.idle(10);
        for (int step = 9; step >= 1; step--) {
            tension(scene, wheels, step / 10.0F);
            scene.idle(8);
        }
        scene.idle(30);
        scene.overlay().showControls(wideRun, Pointing.DOWN, 80).rightClick().withItem(lever);
        for (int step = 2; step <= 10; step++) {
            tension(scene, wheels, step / 10.0F);
            scene.idle(8);
        }
        scene.idle(25);
        caption(scene, "You can change the tension of the system");
        caption(scene, "This directly influences the track's behaviour, ranging from suspension implications to grip and terrain adaptability");
        scene.markAsFinished();
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

    private static void label(CreateSceneBuilder scene, String text, PonderPalette colour, Vec3 target) {
        scene.overlay().showText(60).colored(colour).text(text).placeNearTarget().pointAt(target);
    }

    private static void rpmLabel(CreateSceneBuilder scene, float rpm, Vec3 target) {
        String value = rpm == Math.round(rpm) ? Integer.toString(Math.round(rpm)) : String.format(Locale.ROOT, "%.1f", rpm);
        scene.overlay().showText(60).colored(speedColour(rpm)).text("%s RPM", value).placeNearTarget().pointAt(target);
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
        scene.world().modifyBlockEntityNBT(controller, KineticBlockEntity.class, tag -> {
            CompoundTag chain = tag.getCompound("Chain");
            chain.putString("chain_type", type.toString());
            chain.putString("returned_item", BuiltInRegistries.ITEM.getKey(belt.getItem()).toString());
        }, true);
    }

    static void tension(CreateSceneBuilder scene, Selection wheels, float tension) {
        scene.world().modifyBlockEntityNBT(wheels, KineticBlockEntity.class, tag -> tag.putFloat("BntBeltTension", tension), false);
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

    private record Wheel(BlockPos pos, double radius) {
        Wheel(BlockPos pos, Block block) {
            this(pos, CogwheelSizeHelper.getChainRadius(block));
        }

        float speed(Wheel driven, float rpm) {
            return (float)(rpm * driven.radius / radius);
        }
    }
}
