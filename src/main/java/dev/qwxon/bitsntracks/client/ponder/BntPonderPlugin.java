package dev.qwxon.bitsntracks.client.ponder;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class BntPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return BitsNTracks.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        BntPonderScenes.register(helper);
    }
}
