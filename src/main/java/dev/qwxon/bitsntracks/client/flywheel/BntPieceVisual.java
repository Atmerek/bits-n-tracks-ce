package dev.qwxon.bitsntracks.client.flywheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionParts;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.joml.Vector4fc;

final class BntPieceVisual implements BntSuspensionParts.Sink {
    private static final Vector4fc BOUNDS = new Vector4f(0.0F, 0.5F, 0.0F, 2.0F);
    private static final Map<PartKey, Model> MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Material> MATERIALS = new ConcurrentHashMap<>();

    private final VisualizationContext context;
    private final KineticBlockEntity be;
    private final Vec3 offset;
    private final Map<Model, BntInstancePool<TransformedInstance>> pools = new HashMap<>();
    private int light;
    private boolean showing;

    BntPieceVisual(VisualizationContext context, KineticBlockEntity be, Vec3i visualPos) {
        this.context = context;
        this.be = be;
        this.offset = Vec3.atLowerCornerOf(visualPos);
    }

    void frame(float partialTick, int packedLight) {
        light = packedLight;
        for (BntInstancePool<TransformedInstance> pool : pools.values()) {
            pool.begin();
        }
        BntSuspensionParts.attached(be, partialTick, this);
        for (BntInstancePool<TransformedInstance> pool : pools.values()) {
            pool.settle();
        }
        showing = true;
    }

    boolean showing() {
        return showing;
    }

    void hide() {
        if (!showing) {
            return;
        }
        for (BntInstancePool<TransformedInstance> pool : pools.values()) {
            pool.begin();
            pool.settle();
        }
        showing = false;
    }

    void delete() {
        for (BntInstancePool<TransformedInstance> pool : pools.values()) {
            pool.delete();
        }
        pools.clear();
        showing = false;
    }

    @Override
    public void part(int model, int part, Vec3 x, Vec3 y, Vec3 z, Vec3 origin, Vec3 normalX, Vec3 normalY) {
        boolean mirrored = x.dot(y.cross(z)) < 0.0;
        Model mesh = MODELS.computeIfAbsent(new PartKey(model, part, mirrored), BntPieceVisual::build);
        TransformedInstance instance = pools
            .computeIfAbsent(mesh, key -> new BntInstancePool<>(context.instancerProvider().instancer(InstanceTypes.TRANSFORMED, key)))
            .next();
        Vec3 at = origin.add(offset);
        instance.pose.set(
            (float)x.x, (float)x.y, (float)x.z, 0.0F,
            (float)y.x, (float)y.y, (float)y.z, 0.0F,
            (float)z.x, (float)z.y, (float)z.z, 0.0F,
            (float)at.x, (float)at.y, (float)at.z, 1.0F);
        instance.light(light);
        instance.setChanged();
    }

    private static Model build(PartKey key) {
        ResourceLocation texture = BntSuspensionParts.texture(key.model());
        Material material = MATERIALS.computeIfAbsent(texture, location -> SimpleMaterial.builder()
            .texture(location)
            .cutout(CutoutShaders.ONE_TENTH)
            .mipmap(false)
            .ambientOcclusion(false)
            .build());
        return new SingleMeshModel(new PartMesh(BntSuspensionParts.quads(key.model()), BntSuspensionParts.quadsOf(key.model(), key.part()), key.mirrored()), material);
    }

    private record PartKey(int model, int part, boolean mirrored) {
    }

    private record PartMesh(float[] quads, int[] starts, boolean mirrored) implements QuadMesh {
        @Override
        public int vertexCount() {
            return starts.length * 4;
        }

        @Override
        public void write(MutableVertexList list) {
            int index = 0;
            for (int quad : starts) {
                for (int i = 0; i < 4; i++) {
                    int at = quad + (mirrored ? 3 - i : i) * 5;
                    list.x(index, quads[at]);
                    list.y(index, quads[at + 1]);
                    list.z(index, quads[at + 2]);
                    list.u(index, quads[at + 3]);
                    list.v(index, quads[at + 4]);
                    list.r(index, 1.0F);
                    list.g(index, 1.0F);
                    list.b(index, 1.0F);
                    list.a(index, 1.0F);
                    list.normalX(index, quads[quad + 20]);
                    list.normalY(index, quads[quad + 21]);
                    list.normalZ(index, quads[quad + 22]);
                    list.light(index, 0);
                    list.overlay(index, OverlayTexture.NO_OVERLAY);
                    index++;
                }
            }
        }

        @Override
        public Vector4fc boundingSphere() {
            return BOUNDS;
        }
    }
}
