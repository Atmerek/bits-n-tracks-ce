package dev.qwxon.bitsntracks.physics;

import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.util.SableMathUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record BntAssemblyMass(double mass, Matrix3dc inertia, Matrix3dc inverseInertia, Vector3dc centre) implements MassData {
    @Override
    public double getMass() {
        return this.mass;
    }

    @Override
    public double getInverseMass() {
        return 1.0 / this.mass;
    }

    @Override
    public Matrix3dc getInertiaTensor() {
        return this.inertia;
    }

    @Override
    public Matrix3dc getInverseInertiaTensor() {
        return this.inverseInertia;
    }

    @Override
    public Vector3dc getCenterOfMass() {
        return this.centre;
    }

    private record Part(double mass, Vector3d centre, Matrix3d inertia) {
    }

    @Nullable
    public static ServerSubLevel heaviest(List<ServerSubLevel> bodies) {
        ServerSubLevel heaviest = null;
        double most = 0.0;
        for (ServerSubLevel body : bodies) {
            MassData data = body.getMassTracker();
            if (data != null && !data.isInvalid() && data.getMass() > most) {
                most = data.getMass();
                heaviest = body;
            }
        }
        return heaviest;
    }

    @Nullable
    public static MassData of(ServerSubLevel frame, List<ServerSubLevel> bodies) {
        if (bodies.size() == 1) {
            return frame.getMassTracker();
        }

        Pose3dc framePose = frame.logicalPose();
        Quaterniond toFrame = framePose.orientation().conjugate(new Quaterniond());
        List<Part> parts = new ArrayList<>(bodies.size());
        double mass = 0.0;
        Vector3d centre = new Vector3d();
        for (ServerSubLevel body : bodies) {
            MassData data = body.getMassTracker();
            if (data == null || data.isInvalid()) {
                continue;
            }
            Pose3dc pose = body.logicalPose();
            Vector3d partCentre = framePose.transformPositionInverse(pose.transformPosition(data.getCenterOfMass(), new Vector3d()));
            Matrix3d rotation = new Matrix3d().rotation(new Quaterniond(toFrame).mul(pose.orientation()));
            Matrix3d inertia = new Matrix3d(rotation).mul(data.getInertiaTensor()).mul(rotation.transpose(new Matrix3d()));
            parts.add(new Part(data.getMass(), partCentre, inertia));
            mass += data.getMass();
            centre.fma(data.getMass(), partCentre);
        }
        if (mass <= 0.0) {
            return null;
        }

        centre.div(mass);
        Matrix3d inertia = new Matrix3d().zero();
        for (Part part : parts) {
            inertia.add(part.inertia());
            SableMathUtils.fmaInertiaTensor(part.centre().sub(centre, new Vector3d()), part.mass(), inertia);
        }
        return new BntAssemblyMass(mass, inertia, inertia.invert(new Matrix3d()), centre);
    }
}
