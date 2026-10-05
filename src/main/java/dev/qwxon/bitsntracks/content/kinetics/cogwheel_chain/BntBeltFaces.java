package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder.VertexEmitter;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public final class BntBeltFaces {
    private BntBeltFaces() {
    }

    public static void emit(
        VertexEmitter emitter, List<Vec3> source, List<Vec3> destination, boolean flipInsideOutside, boolean wide, float minV, float maxV
    ) {
        float u0Top = 0.875F;
        float u1Top = 0.4375F;
        float u0Bot = 0.4375F;
        float u1Bot = 0.0F;
        if (flipInsideOutside) {
            float temp0 = u0Top;
            float temp1 = u1Top;
            u0Top = u0Bot;
            u1Top = u1Bot;
            u0Bot = temp0;
            u1Bot = temp1;
        }

        float uEdge = wide ? 0.90625F : 0.9375F;
        float uLeftFace0 = 0.875F;
        float uLeftFace1 = uEdge;
        float uRightFace0 = 0.875F;
        float uRightFace1 = uEdge;
        if (flipInsideOutside) {
            float temp = uLeftFace0;
            uLeftFace0 = uLeftFace1;
            uLeftFace1 = temp;
            temp = uRightFace0;
            uRightFace0 = uRightFace1;
            uRightFace1 = temp;
        }

        quad(emitter, destination.get(1), source.get(1), source.get(0), destination.get(0), u0Top, u1Top, maxV, minV);
        quad(emitter, destination.get(2), source.get(2), source.get(1), destination.get(1), uLeftFace1, uLeftFace0, maxV, minV);
        quad(emitter, destination.get(3), source.get(3), source.get(2), destination.get(2), u0Bot, u1Bot, maxV, minV);
        quad(emitter, destination.get(0), source.get(0), source.get(3), destination.get(3), uRightFace0, uRightFace1, maxV, minV);
    }

    private static void quad(VertexEmitter emitter, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, float u0, float u1, float v0, float v1) {
        Vec3 normal = p3.subtract(p1).cross(p4.subtract(p2));
        normal = normal.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : normal.normalize();
        float nx = (float)normal.x;
        float ny = (float)normal.y;
        float nz = (float)normal.z;
        emitter.emit((float)p1.x, (float)p1.y, (float)p1.z, u0, v0, nx, ny, nz);
        emitter.emit((float)p2.x, (float)p2.y, (float)p2.z, u0, v1, nx, ny, nz);
        emitter.emit((float)p3.x, (float)p3.y, (float)p3.z, u1, v1, nx, ny, nz);
        emitter.emit((float)p4.x, (float)p4.y, (float)p4.z, u1, v0, nx, ny, nz);
    }
}
