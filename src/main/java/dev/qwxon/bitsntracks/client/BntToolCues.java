package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.interaction.BntToolSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntToolCues {
    public static final float MODE = 1.0F;
    public static final float AXIS = 1.2F;
    public static final float SCOPE = 0.85F;
    private static final float STEP_VOLUME = 0.5F;
    private static final float CLICK_VOLUME = 0.25F;
    private static final float SELECT_PITCH = 1.3F;
    private static final float DESELECT_PITCH = 1.0F;
    private static final float ARM_PITCH = 0.7F;

    private BntToolCues() {
    }

    public static void navigate(float pitch) {
        play(BntToolSounds.step(), pitch, STEP_VOLUME);
    }

    public static void select(boolean selected) {
        play(BntToolSounds.click(), selected ? SELECT_PITCH : DESELECT_PITCH, CLICK_VOLUME);
    }

    public static void arm() {
        play(BntToolSounds.click(), ARM_PITCH, CLICK_VOLUME);
    }

    public static void refuse() {
        play(BntToolSounds.deny(), BntToolSounds.REFUSE_PITCH, BntToolSounds.REFUSE_VOLUME);
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
