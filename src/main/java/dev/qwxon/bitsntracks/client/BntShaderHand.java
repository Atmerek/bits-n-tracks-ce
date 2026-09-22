package dev.qwxon.bitsntracks.client;

import java.lang.reflect.Method;
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
    private static Object irisApi;
    private static Method packInUse;
    private static Method shadowPass;
    private static boolean irisLooked;

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

    public static boolean packInUse() {
        return lookForIris() && askIris(packInUse);
    }

    public static boolean shadowPass() {
        return lookForIris() && askIris(shadowPass);
    }

    private static boolean lookForIris() {
        if (!irisLooked) {
            irisLooked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                packInUse = api.getMethod("isShaderPackInUse");
                shadowPass = api.getMethod("isRenderingShadowPass");
            } catch (ReflectiveOperationException | LinkageError e) {
                irisApi = null;
            }
        }
        return irisApi != null;
    }

    private static boolean askIris(Method question) {
        try {
            return (boolean)question.invoke(irisApi);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        solidDrawn = false;
    }
}
