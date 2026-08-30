package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.ChainDriveShapeHelper;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainWholeShape;
import dev.qwxon.bitsntracks.client.BntChainShapeContext;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {ChainDriveShapeHelper.class},
    remap = false
)
public abstract class ChainDriveShapeHelperMixin {
    @Redirect(
        method = "findClosestRayHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;DZ)Lcom/kipti/bnb/content/kinetics/cogwheel_chain/shape/ChainDriveShapeHelper$ChainShapeHit;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;"
        )
    )
    private static Object bnt$captureChainController(
        Map.Entry<BlockPos, CogwheelChain> entry, Level level, Vec3 from, Vec3 to, double maxDistance, boolean rendered
    ) {
        BntChainShapeContext.set(rendered ? level : null, entry.getKey());
        return entry.getValue();
    }

    @Redirect(
        method = "findClosestRayHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;DZ)Lcom/kipti/bnb/content/kinetics/cogwheel_chain/shape/ChainDriveShapeHelper$ChainShapeHit;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/shape/CogwheelChainWholeShape;buildShape(Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChain;)Lcom/kipti/bnb/content/kinetics/cogwheel_chain/shape/CogwheelChainWholeShape;"
        )
    )
    private static CogwheelChainWholeShape bnt$buildShapeWithSuspension(CogwheelChain chain) {
        try {
            return CogwheelChainWholeShape.buildShape(chain);
        } finally {
            BntChainShapeContext.clear();
        }
    }
}
