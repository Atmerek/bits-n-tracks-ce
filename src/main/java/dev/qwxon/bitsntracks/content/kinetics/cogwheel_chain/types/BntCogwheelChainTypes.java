package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.Builder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.registry.core.BnbResourceKeys;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BntCogwheelChainTypes {
    public static final DeferredRegister<CogwheelChainType> REGISTRY = DeferredRegister.create(BnbResourceKeys.COGWHEEL_CHAIN_TYPE, "bits_n_tracks");
    public static final DeferredHolder<CogwheelChainType, CogwheelChainType> INDUSTRIAL_BELT_CHAIN = REGISTRY.register(
        "industrial_belt",
        () -> new Builder()
            .relatedItem(BitsNTracksItems.INDUSTRIAL_BELT::get)
            .renderType(ChainRenderInfo.BELT)
            .renderTexture(BitsNTracks.asResource("textures/block/industrial_belt.png"))
            .permitsAxisChange(false)
            .breakEffectsBlock(() -> Blocks.CHAIN)
            .setCogwheelPredicate(BntCogwheelChainTypes::isFlangedDriveCogwheel)
            .build()
    );

    public static final DeferredHolder<CogwheelChainType, CogwheelChainType> TANK_TREAD_CHAIN = REGISTRY.register(
        "tank_tread",
        () -> new Builder()
            .relatedItem(BitsNTracksItems.TANK_TREAD::get)
            .renderType(ChainRenderInfo.BELT)
            .renderTexture(BitsNTracks.asResource("textures/block/tank_tread.png"))
            .permitsAxisChange(false)
            .breakEffectsBlock(() -> Blocks.CHAIN)
            .setCogwheelPredicate(BntCogwheelChainTypes::isFlangedDriveCogwheel)
            .build()
    );

    /** Cogwheels a belt or tread may be strung on. */
    public static boolean isFlangedDriveCogwheel(Block block) {
        return block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock;
    }

    /** Message key refusing the chain here, or null. */
    public static String refusal(CogwheelChainType type, BlockGetter level, BlockPos pos, BlockState state) {
        if (partnerCarriesChain(level, pos)) {
            return "chain_on_wide_partner";
        }
        boolean own = HiddenCogwheelCompat.isOwnCogwheel(state, level == null ? null : level.getBlockEntity(pos));
        if (type == INDUSTRIAL_BELT_CHAIN.get() || type == TANK_TREAD_CHAIN.get()) {
            return own ? null : "belt_on_bnb_flanged_cogwheel";
        }
        return own && type.getRenderType() != ChainRenderInfo.BELT ? "chain_on_physical_cogwheel" : null;
    }

    /** One track per wide cogwheel, so its other half may not carry a second. */
    private static boolean partnerCarriesChain(BlockGetter level, BlockPos pos) {
        BlockPos partner = level == null ? null : BntCogwheelPairing.partnerPos(level, pos);
        return partner != null && BntChainEngagement.partOfChain(level.getBlockEntity(partner));
    }

    public static void init(IEventBus bus) {
        REGISTRY.register(bus);
    }
}
