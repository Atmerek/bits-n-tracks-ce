package dev.qwxon.bitsntracks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@OnlyIn(Dist.CLIENT)
public final class BntShaderHand {
    public enum Pass {
        NONE,
        SOLID,
        TRANSLUCENT
    }

    private static Pass pass = Pass.NONE;
    private static boolean solidDrawn;

    private BntShaderHand() {
    }

    public static void begin(Pass next) {
        pass = next;
    }

    public static void end() {
        pass = Pass.NONE;
    }

    public static Pass pass() {
        return pass;
    }

    public static void markSolidDrawn() {
        solidDrawn = true;
    }

    public static boolean solidDrawn() {
        return solidDrawn;
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        solidDrawn = false;
    }
}
