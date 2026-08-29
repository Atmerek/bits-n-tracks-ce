package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber(
    modid = "bits_n_tracks"
)
public class TrackModelInteractionHandler {
    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        Player player = event.getEntity();
        if (player.isShiftKeyDown()) {
            ItemStack itemInHand = event.getItemStack();
            if (itemInHand.isEmpty()) {
                Level level = event.getLevel();
                BlockPos pos = event.getPos();
                if (level.getBlockEntity(pos) instanceof KineticBlockEntity kbe) {
                    CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)kbe.getBehaviour(CogwheelChainBehaviour.TYPE);
                    if (behaviour != null && behaviour.isPartOfChain()) {
                        CogwheelChainBehaviour controllerBehaviour = behaviour.isController() ? behaviour : getController(behaviour);
                        if (controllerBehaviour != null) {
                            String typeName = "";

                            try {
                                CogwheelChain chain = controllerBehaviour.getControlledChain();
                                if (chain != null) {
                                    CogwheelChainType chainType = chain.getChainType();
                                    if (chainType != null) {
                                        ResourceLocation key = chainType.getKey();
                                        if (key != null) {
                                            typeName = key.toString();
                                        }
                                    }
                                }
                            } catch (Exception var13) {
                            }

                            if (typeName.toLowerCase().contains("belt")) {
                                KineticBlockEntity controller = (KineticBlockEntity)controllerBehaviour.getBlockEntity();
                                event.setCanceled(true);
                                event.setCancellationResult(InteractionResult.SUCCESS);
                                player.swing(event.getHand());
                                if (!level.isClientSide && controller instanceof TrackModelBehaviourAccess access) {
                                    access.bnt$setTrackModel(!access.bnt$isTrackModel());
                                    controller.setChanged();
                                    level.sendBlockUpdated(controller.getBlockPos(), controller.getBlockState(), controller.getBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static CogwheelChainBehaviour getController(CogwheelChainBehaviour component) {
        if (component.getControllerOffset() != null && component.getLevel() != null) {
            BlockPos controllerPos = component.getPos().offset(component.getControllerOffset());
            return component.getLevel().getBlockEntity(controllerPos) instanceof KineticBlockEntity kbe
                ? (CogwheelChainBehaviour)kbe.getBehaviour(CogwheelChainBehaviour.TYPE)
                : null;
        } else {
            return null;
        }
    }
}
