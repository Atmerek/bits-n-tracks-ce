package dev.qwxon.bitsntracks.physics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class CogwheelSizeHelper {
    private static final Set<String> COGWHEEL_NAMESPACES = Set.of("create", "bits_n_bobs", "bits_n_tracks");
    private static final Map<Block, CogwheelSizeHelper.Size> SIZE_CACHE = new ConcurrentHashMap<>();

    private static final double TINY_CHAIN_RADIUS = 0.25;
    private static final double SMALL_CHAIN_RADIUS = 0.5;
    private static final double MEDIUM_CHAIN_RADIUS = 0.75;
    private static final double LARGE_CHAIN_RADIUS = 1.0;

    private CogwheelSizeHelper() {
    }

    private static CogwheelSizeHelper.Size sizeOf(Block block) {
        return SIZE_CACHE.computeIfAbsent(block, CogwheelSizeHelper::classify);
    }

    private static CogwheelSizeHelper.Size classify(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !COGWHEEL_NAMESPACES.contains(id.getNamespace())) {
            return CogwheelSizeHelper.Size.SMALL;
        } else {
            String path = id.getPath();
            if (path.startsWith("large_")) {
                return CogwheelSizeHelper.Size.LARGE;
            } else if (path.startsWith("medium_")) {
                return CogwheelSizeHelper.Size.MEDIUM;
            } else {
                return path.startsWith("tiny_") || path.contains("_tiny_")
                    ? CogwheelSizeHelper.Size.TINY
                    : CogwheelSizeHelper.Size.SMALL;
            }
        }
    }

    public static boolean isLarge(Block block) {
        return sizeOf(block) == CogwheelSizeHelper.Size.LARGE;
    }

    public static boolean isMedium(Block block) {
        return sizeOf(block) == CogwheelSizeHelper.Size.MEDIUM;
    }

    public static boolean isTiny(Block block) {
        return sizeOf(block) == CogwheelSizeHelper.Size.TINY;
    }

    public static double getRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeCollisionRadius();
            case MEDIUM -> BntPhysicsTuning.getMediumCollisionRadius();
            case TINY -> BntPhysicsTuning.getTinyCollisionRadius();
            case SMALL -> BntPhysicsTuning.getSmallCollisionRadius();
        };
    }

    public static double getChainRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> LARGE_CHAIN_RADIUS;
            case MEDIUM -> MEDIUM_CHAIN_RADIUS;
            case TINY -> TINY_CHAIN_RADIUS;
            case SMALL -> SMALL_CHAIN_RADIUS;
        };
    }

    public static double getChainRadius(boolean isLarge, boolean hasSmallCogwheelOffset) {
        if (isLarge) {
            return hasSmallCogwheelOffset ? MEDIUM_CHAIN_RADIUS : LARGE_CHAIN_RADIUS;
        } else {
            return hasSmallCogwheelOffset ? SMALL_CHAIN_RADIUS : TINY_CHAIN_RADIUS;
        }
    }

    public static double getToolHighlightRadius(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeToolHighlightRadius();
            case MEDIUM -> BntPhysicsTuning.getMediumToolHighlightRadius();
            case TINY -> BntPhysicsTuning.getTinyToolHighlightRadius();
            case SMALL -> BntPhysicsTuning.getSmallToolHighlightRadius();
        };
    }

    public static double getSuspensionRest(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeSuspensionRest();
            case MEDIUM -> BntPhysicsTuning.getMediumSuspensionRest();
            case TINY -> BntPhysicsTuning.getTinySuspensionRest();
            case SMALL -> BntPhysicsTuning.getSmallSuspensionRest();
        };
    }

    public static double getVerticalOffset(Block block) {
        return switch (sizeOf(block)) {
            case LARGE -> BntPhysicsTuning.getLargeVerticalOffset();
            case MEDIUM -> BntPhysicsTuning.getMediumVerticalOffset();
            case TINY -> BntPhysicsTuning.getTinyVerticalOffset();
            case SMALL -> BntPhysicsTuning.getSmallVerticalOffset();
        };
    }

    private enum Size {
        TINY,
        SMALL,
        MEDIUM,
        LARGE
    }
}
