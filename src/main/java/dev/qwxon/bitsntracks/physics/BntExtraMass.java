package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntVisualized;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BntExtraMass {
    private static final double INDUSTRIAL_BELT = 0.25;
    private static final double TANK_TREAD = 0.5;
    private static final double SUSPENSION_PIECE = 0.25;
    private static final long INTERVAL = 20L;
    private static final Map<KineticBlockEntity, Applied> APPLIED = new WeakHashMap<>();

    private BntExtraMass() {
    }

    private record Applied(MassTracker tracker, BlockState state, BlockPos pos, double mass) {
    }

    public static void tick(KineticBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide || !BntVisualized.ownType(be)
            || (level.getGameTime() + be.getBlockPos().hashCode()) % INTERVAL != 0L) {
            return;
        }
        MassTracker tracker = Sable.HELPER.getContaining(be) instanceof ServerSubLevel subLevel && !subLevel.isRemoved()
            ? subLevel.getSelfMassTracker()
            : null;
        Applied applied = APPLIED.get(be);
        double wanted = tracker == null ? 0.0 : wanted(be);
        if (applied != null && applied.tracker() == tracker && applied.state() == be.getBlockState() && applied.mass() == wanted) {
            return;
        }
        release(be);
        if (wanted > 0.0) {
            tracker.addBlockMass(level, be.getBlockState(), be.getBlockPos(), wanted, null);
            APPLIED.put(be, new Applied(tracker, be.getBlockState(), be.getBlockPos().immutable(), wanted));
        }
    }

    public static void release(KineticBlockEntity be) {
        Applied applied = APPLIED.remove(be);
        if (applied != null && be.getLevel() != null) {
            applied.tracker().addBlockMass(be.getLevel(), applied.state(), applied.pos(), -applied.mass(), null);
        }
    }

    private static double wanted(KineticBlockEntity be) {
        double mass = 0.0;
        if (be.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour behaviour && behaviour.isController()) {
            CogwheelChain chain = behaviour.getControlledChain();
            if (chain != null && chain.getChainType() == BntCogwheelChainTypes.INDUSTRIAL_BELT_CHAIN.get()) {
                mass += INDUSTRIAL_BELT;
            } else if (chain != null && chain.getChainType() == BntCogwheelChainTypes.TANK_TREAD_CHAIN.get()) {
                mass += TANK_TREAD;
            }
        }
        if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$getSuspensionSide() != 0) {
            mass += BntSuspension.heldByWidePartner(be) ? SUSPENSION_PIECE / 2.0 : SUSPENSION_PIECE;
        }
        return mass;
    }
}
