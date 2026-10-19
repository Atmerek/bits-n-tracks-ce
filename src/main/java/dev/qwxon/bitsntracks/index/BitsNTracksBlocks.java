package dev.qwxon.bitsntracks.index;

import com.kipti.bnb.registry.core.BnbTags.BnbBlockTags;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.BntHeadBlock;
import dev.qwxon.bitsntracks.content.BntHeadBlockItem;
import dev.qwxon.bitsntracks.content.CogwheelMaterial;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class BitsNTracksBlocks {
    public static final BlockEntry<HiddenCogwheelBlock> TINY_HIDDEN_FLANGED_COGWHEEL = hidden("tiny_hidden_flanged_cogwheel", CogwheelSize.TINY);
    public static final BlockEntry<BntFlangedCogwheelBlock> TINY_FLANGED_COGWHEEL =
        flanged("tiny_flanged_cogwheel", CogwheelSize.TINY, CogwheelMaterial.WOODEN);
    public static final BlockEntry<BntFlangedCogwheelBlock> INTERMEDIATE_FLANGED_COGWHEEL =
        flanged("intermediate_flanged_cogwheel", CogwheelSize.INTERMEDIATE, CogwheelMaterial.WOODEN);
    public static final BlockEntry<BntFlangedCogwheelBlock> INDUSTRIAL_TINY_FLANGED_COGWHEEL =
        flanged("industrial_tiny_flanged_cogwheel", CogwheelSize.TINY, CogwheelMaterial.INDUSTRIAL);
    public static final BlockEntry<BntFlangedCogwheelBlock> INTERMEDIATE_INDUSTRIAL_FLANGED_COGWHEEL =
        flanged("intermediate_industrial_flanged_cogwheel", CogwheelSize.INTERMEDIATE, CogwheelMaterial.INDUSTRIAL);
    public static final BlockEntry<HiddenCogwheelBlock> SMALL_HIDDEN_FLANGED_COGWHEEL = hidden("small_hidden_flanged_cogwheel", CogwheelSize.SMALL);
    public static final BlockEntry<HiddenCogwheelBlock> LARGE_HIDDEN_FLANGED_COGWHEEL = hidden("large_hidden_flanged_cogwheel", CogwheelSize.LARGE);
    public static final BlockEntry<BntFlangedCogwheelBlock> SMALL_FLANGED_COGWHEEL =
        flanged("flanged_cogwheel", CogwheelSize.SMALL, CogwheelMaterial.WOODEN);
    public static final BlockEntry<BntFlangedCogwheelBlock> LARGE_FLANGED_COGWHEEL =
        flanged("large_flanged_cogwheel", CogwheelSize.LARGE, CogwheelMaterial.WOODEN);
    public static final BlockEntry<BntFlangedCogwheelBlock> INDUSTRIAL_FLANGED_COGWHEEL =
        flanged("industrial_flanged_cogwheel", CogwheelSize.SMALL, CogwheelMaterial.INDUSTRIAL);
    public static final BlockEntry<BntFlangedCogwheelBlock> LARGE_INDUSTRIAL_FLANGED_COGWHEEL =
        flanged("large_industrial_flanged_cogwheel", CogwheelSize.LARGE, CogwheelMaterial.INDUSTRIAL);
    public static final BlockEntry<BntFlangedCogwheelBlock> MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL =
        flanged("medium_industrial_flanged_cogwheel", CogwheelSize.MEDIUM, CogwheelMaterial.INDUSTRIAL);
    public static final BlockEntry<BntFlangedCogwheelBlock> MEDIUM_FLANGED_COGWHEEL =
        flanged("medium_flanged_cogwheel", CogwheelSize.MEDIUM, CogwheelMaterial.WOODEN);
    public static final BlockEntry<BntFlangedCogwheelBlock> MEDIUM_LARGE_FLANGED_COGWHEEL =
        flanged("medium_large_flanged_cogwheel", CogwheelSize.MEDIUM_LARGE, CogwheelMaterial.WOODEN);
    public static final BlockEntry<BntFlangedCogwheelBlock> MEDIUM_LARGE_INDUSTRIAL_FLANGED_COGWHEEL =
        flanged("medium_large_industrial_flanged_cogwheel", CogwheelSize.MEDIUM_LARGE, CogwheelMaterial.INDUSTRIAL);
    public static final BlockEntry<HiddenCogwheelBlock> MEDIUM_HIDDEN_FLANGED_COGWHEEL = hidden("medium_hidden_flanged_cogwheel", CogwheelSize.MEDIUM);
    public static final BlockEntry<BntHeadBlock> QWXONN_HEAD = head("qwxonn_head");
    public static final BlockEntry<BntHeadBlock> ATMEREK_HEAD = head("atmerek_head");
    public static final BlockEntry<BntHeadBlock> CUBESTER_HEAD = head("cubester_head");
    public static final BlockEntry<BntHeadBlock> ALESRR_HEAD = head("alesrr_head");
    public static final BlockEntry<BntHeadBlock> SPECIAL_THANKS_HEAD = head("special_thanks_head");

    private static BlockEntry<BntFlangedCogwheelBlock> flanged(String name, CogwheelSize size, CogwheelMaterial material) {
        boolean industrial = material == CogwheelMaterial.INDUSTRIAL;
        return BitsNTracks.REGISTRATE
            .block(name, p -> new BntFlangedCogwheelBlock(p, size, material))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(industrial ? MapColor.METAL : MapColor.DIRT).noOcclusion())
            .properties(p -> industrial ? p.sound(SoundType.NETHERITE_BLOCK) : p)
            .tag(BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag)
            .item()
            .build()
            .blockstate((c, p) -> {})
            .register();
    }

    private static BlockEntry<HiddenCogwheelBlock> hidden(String name, CogwheelSize size) {
        return BitsNTracks.REGISTRATE
            .block(name, p -> new HiddenCogwheelBlock(p, size))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT).noOcclusion())
            .tag(BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag)
            .blockstate((c, p) -> {})
            .register();
    }

    private static BlockEntry<BntHeadBlock> head(String name) {
        return BitsNTracks.REGISTRATE
            .block(name, BntHeadBlock::new)
            .initialProperties(() -> Blocks.SKELETON_SKULL)
            .properties(p -> p.sound(SoundType.BONE_BLOCK).mapColor(MapColor.WOOL))
            .properties(p -> p.strength(1.0F).noOcclusion().pushReaction(PushReaction.DESTROY))
            .item(BntHeadBlockItem::new)
            .build()
            .blockstate((c, p) -> {})
            .register();
    }

    public static void init() {
    }
}
