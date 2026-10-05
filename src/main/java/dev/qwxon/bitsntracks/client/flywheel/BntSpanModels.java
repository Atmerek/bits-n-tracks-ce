package dev.qwxon.bitsntracks.client.flywheel;

import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder.VertexEmitter;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltFaces;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntTankTread;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntTrackSink;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.joml.Vector4fc;

final class BntSpanModels {
    private static final List<Vec3> SOURCE = List.of(new Vec3(1.0, 1.0, 0.0), new Vec3(-1.0, 1.0, 0.0), new Vec3(-1.0, -1.0, 0.0), new Vec3(1.0, -1.0, 0.0));
    private static final List<Vec3> DESTINATION = List.of(new Vec3(1.0, 1.0, 1.0), new Vec3(-1.0, 1.0, 1.0), new Vec3(-1.0, -1.0, 1.0), new Vec3(1.0, -1.0, 1.0));
    private static final Vector4fc BOUNDS = new Vector4f(0.0F, 0.0F, 0.5F, 1.5F);
    private static final Map<Key, Model> MODELS = new ConcurrentHashMap<>();
    private static final Map<Sheet, Model> SHEETS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Material> MATERIALS = new ConcurrentHashMap<>();

    private BntSpanModels() {
    }

    static Model span(ResourceLocation texture, Object shape) {
        return MODELS.computeIfAbsent(new Key(texture, shape), BntSpanModels::build);
    }

    static Model box(ResourceLocation texture, float[][] uv, boolean mirrored) {
        return SHEETS.computeIfAbsent(new Sheet(texture, uv, mirrored),
            sheet -> MODELS.computeIfAbsent(new Key(texture, new Box(new Uv(uv), mirrored)), BntSpanModels::build));
    }

    private static Model build(Key key) {
        Captured mesh = new Captured();
        if (key.shape() instanceof BntTrackSink.Belt belt) {
            BntBeltFaces.emit(mesh, SOURCE, DESTINATION, belt.flip(), belt.wide(), 0.0F, 1.0F);
        } else if (key.shape() instanceof BntTrackSink.Generic generic) {
            ChainQuadBuilder.buildSegmentFaces(DESTINATION, SOURCE, generic.info(), 0.0F, 1.0F, generic.flip(), mesh, true);
        } else if (key.shape() instanceof Box box) {
            BntTankTread.unitBox(box.uv().faces(), box.mirrored(), mesh);
        }
        return new SingleMeshModel(new SpanMesh(mesh.data.toFloatArray()), MATERIALS.computeIfAbsent(key.texture(), BntSpanModels::material));
    }

    private static Material material(ResourceLocation texture) {
        return SimpleMaterial.builderOf(Materials.CUTOUT_MIPPED_UNSHADED_BLOCK)
            .texture(texture)
            .mipmap(true)
            .backfaceCulling(true)
            .ambientOcclusion(false)
            .build();
    }

    private record Key(ResourceLocation texture, Object shape) {
    }

    private record Sheet(ResourceLocation texture, float[][] uv, boolean mirrored) {
    }

    private record Box(Uv uv, boolean mirrored) {
    }

    private record Uv(float[][] faces) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Uv uv && Arrays.deepEquals(faces, uv.faces);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(faces);
        }
    }

    private static final class Captured implements VertexEmitter {
        private final FloatArrayList data = new FloatArrayList();

        @Override
        public void emit(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            data.add(x);
            data.add(y);
            data.add(z);
            data.add(u);
            data.add(v);
            data.add(nx);
            data.add(ny);
            data.add(nz);
        }
    }

    private record SpanMesh(float[] data) implements QuadMesh {
        @Override
        public int vertexCount() {
            return data.length / 8;
        }

        @Override
        public void write(MutableVertexList list) {
            for (int i = 0; i < vertexCount(); i++) {
                int at = i * 8;
                list.x(i, data[at]);
                list.y(i, data[at + 1]);
                list.z(i, data[at + 2]);
                list.u(i, data[at + 3]);
                list.v(i, data[at + 4]);
                list.r(i, 1.0F);
                list.g(i, 1.0F);
                list.b(i, 1.0F);
                list.a(i, 1.0F);
                list.normalX(i, data[at + 5]);
                list.normalY(i, data[at + 6]);
                list.normalZ(i, data[at + 7]);
                list.light(i, 0);
                list.overlay(i, OverlayTexture.NO_OVERLAY);
            }
        }

        @Override
        public Vector4fc boundingSphere() {
            return BOUNDS;
        }
    }
}
