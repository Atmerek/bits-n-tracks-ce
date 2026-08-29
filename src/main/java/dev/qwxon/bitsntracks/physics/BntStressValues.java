package dev.qwxon.bitsntracks.physics;

import com.simibubi.create.api.stress.BlockStressValues;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class BntStressValues {
    private static final Set<String> COGWHEEL_BLOCKS = Set.of(
        "tiny_flanged_cogwheel",
        "flanged_cogwheel",
        "medium_flanged_cogwheel",
        "large_flanged_cogwheel",
        "industrial_tiny_flanged_cogwheel",
        "industrial_flanged_cogwheel",
        "medium_industrial_flanged_cogwheel",
        "large_industrial_flanged_cogwheel",
        "tiny_hidden_flanged_cogwheel",
        "small_hidden_flanged_cogwheel",
        "medium_hidden_flanged_cogwheel",
        "large_hidden_flanged_cogwheel"
    );
    private static boolean registered;

    private BntStressValues() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            BlockStressValues.IMPACTS
                .registerProvider(
                    block -> {
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                        return "bits_n_tracks".equals(id.getNamespace()) && COGWHEEL_BLOCKS.contains(id.getPath())
                            ? () -> BntPhysicsTuning.getStressImpact(block)
                            : null;
                    }
                );
        }
    }
}
