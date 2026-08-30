package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainWholeShape;
import dev.qwxon.bitsntracks.client.BntChainShapeContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {CogwheelChainWholeShape.class},
    remap = false
)
public abstract class CogwheelChainWholeShapeMixin {
    @Redirect(
        method = "buildShape",
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/RenderedChainPathNode;getPosition()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 bnt$offsetNodePositionForSuspension(RenderedChainPathNode node) {
        return BntChainShapeContext.transform(node);
    }
}
