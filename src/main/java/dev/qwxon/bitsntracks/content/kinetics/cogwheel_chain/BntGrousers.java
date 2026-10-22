package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class BntGrousers {
    private static final double BLOCKS_PER_GROUSER = 1.0;
    private static final int LINKS_PER_GROUSER = 2;

    private BntGrousers() {
    }

    public static boolean accepts(@Nullable CogwheelChain chain) {
        return chain != null && chain.getChainType().getRenderType() == ChainRenderInfo.BELT
            && chain.getChainType() != BntCogwheelChainTypes.TANK_TREAD_CHAIN.get();
    }

    public static boolean strip(Level level, BlockPos controllerPos, Player player) {
        int grousers = at(level, controllerPos);
        if (grousers <= 0) {
            return false;
        }
        write(level, controllerPos, 0);
        if (!player.hasInfiniteMaterials()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(BitsNTracksItems.GROUSER.get(), grousers));
        }
        level.playSound(null, controllerPos, SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public static int at(Level level, BlockPos pos) {
        int grousers = 0;
        for (BlockPos nodePos : BntBeltTension.chainPositions(level, pos)) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess access) {
                grousers = Math.max(grousers, access.bnt$getGrousers());
            }
        }
        return grousers;
    }

    public static boolean fitted(@Nullable BlockEntity controller) {
        return controller instanceof KineticBlockEntityPhysicsAccess access && access.bnt$getGrousers() > 0;
    }

    public static int cost(Level level, BlockPos controllerPos) {
        return Math.max(1, Mth.ceil(BntBeltLinks.fitLength(level, controllerPos) / BLOCKS_PER_GROUSER));
    }

    public static double spacing() {
        return BntBeltLinks.pitch() * LINKS_PER_GROUSER;
    }

    public static void write(Level level, BlockPos pos, int grousers) {
        for (BlockPos nodePos : BntBeltTension.chainPositions(level, pos)) {
            BlockEntity be = level.getBlockEntity(nodePos);
            if (be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setGrousers(grousers);
                kinetic.setChanged();
                kinetic.sendData();
            }
        }
    }

    public static void destroyed(CogwheelChainBehaviour behaviour) {
        Level level = behaviour.getLevel();
        BlockPos controllerPos = BntChains.controllerPos(behaviour);
        if (level == null || level.isClientSide || controllerPos == null || BntChains.chainAt(level, controllerPos) == null) {
            return;
        }
        int grousers = at(level, controllerPos);
        if (grousers <= 0) {
            return;
        }
        write(level, controllerPos, 0);
        Block.popResource(level, behaviour.getPos(), new ItemStack(BitsNTracksItems.GROUSER.get(), grousers));
    }
}
