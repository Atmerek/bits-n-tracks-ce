package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.Builder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import com.kipti.bnb.registry.core.BnbResourceKeys;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
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
            .setCogwheelPredicate(
                block -> {
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
            )
            .build()
    );

    public static void init(IEventBus bus) {
        REGISTRY.register(bus);
    }
}
