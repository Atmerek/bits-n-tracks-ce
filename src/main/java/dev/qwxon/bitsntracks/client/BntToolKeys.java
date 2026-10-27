package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@OnlyIn(Dist.CLIENT)
public final class BntToolKeys {
    private static final String CATEGORY = "key.categories.bits_n_tracks";
    public static final KeyMapping NEXT_MODE = key("next_mode");
    public static final KeyMapping PREVIOUS_MODE = key("previous_mode");
    public static final KeyMapping STEP_UP = key("step_up");
    public static final KeyMapping STEP_DOWN = key("step_down");

    private BntToolKeys() {
    }

    private static KeyMapping key(String name) {
        return new KeyMapping("key.bits_n_tracks." + name, KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(NEXT_MODE);
        event.register(PREVIOUS_MODE);
        event.register(STEP_UP);
        event.register(STEP_DOWN);
    }

    static int presses(KeyMapping key) {
        int count = 0;
        while (key.consumeClick()) {
            count++;
        }
        return count;
    }
}
