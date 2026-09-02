package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditInteractionHandler;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment.SegmentType;
import com.llamalad7.mixinextras.sugar.Local;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEdit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {CogwheelChainPartialEditInteractionHandler.class},
    remap = false
)
public abstract class CogwheelChainPartialEditInteractionHandlerMixin {
    @Redirect(
        method = {"resolveSelectedSegmentNodes"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChain;getSegments()Ljava/util/List;"
        )
    )
    private static List<CogwheelChainSegment> bnt$countSegmentsAsNodes(
        CogwheelChain chain, @Local(argsOnly = true) CogwheelChainSegment selectedSegment) {
        int nodeIndex = BntChainEdit.nodeIndexForSegment(chain, selectedSegment);
        if (nodeIndex < 0) {
            return chain.getSegments();
        }

        List<CogwheelChainSegment> counted = new ArrayList<>(nodeIndex + 1);
        for (int i = 0; i < nodeIndex; i++) {
            counted.add(new CogwheelChainSegment(Vec3.ZERO, Vec3.ZERO, SegmentType.BETWEEN_NODES, -1.0F - i, -1.0F - i));
        }
        counted.add(selectedSegment);
        return counted;
    }
}
