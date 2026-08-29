package dev.qwxon.bitsntracks.physics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.server.level.ServerLevel;

public final class BntPhysicsRegistry {
    private static final WorldAttached<Collection<KineticBlockEntity>> ENABLED_WHEELS = new WorldAttached(ignored -> new ObjectOpenHashSet());

    private BntPhysicsRegistry() {
    }

    public static void add(KineticBlockEntity be) {
        if (be.getLevel() != null) {
            ((Collection)ENABLED_WHEELS.get(be.getLevel())).add(be);
        }
    }

    public static void remove(KineticBlockEntity be) {
        if (be.getLevel() != null) {
            ((Collection)ENABLED_WHEELS.get(be.getLevel())).remove(be);
        }
    }

    public static Collection<KineticBlockEntity> getEnabled(ServerLevel level) {
        return (Collection<KineticBlockEntity>)ENABLED_WHEELS.get(level);
    }
}
