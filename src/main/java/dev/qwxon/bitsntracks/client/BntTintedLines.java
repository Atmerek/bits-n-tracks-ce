package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Recolours every line passing through it. */
@OnlyIn(Dist.CLIENT)
public final class BntTintedLines implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int red;
    private final int green;
    private final int blue;

    public BntTintedLines(VertexConsumer delegate, int red, int green, int blue) {
        this.delegate = delegate;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.delegate.setColor(this.red, this.green, this.blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }
}
