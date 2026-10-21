package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntCogwheelShafts;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
import dev.qwxon.bitsntracks.content.BntShaftEnds;
import dev.qwxon.bitsntracks.content.BntWideSide;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.render.SuperBufferFactory;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntSqueezedModel extends BakedModelWrapper<BakedModel> {
    private static final float EPSILON = 1.0E-4F;
    private static final float PX = 1.0F / 16.0F;
    private static final float AXLE = 2.0F * PX;
    private static final float INSET = 0.5F * PX;
    private static final SuperByteBufferCache.Compartment<Key> BUFFERS = new SuperByteBufferCache.Compartment<>();

    private final int axis;
    private final float centre;
    private final float face;
    private final float rim;
    private final float shift;
    private final float low;
    private final float high;

    private BntSqueezedModel(BakedModel model, BlockState state, int trim, BntShaftEnds shafts) {
        super(model);
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        this.axis = axis.ordinal();
        BntWideSide side = BntCogwheelPairing.sideOf(state);
        this.centre = side == BntWideSide.NONE ? 0.5F : side == BntWideSide.POSITIVE ? 1.0F : 0.0F;
        this.face = side == BntWideSide.NONE ? 0.5F : 1.0F;
        this.rim = this.face - (float)BntCogwheelWidth.FLANGE_INNER * PX;
        this.shift = (float)BntCogwheelWidth.flangeShift(side != BntWideSide.NONE, trim) * PX;
        boolean lowHidden = hidden(state, shafts, axis, AxisDirection.NEGATIVE);
        boolean highHidden = hidden(state, shafts, axis, AxisDirection.POSITIVE);
        float[] body = lowHidden || highHidden ? this.body(model, state) : null;
        this.low = lowHidden ? body[0] + INSET : Float.NaN;
        this.high = highHidden ? body[1] - INSET : Float.NaN;
    }

    public static void register() {
        SuperByteBufferCache.getInstance().registerCompartment(BUFFERS);
    }

    public static boolean changes(BlockState state, int trim, BntShaftEnds shafts) {
        if (trim > 0) {
            return true;
        }
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        return hidden(state, shafts, axis, AxisDirection.NEGATIVE) || hidden(state, shafts, axis, AxisDirection.POSITIVE);
    }

    public static BakedModel of(BakedModel model, BlockState state, int trim, BntShaftEnds shafts) {
        return changes(state, trim, shafts) ? new BntSqueezedModel(model, state, trim, shafts) : model;
    }

    public static SuperByteBuffer buffer(BlockState state, int trim, BntShaftEnds shafts) {
        return SuperByteBufferCache.getInstance().get(BUFFERS, new Key(state, trim, shafts), () -> SuperBufferFactory.getInstance()
            .createForBlock(of(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state, trim, shafts), state));
    }

    private static boolean hidden(BlockState state, BntShaftEnds shafts, Axis axis, AxisDirection direction) {
        return !BntCogwheelShafts.has(state, shafts, Direction.fromAxisAndDirection(axis, direction));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return squeeze(super.getQuads(state, side, rand));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return squeeze(super.getQuads(state, side, rand, data, renderType));
    }

    private float[] body(BakedModel model, BlockState state) {
        float[] body = {1.0F, 0.0F};
        RandomSource random = RandomSource.create(42L);
        for (int index = -1; index < Direction.values().length; index++) {
            for (BakedQuad quad : model.getQuads(state, index < 0 ? null : Direction.values()[index], random, ModelData.EMPTY, null)) {
                int[] vertices = quad.getVertices();
                if (this.axle(vertices)) {
                    continue;
                }
                for (int vertex = 0; vertex < vertices.length / IQuadTransformer.STRIDE; vertex++) {
                    float position = position(vertices, vertex, this.axis);
                    body[0] = Math.min(body[0], position);
                    body[1] = Math.max(body[1], position);
                }
            }
        }
        return body;
    }

    private boolean axle(int[] vertices) {
        for (int vertex = 0; vertex < vertices.length / IQuadTransformer.STRIDE; vertex++) {
            for (int other = 0; other < 3; other++) {
                if (other != this.axis && Math.abs(position(vertices, vertex, other) - 0.5F) > AXLE + EPSILON) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<BakedQuad> squeeze(List<BakedQuad> quads) {
        List<BakedQuad> squeezed = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            squeezed.add(squeeze(quad));
        }
        return squeezed;
    }

    private BakedQuad squeeze(BakedQuad quad) {
        int[] original = quad.getVertices();
        int[] vertices = original.clone();
        int count = vertices.length / IQuadTransformer.STRIDE;
        if ((!Float.isNaN(this.low) || !Float.isNaN(this.high)) && this.axle(original)) {
            this.clip(original, vertices, count);
        }
        if (this.shift > 0.0F) {
            for (int vertex = 0; vertex < count; vertex++) {
                float offset = position(vertices, vertex, this.axis) - this.centre;
                float distance = Math.abs(offset);
                if (distance < this.face - EPSILON) {
                    float moved = distance >= this.rim - EPSILON ? distance - this.shift : distance * (this.rim - this.shift) / this.rim;
                    vertices[vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION + this.axis] = Float.floatToRawIntBits(this.centre + Math.copySign(moved, offset));
                }
            }
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    private void clip(int[] original, int[] vertices, int count) {
        for (int vertex = 0; vertex < count; vertex++) {
            float position = position(original, vertex, this.axis);
            float target = !Float.isNaN(this.low) && position < this.low ? this.low
                : !Float.isNaN(this.high) && position > this.high ? this.high
                : Float.NaN;
            if (Float.isNaN(target)) {
                continue;
            }
            int base = vertex * IQuadTransformer.STRIDE;
            vertices[base + IQuadTransformer.POSITION + this.axis] = Float.floatToRawIntBits(target);
            int partner = this.partner(original, count, vertex);
            if (partner < 0) {
                continue;
            }
            float from = position(original, partner, this.axis);
            float t = (target - from) / (position - from);
            int partnerBase = partner * IQuadTransformer.STRIDE;
            for (int uv = 0; uv < 2; uv++) {
                float start = Float.intBitsToFloat(original[partnerBase + IQuadTransformer.UV0 + uv]);
                float end = Float.intBitsToFloat(original[base + IQuadTransformer.UV0 + uv]);
                vertices[base + IQuadTransformer.UV0 + uv] = Float.floatToRawIntBits(start + (end - start) * t);
            }
        }
    }

    private int partner(int[] vertices, int count, int vertex) {
        for (int other = 0; other < count; other++) {
            if (Math.abs(position(vertices, other, this.axis) - position(vertices, vertex, this.axis)) <= EPSILON) {
                continue;
            }
            boolean across = true;
            for (int coordinate = 0; coordinate < 3; coordinate++) {
                if (coordinate != this.axis && Math.abs(position(vertices, other, coordinate) - position(vertices, vertex, coordinate)) > EPSILON) {
                    across = false;
                }
            }
            if (across) {
                return other;
            }
        }
        return -1;
    }

    private static float position(int[] vertices, int vertex, int coordinate) {
        return Float.intBitsToFloat(vertices[vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION + coordinate]);
    }

    private record Key(BlockState state, int trim, BntShaftEnds shafts) {
    }
}
