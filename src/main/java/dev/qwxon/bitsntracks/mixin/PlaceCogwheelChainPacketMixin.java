package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.network.packets.from_client.PlaceCogwheelChainPacket;
import com.simibubi.create.AllItems;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlaceCogwheelChainPacket.class})
public abstract class PlaceCogwheelChainPacketMixin {
    @Unique
    private static final ThreadLocal<Set<BlockPos>> bnt$physicsNodes = ThreadLocal.withInitial(HashSet::new);
    @Unique
    private static final ThreadLocal<Item> bnt$linkItem = ThreadLocal.withInitial(() -> Items.CHAIN);

    @Shadow(
        remap = false
    )
    public abstract PlacingCogwheelChain worldSpacePartialChain();

    @Shadow(
        remap = false
    )
    public abstract int priorityChainTakeHand();

    @Inject(
        method = {"handle"},
        at = {@At("HEAD")},
        remap = false
    )
    private void bnt$capturePlacementContext(ServerPlayer player, CallbackInfo ci) {
        Set<BlockPos> positions = bnt$physicsNodes.get();
        positions.clear();
        Level level = player.level();
        HiddenCogwheelCompat.setPlacementLevel(level);

        for (PlacingCogwheelNode node : this.worldSpacePartialChain().getVisitedNodes()) {
            BlockEntity be = level.getBlockEntity(node.pos());
            if (HiddenCogwheelCompat.isPhysicsEnabled(be)) {
                positions.add(node.pos().immutable());
            }
        }

        bnt$linkItem.set(this.bnt$resolveLinkItem(player));
    }

    @Redirect(
        method = {"handle"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/item/Items;CHAIN:Lnet/minecraft/world/item/Item;"
        ),
        remap = false,
        require = 0
    )
    private Item bnt$useSelectedLinkItem() {
        return bnt$linkItem.get();
    }

    @Inject(
        method = {"handle"},
        at = {@At("RETURN")},
        remap = false
    )
    private void bnt$restorePlacementContext(ServerPlayer player, CallbackInfo ci) {
        Set<BlockPos> positions = bnt$physicsNodes.get();

        try {
            Level level = player.level();

            for (BlockPos pos : positions) {
                if (level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
                    access.bnt$setPhysicsEnabled(true);
                }
            }
        } finally {
            positions.clear();
            bnt$linkItem.remove();
            HiddenCogwheelCompat.setPlacementLevel(null);
        }
    }

    @Unique
    private Item bnt$resolveLinkItem(ServerPlayer player) {
        InteractionHand preferredHand = this.priorityChainTakeHand() == InteractionHand.OFF_HAND.ordinal()
            ? InteractionHand.OFF_HAND
            : InteractionHand.MAIN_HAND;
        ItemStack preferredStack = player.getItemInHand(preferredHand);
        if (preferredStack.is((Item)AllItems.BELT_CONNECTOR.get())) {
            return (Item)AllItems.BELT_CONNECTOR.get();
        } else if (preferredStack.is((Item)BitsNTracksItems.INDUSTRIAL_BELT.get())) {
            return (Item)BitsNTracksItems.INDUSTRIAL_BELT.get();
        } else if (preferredStack.is(Items.CHAIN)) {
            return Items.CHAIN;
        } else {
            InteractionHand otherHand = preferredHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack otherStack = player.getItemInHand(otherHand);
            if (otherStack.is((Item)AllItems.BELT_CONNECTOR.get())) {
                return (Item)AllItems.BELT_CONNECTOR.get();
            } else if (otherStack.is((Item)BitsNTracksItems.INDUSTRIAL_BELT.get())) {
                return (Item)BitsNTracksItems.INDUSTRIAL_BELT.get();
            } else {
                return otherStack.is(Items.CHAIN) ? Items.CHAIN : Items.CHAIN;
            }
        }
    }
}
