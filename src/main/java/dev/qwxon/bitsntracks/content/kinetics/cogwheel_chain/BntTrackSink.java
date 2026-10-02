package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public interface BntTrackSink {
    ThreadLocal<BntTrackSink> CURRENT = new ThreadLocal<>();

    static BntTrackSink current() {
        return CURRENT.get();
    }

    double scrollRate();

    double reach();

    boolean links();

    void at(ResourceLocation texture, Vec3 origin);

    void span(Object shape, List<Vec3> source, List<Vec3> destination, float minV, float maxV, float scrollV, int lightFrom, int lightTo);

    void box(float[][] uv, Vec3 centre, Vec3 width, Vec3 thick, Vec3 length, int light, Vec3 drift, double along, double alongRate);

    record Belt(boolean flip, boolean wide) {
    }

    record Generic(ChainRenderInfo info, boolean flip) {
    }
}
