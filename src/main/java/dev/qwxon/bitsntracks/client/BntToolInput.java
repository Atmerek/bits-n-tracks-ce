package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainInteractionHandler;
import com.simibubi.create.AllKeys;
import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.interaction.BntAlignmentModePayload;
import dev.qwxon.bitsntracks.interaction.BntLeverPayload;
import dev.qwxon.bitsntracks.interaction.BntToolMessages;
import dev.qwxon.bitsntracks.interaction.BntTuningModePayload;
import dev.qwxon.bitsntracks.interaction.BntCogwheelInteraction;
import dev.qwxon.bitsntracks.physics.BntTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntToolInput {
    private static boolean attackHandled;
    private static boolean useHandled;
    private static boolean attackWasDown;
    private static boolean useWasDown;
    private static double scrollCarry;

    private BntToolInput() {
    }

    public static boolean menuKeyDown() {
        return AllKeys.TOOL_MENU.isPressed();
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean lever = stack.getItem() instanceof CogAlignmentLeverItem;
        boolean tuner = stack.getItem() instanceof SuspensionToolItem;
        if (!lever && !tuner) {
            return;
        }

        if (event.isAttack()) {
            if (!Screen.hasControlDown()) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!attackHandled && !attackWasDown) {
                attackHandled = true;
                toggleScope(stack, lever);
            }
            return;
        }
        if (!event.isUseItem()) {
            return;
        }

        boolean fresh = !useHandled && !useWasDown;
        boolean consumed = lever ? useLever(level, stack, fresh) : useTuner(level, fresh);
        if (consumed) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (fresh) {
                useHandled = true;
            }
        }
    }

    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.screen != null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean lever = stack.getItem() instanceof CogAlignmentLeverItem;
        boolean tuner = stack.getItem() instanceof SuspensionToolItem;
        if (!lever && !tuner) {
            return;
        }

        scrollCarry += event.getScrollDeltaY();
        int steps = (int)scrollCarry;
        scrollCarry -= steps;
        boolean focused = menuKeyDown();
        boolean consumes = focused
            || lever && CogAlignmentLeverItem.mode(stack) == BntLeverMode.OFFSET && BntToolState.leverCog != null
            || lever && CogAlignmentLeverItem.mode(stack) == BntLeverMode.TENSION && BntToolState.leverChain != null
            || tuner && BntToolState.tunerCog != null;
        if (!consumes) {
            scrollCarry = 0.0;
            return;
        }
        event.setCanceled(true);
        if (steps == 0) {
            return;
        }
        BntToolSpin.kick(steps);

        if (focused) {
            cycleMode(stack, lever, steps < 0 ? 1 : -1);
        } else if (lever && CogAlignmentLeverItem.mode(stack) == BntLeverMode.OFFSET) {
            if (Screen.hasShiftDown()) {
                BntToolState.cycleAxis(level, BntToolState.leverCog, steps < 0 ? 1 : -1);
                BntToolCues.navigate(BntToolCues.AXIS);
            } else {
                BntToolState.addOffsetSteps(steps);
            }
        } else if (lever) {
            BntToolState.addTensionSteps(steps);
        } else {
            BntToolState.addTuningSteps(steps);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        attackWasDown = minecraft.options.keyAttack.isDown();
        useWasDown = minecraft.options.keyUse.isDown();
        attackHandled = false;
        useHandled = false;
        BntToolState.tick();
        BntToolOutlines.tick();
    }

    private static boolean useLever(ClientLevel level, ItemStack stack, boolean fresh) {
        BlockPos cog = aimedCog(level);
        BntLeverMode mode = CogAlignmentLeverItem.mode(stack);
        switch (mode) {
            case OFFSET -> {
                if (cog == null && BntToolState.leverCog == null) {
                    return false;
                }
                if (fresh) {
                    BntToolState.selectLeverCog(cog == null || cog.equals(BntToolState.leverCog) ? null : cog.immutable());
                    BntToolCues.select(BntToolState.leverCog != null);
                }
                return true;
            }
            case TENSION -> {
                BlockPos chain = aimedChain(level, cog);
                if (chain == null && BntToolState.leverChain == null) {
                    return false;
                }
                if (fresh) {
                    BntToolState.selectLeverChain(chain == null || chain.equals(BntToolState.leverChain) ? null : chain.immutable());
                    BntToolCues.select(BntToolState.leverChain != null);
                }
                return true;
            }
            case ROUTE -> {
                if (cog == null) {
                    return false;
                }
                if (fresh) {
                    BntClientRouteClick.send(cog);
                }
                return true;
            }
            case VISIBILITY -> {
                return sendLever(cog, BntLeverPayload.VISIBILITY, fresh);
            }
            case WIDTH -> {
                if (cog == null) {
                    return false;
                }
                if (fresh) {
                    PacketDistributor.sendToServer(new BntLeverPayload(
                        cog, BntLeverPayload.WIDTH, 0, Screen.hasShiftDown() ? -1 : 1, CogAlignmentLeverItem.wholeTrack(stack)));
                }
                return true;
            }
            case PHYSICS -> {
                BlockPos target = aimedBlock(level);
                if (target == null || !BntCogwheelInteraction.canTogglePhysics(level, target)) {
                    return false;
                }
                return sendLever(target, BntLeverPayload.PHYSICS, fresh);
            }
            case RESET -> {
                if (cog == null) {
                    return false;
                }
                if (fresh) {
                    if (BntToolState.armReset(cog)) {
                        PacketDistributor.sendToServer(new BntLeverPayload(cog, BntLeverPayload.RESET, 0, 0, CogAlignmentLeverItem.wholeTrack(stack)));
                    } else {
                        BntToolMessages.clear();
                        BntToolCues.arm();
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean sendLever(@Nullable BlockPos pos, int action, boolean fresh) {
        if (pos == null) {
            return false;
        }
        if (fresh) {
            PacketDistributor.sendToServer(new BntLeverPayload(pos, action, 0, 0, false));
        }
        return true;
    }

    private static boolean useTuner(ClientLevel level, boolean fresh) {
        BlockPos pos = aimedBlock(level);
        boolean piece = pos != null && BntSuspension.hasPiece(level.getBlockEntity(pos));
        if (!piece && BntToolState.tunerCog == null) {
            return false;
        }
        if (fresh) {
            BntToolState.selectTunerCog(!piece || pos.equals(BntToolState.tunerCog) ? null : pos.immutable());
            BntToolCues.select(BntToolState.tunerCog != null);
        }
        return true;
    }

    private static void cycleMode(ItemStack stack, boolean lever, int direction) {
        if (lever) {
            BntLeverMode mode = CogAlignmentLeverItem.mode(stack).cycle(direction);
            CogAlignmentLeverItem.setMode(stack, mode);
            PacketDistributor.sendToServer(new BntAlignmentModePayload(mode.ordinal(), CogAlignmentLeverItem.wholeTrack(stack)));
        } else {
            BntTuning setting = SuspensionToolItem.setting(stack).cycle(direction);
            SuspensionToolItem.setMode(stack, setting, SuspensionToolItem.wholeTrack(stack));
            PacketDistributor.sendToServer(new BntTuningModePayload(setting.ordinal(), SuspensionToolItem.wholeTrack(stack)));
        }
        BntToolMessages.clear();
        BntToolCues.navigate(BntToolCues.MODE);
    }

    private static void toggleScope(ItemStack stack, boolean lever) {
        if (lever) {
            boolean wholeTrack = !CogAlignmentLeverItem.wholeTrack(stack);
            CogAlignmentLeverItem.setWholeTrack(stack, wholeTrack);
            PacketDistributor.sendToServer(new BntAlignmentModePayload(CogAlignmentLeverItem.mode(stack).ordinal(), wholeTrack));
        } else {
            boolean wholeTrack = !SuspensionToolItem.wholeTrack(stack);
            SuspensionToolItem.setMode(stack, SuspensionToolItem.setting(stack), wholeTrack);
            PacketDistributor.sendToServer(new BntTuningModePayload(SuspensionToolItem.setting(stack).ordinal(), wholeTrack));
        }
        BntToolMessages.clear();
        BntToolCues.navigate(BntToolCues.SCOPE);
    }

    @Nullable
    static BlockPos aimedBlock(ClientLevel level) {
        HitResult hit = Minecraft.getInstance().hitResult;
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK ? blockHit.getBlockPos() : null;
    }

    @Nullable
    static BlockPos aimedCog(ClientLevel level) {
        BlockPos pos = aimedBlock(level);
        return pos != null && BntCogwheels.isOwnCogwheel(level, pos) ? pos : null;
    }

    @Nullable
    static BlockPos aimedChain(ClientLevel level, @Nullable BlockPos cog) {
        if (BntBeltClick.wouldRetension()) {
            return CogwheelChainInteractionHandler.getSelectedController();
        }
        return cog == null ? null : BntToolState.chainController(level, cog);
    }
}
