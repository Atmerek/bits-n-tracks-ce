package dev.qwxon.bitsntracks.client.flywheel;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.qwxon.bitsntracks.BitsNTracks;
import org.lwjgl.system.MemoryUtil;

public final class BntInstanceTypes {
    public static final InstanceType<BntSpanInstance> SPAN = SimpleInstanceType.builder(BntSpanInstance::new)
        .layout(
            LayoutBuilder.create()
                .vector("lightFrom", FloatRepr.UNSIGNED_SHORT, 2)
                .vector("lightTo", FloatRepr.UNSIGNED_SHORT, 2)
                .vector("originFrom", FloatRepr.FLOAT, 3)
                .vector("acrossFrom", FloatRepr.FLOAT, 3)
                .vector("upFrom", FloatRepr.FLOAT, 3)
                .vector("originTo", FloatRepr.FLOAT, 3)
                .vector("acrossTo", FloatRepr.FLOAT, 3)
                .vector("upTo", FloatRepr.FLOAT, 3)
                .vector("textureV", FloatRepr.FLOAT, 2)
                .scalar("stamp", IntegerRepr.INT)
                .vector("timing", FloatRepr.FLOAT, 3)
                .vector("drift", FloatRepr.FLOAT, 3)
                .vector("along", FloatRepr.FLOAT, 2)
                .build()
        )
        .writer((ptr, instance) -> {
            ExtraMemoryOps.put2x16(ptr, instance.lightFrom);
            ExtraMemoryOps.put2x16(ptr + 4L, instance.lightTo);
            float[] frames = instance.frames;
            for (int i = 0; i < frames.length; i++) {
                MemoryUtil.memPutFloat(ptr + 8L + i * 4L, frames[i]);
            }
            MemoryUtil.memPutFloat(ptr + 80L, instance.minV);
            MemoryUtil.memPutFloat(ptr + 84L, instance.maxV);
            MemoryUtil.memPutInt(ptr + 88L, instance.stampTicks);
            MemoryUtil.memPutFloat(ptr + 92L, instance.stampPartial);
            MemoryUtil.memPutFloat(ptr + 96L, instance.reach);
            MemoryUtil.memPutFloat(ptr + 100L, instance.scrollV);
            MemoryUtil.memPutFloat(ptr + 104L, instance.driftX);
            MemoryUtil.memPutFloat(ptr + 108L, instance.driftY);
            MemoryUtil.memPutFloat(ptr + 112L, instance.driftZ);
            MemoryUtil.memPutFloat(ptr + 116L, instance.along);
            MemoryUtil.memPutFloat(ptr + 120L, instance.alongRate);
        })
        .vertexShader(BitsNTracks.asResource("instance/span.vert"))
        .cullShader(BitsNTracks.asResource("instance/cull/span.glsl"))
        .build();

    private BntInstanceTypes() {
    }
}
