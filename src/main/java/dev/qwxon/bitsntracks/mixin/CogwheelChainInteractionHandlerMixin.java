package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.ChainCoordinateSpace;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainInteractionHandler;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {CogwheelChainInteractionHandler.class},
    remap = false
)
public class CogwheelChainInteractionHandlerMixin {
    @Inject(
        method = {"drawCustomBlockSelection"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void bnt$drawDynamicBlockSelection(PoseStack ms, MultiBufferSource buffer, Vec3 cam, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != null) {
            try {
                Field selectedControllerField = CogwheelChainInteractionHandler.class.getDeclaredField("selectedController");
                selectedControllerField.setAccessible(true);
                BlockPos selectedController = (BlockPos)selectedControllerField.get(null);
                Method invalidSelectionMethod = CogwheelChainInteractionHandler.class.getDeclaredMethod("invalidSelection", Level.class);
                invalidSelectionMethod.setAccessible(true);
                if ((Boolean)invalidSelectionMethod.invoke(null, level)) {
                    Method clearSelectionMethod = CogwheelChainInteractionHandler.class.getDeclaredMethod("clearSelection");
                    clearSelectionMethod.setAccessible(true);
                    clearSelectionMethod.invoke(null);
                    ci.cancel();
                    return;
                }

                Field selectedShapeField = CogwheelChainInteractionHandler.class.getDeclaredField("selectedShape");
                selectedShapeField.setAccessible(true);
                CogwheelChainShape selectedShape = (CogwheelChainShape)selectedShapeField.get(null);
                VertexConsumer vc = buffer.getBuffer(RenderType.lines());
                ms.pushPose();
                ms.translate(-cam.x, -cam.y, -cam.z);
                ChainCoordinateSpace space = ChainCoordinateSpace.forRender(level, selectedController);
                BlockEntity be = level.getBlockEntity(selectedController);
                if (be == null) {
                    try {
                        Object clientLevelDataObj = Class.forName("dev.ryanhcode.sable.level.data.ClientLevelData")
                            .getMethod("get", Level.class)
                            .invoke(null, level);

                        for (Object slObj : (Collection)clientLevelDataObj.getClass().getMethod("getSubLevels").invoke(clientLevelDataObj)) {
                            ClientSubLevel sl = (ClientSubLevel)slObj;

                            try {
                                Method getBlockEntityMethod = slObj.getClass().getMethod("getBlockEntity", BlockPos.class);
                                BlockEntity testBe = (BlockEntity)getBlockEntityMethod.invoke(slObj, selectedController);
                                if (testBe != null) {
                                    be = testBe;
                                    break;
                                }
                            } catch (Exception var23) {
                            }
                        }
                    } catch (Exception var24) {
                    }
                }

                boolean isTrackModel = false;
                if (be instanceof KineticBlockEntity kbe) {
                    CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)kbe.getBehaviour(CogwheelChainBehaviour.TYPE);
                    if (behaviour instanceof TrackModelBehaviourAccess access && access.bnt$isTrackModel()) {
                        try {
                            Method getChainTypeMethod = behaviour.getClass().getMethod("getChainType");
                            Object chainType = getChainTypeMethod.invoke(behaviour);
                            if (chainType != null) {
                                Method getKeyMethod = chainType.getClass().getMethod("getKey");
                                Object key = getKeyMethod.invoke(chainType);
                                if (key != null && key.toString().toLowerCase().contains("belt")) {
                                    isTrackModel = true;
                                }
                            }
                        } catch (Exception var22) {
                        }
                    }
                }

                boolean scaleBelt = isTrackModel;
                Method drawOutlineMethod = CogwheelChainShape.class
                    .getDeclaredMethod("drawOutline", PoseStack.class, VertexConsumer.class, UnaryOperator.class);
                drawOutlineMethod.setAccessible(true);
                UnaryOperator<Vec3> spaceTransform = vec -> {
                    if (scaleBelt) {
                        vec = new Vec3(vec.x * 4.0, vec.y, vec.z);
                    }

                    return space.toWorld(vec);
                };
                drawOutlineMethod.invoke(selectedShape, ms, vc, spaceTransform);
                ms.popPose();
                ci.cancel();
            } catch (Exception var25) {
                var25.printStackTrace();
            }
        }
    }
}
