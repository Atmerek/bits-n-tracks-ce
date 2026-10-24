package dev.qwxon.bitsntracks.physics;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class BntSubLevelLinks {
    private static final WorldAttached<List<Link>> LINKS = new WorldAttached<>(ignored -> new ArrayList<>());

    private BntSubLevelLinks() {
    }

    public record Link(
        PhysicsPipeline pipeline, ServerSubLevel a, ServerSubLevel b, PhysicsConstraintConfiguration<?> configuration,
        PhysicsConstraintHandle handle
    ) {
        boolean alive(PhysicsPipeline current) {
            return this.pipeline == current && !this.a.isRemoved() && !this.b.isRemoved() && this.handle.isValid();
        }
    }

    public static synchronized void record(
        PhysicsPipeline pipeline, @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB,
        PhysicsConstraintConfiguration<?> configuration, @Nullable PhysicsConstraintHandle handle
    ) {
        if (handle != null && bodyA instanceof ServerSubLevel a && bodyB instanceof ServerSubLevel b && carriesLoad(configuration)) {
            LINKS.get(a.getLevel()).add(new Link(pipeline, a, b, configuration, handle));
        }
    }

    public static synchronized List<Link> within(ServerLevel level, PhysicsPipeline pipeline, Collection<ServerSubLevel> bodies) {
        List<Link> links = new ArrayList<>();
        for (Link link : LINKS.get(level)) {
            if (link.alive(pipeline) && bodies.contains(link.a()) && bodies.contains(link.b())) {
                links.add(link);
            }
        }
        return links;
    }

    private static boolean carriesLoad(PhysicsConstraintConfiguration<?> configuration) {
        if (configuration instanceof RotaryConstraintConfiguration || configuration instanceof FixedConstraintConfiguration) {
            return true;
        }
        if (configuration instanceof GenericConstraintConfiguration generic) {
            for (ConstraintJointAxis axis : ConstraintJointAxis.LINEAR) {
                if (generic.lockedAxes().contains(axis)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static synchronized List<List<ServerSubLevel>> assemblies(
        ServerLevel level, PhysicsPipeline pipeline, Collection<ServerSubLevel> bodies
    ) {
        Map<ServerSubLevel, List<ServerSubLevel>> joined = new Reference2ObjectOpenHashMap<>();
        Iterator<Link> iterator = LINKS.get(level).iterator();
        while (iterator.hasNext()) {
            Link link = iterator.next();
            if (!link.alive(pipeline)) {
                iterator.remove();
                continue;
            }
            joined.computeIfAbsent(link.a(), ignored -> new ArrayList<>()).add(link.b());
            joined.computeIfAbsent(link.b(), ignored -> new ArrayList<>()).add(link.a());
        }

        List<List<ServerSubLevel>> assemblies = new ArrayList<>();
        Set<ServerSubLevel> visited = new ReferenceOpenHashSet<>();
        for (ServerSubLevel body : bodies) {
            if (!visited.add(body)) {
                continue;
            }
            List<ServerSubLevel> assembly = new ArrayList<>();
            ArrayDeque<ServerSubLevel> open = new ArrayDeque<>();
            open.add(body);
            while (!open.isEmpty()) {
                ServerSubLevel next = open.poll();
                assembly.add(next);
                for (ServerSubLevel neighbour : joined.getOrDefault(next, List.of())) {
                    if (visited.add(neighbour)) {
                        open.add(neighbour);
                    }
                }
            }
            assemblies.add(assembly);
        }
        return assemblies;
    }
}
