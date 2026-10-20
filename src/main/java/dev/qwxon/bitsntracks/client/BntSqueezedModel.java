package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
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
    private static final SuperByteBufferCache.Compartment<Key> BUFFERS = new SuperByteBufferCache.Compartment<>();

    private final int axis;
    private final float centre;
    private final float face;
    private final float rim;
    private final float shift;

    private BntSqueezedModel(BakedModel model, BlockState state, int trim) {
        super(model);
        this.axis = state.getValue(RotatedPillarKineticBlock.AXIS).ordinal();
        BntWideSide side = BntCogwheelPairing.sideOf(state);
        this.centre = side == BntWideSide.NONE ? 0.5F : side == BntWideSide.POSITIVE ? 1.0F : 0.0F;
        this.face = side == BntWideSide.NONE ? 0.5F : 1.0F;
        this.rim = this.face - (float)BntCogwheelWidth.FLANGE_INNER * PX;
        this.shift = (float)BntCogwheelWidth.flangeShift(side != BntWideSide.NONE, trim) * PX;
    }

    public static void register() {
        SuperByteBufferCache.getInstance().registerCompartment(BUFFERS);
    }

    public static BakedModel of(BakedModel model, BlockState state, int trim) {
        return trim <= 0 ? model : new BntSqueezedModel(model, state, trim);
    }

    public static SuperByteBuffer buffer(BlockState state, int trim) {
        return SuperByteBufferCache.getInstance().get(BUFFERS, new Key(state, trim), () -> SuperBufferFactory.getInstance()
            .createForBlock(of(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state, trim), state));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return squeeze(super.getQuads(state, side, rand));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return squeeze(super.getQuads(state, side, rand, data, renderType));
    }

    private List<BakedQuad> squeeze(List<BakedQuad> quads) {
        List<BakedQuad> squeezed = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            squeezed.add(squeeze(quad));
        }
        return squeezed;
    }

    private BakedQuad squeeze(BakedQuad quad) {
        int[] vertices = quad.getVertices().clone();
        for (int vertex = 0; vertex < vertices.length / IQuadTransformer.STRIDE; vertex++) {
            int base = vertex * IQuadTransformer.STRIDE;
            int at = base + IQuadTransformer.POSITION + axis;
            float position = Float.intBitsToFloat(vertices[at]);
            float offset = position - centre;
            float distance = Math.abs(offset);
            if (distance < face - EPSILON) {
                float moved = distance >= rim - EPSILON ? distance - shift : distance * (rim - shift) / rim;
                vertices[at] = Float.floatToRawIntBits(centre + Math.copySign(moved, offset));
            }
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    private record Key(BlockState state, int trim) {
    }
}
