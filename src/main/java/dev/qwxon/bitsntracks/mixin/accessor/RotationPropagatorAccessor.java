package dev.qwxon.bitsntracks.mixin.accessor;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RotationPropagator.class, remap = false)
public interface RotationPropagatorAccessor {
    @Invoker("getPotentialNeighbourLocations")
    static List<BlockPos> bnt$potentialNeighbours(KineticBlockEntity be) {
        throw new AssertionError();
    }
}
