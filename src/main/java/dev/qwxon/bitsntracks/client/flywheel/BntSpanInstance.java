package dev.qwxon.bitsntracks.client.flywheel;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import net.minecraft.world.phys.Vec3;

public final class BntSpanInstance extends AbstractInstance {
    public int lightFrom;
    public int lightTo;
    public final float[] frames = new float[18];
    public float minV;
    public float maxV = 1.0F;
    public int stampTicks;
    public float stampPartial;
    public float reach;
    public float scrollV;
    public float driftX;
    public float driftY;
    public float driftZ;
    public float along = 0.5F;
    public float alongRate;

    public BntSpanInstance(InstanceType<BntSpanInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public BntSpanInstance frame(int end, Vec3 origin, Vec3 across, Vec3 up) {
        int at = end * 9;
        frames[at] = (float)origin.x;
        frames[at + 1] = (float)origin.y;
        frames[at + 2] = (float)origin.z;
        frames[at + 3] = (float)across.x;
        frames[at + 4] = (float)across.y;
        frames[at + 5] = (float)across.z;
        frames[at + 6] = (float)up.x;
        frames[at + 7] = (float)up.y;
        frames[at + 8] = (float)up.z;
        return this;
    }

    public BntSpanInstance texture(float minV, float maxV) {
        this.minV = minV;
        this.maxV = maxV;
        return this;
    }

    public BntSpanInstance light(int from, int to) {
        lightFrom = from;
        lightTo = to;
        return this;
    }

    public BntSpanInstance stamp(int ticks, float partial, float reach) {
        stampTicks = ticks;
        stampPartial = partial;
        this.reach = reach;
        return this;
    }

    public BntSpanInstance scroll(float scrollV) {
        this.scrollV = scrollV;
        driftX = 0.0F;
        driftY = 0.0F;
        driftZ = 0.0F;
        along = 0.5F;
        alongRate = 0.0F;
        return this;
    }

    public BntSpanInstance travel(Vec3 drift, double along, double alongRate) {
        scrollV = 0.0F;
        driftX = (float)drift.x;
        driftY = (float)drift.y;
        driftZ = (float)drift.z;
        this.along = (float)along;
        this.alongRate = (float)alongRate;
        return this;
    }
}
