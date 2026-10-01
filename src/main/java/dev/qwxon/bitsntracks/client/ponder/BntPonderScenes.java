package dev.qwxon.bitsntracks.client.ponder;

import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class BntPonderScenes {
    private BntPonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.getId(),
                BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.getId())
            .addStoryBoard("flanged_cogwheel/introduction", BntFlangedCogwheelScenes::introduction)
            .addStoryBoard("flanged_cogwheel/powering", BntFlangedCogwheelScenes::powering);

        helper.forComponents(
                BitsNTracksItems.TANK_TREAD.getId(),
                BitsNTracksItems.INDUSTRIAL_BELT.getId())
            .addStoryBoard("track/introduction", BntFlangedCogwheelScenes::tracks)
            .addStoryBoard("bob", BntBobScenes::tension);

        helper.forComponents(BitsNTracksItems.COG_ALIGNMENT_LEVER.getId())
            .addStoryBoard("bob", BntBobScenes::collisions)
            .addStoryBoard("bob", BntBobScenes::customizing)
            .addStoryBoard("bob", BntBobScenes::tension)
            .addStoryBoard("bob_obstacle", BntSuspensionScenes::singleArm)
            .addStoryBoard("bob_bogie", BntSuspensionScenes::bogie);

        helper.forComponents(BitsNTracksItems.SUSPENSION_PIECE.getId())
            .addStoryBoard("bob_obstacle", BntSuspensionScenes::singleArm)
            .addStoryBoard("bob_bogie", BntSuspensionScenes::bogie);
    }
}
