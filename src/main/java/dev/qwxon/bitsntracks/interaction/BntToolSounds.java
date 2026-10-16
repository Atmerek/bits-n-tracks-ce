package dev.qwxon.bitsntracks.interaction;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BntToolSounds {
    private static final float ADJUST_VOLUME = 0.5F;
    private static final float ADJUST_LOW = 0.8F;
    private static final float ADJUST_RANGE = 0.6F;
    private static final float TOGGLE_VOLUME = 0.35F;
    private static final float TOGGLE_ON = 1.4F;
    private static final float TOGGLE_OFF = 1.1F;
    private static final float PHYSICS_ON = 0.9F;
    private static final float PHYSICS_OFF = 0.7F;
    private static final float MOUNT_PITCH = 0.8F;
    private static final float RESET_VOLUME = 0.6F;
    private static final float RESET_PITCH = 0.8F;
    public static final float REFUSE_VOLUME = 0.6F;
    public static final float REFUSE_PITCH = 0.8F;

    private BntToolSounds() {
    }

    public static SoundEvent step() {
        return AllSoundEvents.SCROLL_VALUE.getMainEvent();
    }

    public static SoundEvent click() {
        return SoundEvents.UI_BUTTON_CLICK.value();
    }

    public static SoundEvent deny() {
        return AllSoundEvents.DENY.getMainEvent();
    }

    public static void adjust(Level level, BlockPos pos, float fraction) {
        level.playSound(null, pos, step(), SoundSource.BLOCKS, ADJUST_VOLUME, ADJUST_LOW + ADJUST_RANGE * Mth.clamp(fraction, 0.0F, 1.0F));
    }

    public static void toggle(Level level, BlockPos pos, boolean on) {
        level.playSound(null, pos, click(), SoundSource.BLOCKS, TOGGLE_VOLUME, on ? TOGGLE_ON : TOGGLE_OFF);
    }

    public static void physics(Level level, BlockPos pos, boolean on) {
        level.playSound(null, pos, click(), SoundSource.BLOCKS, TOGGLE_VOLUME, on ? PHYSICS_ON : PHYSICS_OFF);
    }

    public static void mount(Level level, BlockPos pos, boolean on) {
        level.playSound(null, pos, on ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, MOUNT_PITCH);
    }

    public static void reset(Level level, BlockPos pos) {
        level.playSound(null, pos, AllSoundEvents.CONFIRM.getMainEvent(), SoundSource.BLOCKS, RESET_VOLUME, RESET_PITCH);
    }

    public static void refuse(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(deny(), SoundSource.PLAYERS, REFUSE_VOLUME, REFUSE_PITCH);
        }
    }
}
