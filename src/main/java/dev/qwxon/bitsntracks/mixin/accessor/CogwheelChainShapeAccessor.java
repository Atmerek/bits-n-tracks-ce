package dev.qwxon.bitsntracks.mixin.accessor;

import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.UnaryOperator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CogwheelChainShape.class, remap = false)
public interface CogwheelChainShapeAccessor {
    @Invoker("drawOutline")
    void bnt$drawOutline(PoseStack ms, VertexConsumer consumer, UnaryOperator<Vec3> transform);
}
