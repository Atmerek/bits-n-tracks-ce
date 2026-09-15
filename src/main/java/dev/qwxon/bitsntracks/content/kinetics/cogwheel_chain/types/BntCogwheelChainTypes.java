package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.Builder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.registry.core.BnbResourceKeys;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BntCogwheelChainTypes {
    private static final TagKey<Block> BNB_FLANGED_COGWHEEL =
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bits_n_bobs", "flanged_cogwheel"));

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
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();
        return path.equals("large_industrial_flanged_cogwheel")
            || path.equals("medium_industrial_flanged_cogwheel")
            || path.equals("large_hidden_flanged_cogwheel")
            || path.equals("industrial_flanged_cogwheel")
            || path.equals("small_hidden_flanged_cogwheel")
            || path.equals("medium_hidden_flanged_cogwheel")
            || id.toString().equals("dndecor:industrial_cogwheel")
            || id.toString().equals("dndecor:medium_industrial_cogwheel")
            || id.toString().equals("dndecor:large_industrial_cogwheel");
    }

    /** Message key refusing the chain here, or null. */
    public static String refusal(CogwheelChainType type, BlockGetter level, BlockPos pos, BlockState state) {
        if (type.getRenderType() != ChainRenderInfo.BELT) {
            return null;
        }
        Block original = originalBlock(level, pos, state);
        return BuiltInRegistries.BLOCK.getKey(original).getNamespace().equals("bits_n_bobs")
            && original.defaultBlockState().is(BNB_FLANGED_COGWHEEL)
            ? "belt_on_bnb_flanged_cogwheel"
            : null;
    }

    private static Block originalBlock(BlockGetter level, BlockPos pos, BlockState state) {
        if (level != null && HiddenCogwheelCompat.isHiddenCogwheel(state)
            && level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
            String id = access.bnt$getOriginalBlock();
            Block original = id == null || id.isEmpty() ? Blocks.AIR : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            if (original != Blocks.AIR) {
                return original;
            }
        }
        return state.getBlock();
    }

    public static void init(IEventBus bus) {
        REGISTRY.register(bus);
    }
}
