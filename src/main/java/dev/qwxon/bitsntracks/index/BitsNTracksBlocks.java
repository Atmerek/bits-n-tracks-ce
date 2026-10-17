package dev.qwxon.bitsntracks.index;

import com.kipti.bnb.registry.core.BnbTags.BnbBlockTags;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.BntHeadBlock;
import dev.qwxon.bitsntracks.content.BntHeadBlockItem;
import dev.qwxon.bitsntracks.content.CogwheelMaterial;
import dev.qwxon.bitsntracks.content.CogwheelSize;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;

public class BitsNTracksBlocks {
    public static final BlockEntry<HiddenCogwheelBlock> TINY_HIDDEN_FLANGED_COGWHEEL = BitsNTracks.REGISTRATE
        .block("tiny_hidden_flanged_cogwheel", p -> new HiddenCogwheelBlock(p, CogwheelSize.TINY))
        .initialProperties(SharedProperties::wooden)
        .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
        .properties(p -> p.noOcclusion())
        .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> TINY_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("tiny_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.TINY, CogwheelMaterial.WOODEN))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> INTERMEDIATE_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("intermediate_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.INTERMEDIATE, CogwheelMaterial.WOODEN))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> INDUSTRIAL_TINY_FLANGED_COGWHEEL =((BlockBuilder)BitsNTracks.REGISTRATE
            .block("industrial_tiny_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.TINY, CogwheelMaterial.INDUSTRIAL))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> INTERMEDIATE_INDUSTRIAL_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("intermediate_industrial_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.INTERMEDIATE, CogwheelMaterial.INDUSTRIAL))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<HiddenCogwheelBlock> SMALL_HIDDEN_FLANGED_COGWHEEL = BitsNTracks.REGISTRATE
        .block("small_hidden_flanged_cogwheel", p -> new HiddenCogwheelBlock(p, CogwheelSize.SMALL))
        .initialProperties(SharedProperties::wooden)
        .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
        .properties(p -> p.noOcclusion())
        .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
        .register();
    public static final BlockEntry<HiddenCogwheelBlock> LARGE_HIDDEN_FLANGED_COGWHEEL = BitsNTracks.REGISTRATE
        .block("large_hidden_flanged_cogwheel", p -> new HiddenCogwheelBlock(p, CogwheelSize.LARGE))
        .initialProperties(SharedProperties::wooden)
        .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
        .properties(p -> p.noOcclusion())
        .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> SMALL_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.SMALL, CogwheelMaterial.WOODEN))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> LARGE_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("large_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.LARGE, CogwheelMaterial.WOODEN))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> INDUSTRIAL_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("industrial_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.SMALL, CogwheelMaterial.INDUSTRIAL))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> LARGE_INDUSTRIAL_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("large_industrial_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.LARGE, CogwheelMaterial.INDUSTRIAL))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("medium_industrial_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.MEDIUM, CogwheelMaterial.INDUSTRIAL))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntFlangedCogwheelBlock> MEDIUM_FLANGED_COGWHEEL = ((BlockBuilder)BitsNTracks.REGISTRATE
            .block("medium_flanged_cogwheel", p -> new BntFlangedCogwheelBlock(p, CogwheelSize.MEDIUM, CogwheelMaterial.WOODEN))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .properties(p -> p.noOcclusion())
            .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
            .item()
            .build())
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<HiddenCogwheelBlock> MEDIUM_HIDDEN_FLANGED_COGWHEEL = BitsNTracks.REGISTRATE
        .block("medium_hidden_flanged_cogwheel", p -> new HiddenCogwheelBlock(p, CogwheelSize.MEDIUM))
        .initialProperties(SharedProperties::wooden)
        .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
        .properties(p -> p.noOcclusion())
        .tag(new TagKey[]{BnbBlockTags.COGWHEEL_CHAIN_NO_SMALL_OFFSET.tag})
        .blockstate((c, p) -> {})
        .register();
    public static final BlockEntry<BntHeadBlock> QWXONN_HEAD = head("qwxonn_head");
    public static final BlockEntry<BntHeadBlock> ATMEREK_HEAD = head("atmerek_head");
    public static final BlockEntry<BntHeadBlock> CUBESTER_HEAD = head("cubester_head");
    public static final BlockEntry<BntHeadBlock> ALESRR_HEAD = head("alesrr_head");
    public static final BlockEntry<BntHeadBlock> SPECIAL_THANKS_HEAD = head("special_thanks_head");

    private static BlockEntry<BntHeadBlock> head(String name) {
        return ((BlockBuilder)BitsNTracks.REGISTRATE
                .block(name, BntHeadBlock::new)
                .initialProperties(() -> Blocks.SKELETON_SKULL)
                .properties(p -> p.sound(SoundType.BONE_BLOCK).mapColor(MapColor.WOOL))
                .properties(p -> p.strength(1.0F).noOcclusion().pushReaction(PushReaction.DESTROY))
                .item(BntHeadBlockItem::new)
                .build())
            .blockstate((c, p) -> {})
            .register();
    }

    public static void init() {
    }
}
