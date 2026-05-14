package com.akikazu.colony.common.building.functional;

import com.akikazu.colony.api.building.functional.FunctionalBlockDetector;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import java.util.Objects;

/**
 * {@link FunctionalBlockDetector} backed by a vanilla block tag.
 *
 * <p>
 * The detector matches any block whose state {@link net.minecraft.world.level.block.state.BlockState#is(TagKey) is in}
 * the supplied tag. Modpack authors can append to the tag (for modded plush beds, modded doors, etc.) without touching
 * the detector JSON. The codec preserves the original {@code "tag"} string verbatim ({@code "#namespace:path"}) so
 * encode-then-decode is identity.
 */
public final class TaggedBlockDetector implements FunctionalBlockDetector
{
    public static final Identifier TYPE_ID = Identifier.of("colony", "tagged_blocks");

    private static final Codec<TagKey<Block>> BLOCK_TAG_CODEC = Codec.STRING.comapFlatMap(
            TaggedBlockDetector::parseTagString,
            TaggedBlockDetector::encodeTagString);

    public static final MapCodec<TaggedBlockDetector> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(TaggedBlockDetector::id),
            Identifier.CODEC.fieldOf("function").forGetter(TaggedBlockDetector::detects),
            BLOCK_TAG_CODEC.fieldOf("tag").forGetter(TaggedBlockDetector::tag))
            .apply(instance, TaggedBlockDetector::new));

    private final Identifier id;
    private final Identifier function;
    private final TagKey<Block> tag;

    public TaggedBlockDetector(Identifier id, Identifier function, TagKey<Block> tag)
    {
        this.id = Objects.requireNonNull(id, "id");
        this.function = Objects.requireNonNull(function, "function");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    public static TaggedBlockDetector ofBlockTag(Identifier id, Identifier function, TagKey<Block> tag)
    {
        return new TaggedBlockDetector(id, function, tag);
    }

    private static DataResult<TagKey<Block>> parseTagString(String raw)
    {
        String stripped = raw.startsWith("#") ? raw.substring(1) : raw;
        int colon = stripped.indexOf(':');

        if (colon < 0)
        {
            return DataResult.error(() -> "tag '%s' must contain ':' separator".formatted(raw));
        }

        try
        {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    stripped.substring(0, colon),
                    stripped.substring(colon + 1));

            return DataResult.success(TagKey.create(Registries.BLOCK, location));
        }
        catch (RuntimeException e)
        {
            return DataResult.error(() -> "invalid tag '%s': %s".formatted(raw, e.getMessage()));
        }
    }

    private static String encodeTagString(TagKey<Block> tag)
    {
        return "#" + tag.location();
    }

    @Override
    public Identifier id()
    {
        return id;
    }

    @Override
    public Identifier detects()
    {
        return function;
    }

    public TagKey<Block> tag()
    {
        return tag;
    }

    @Override
    public boolean matches(BlockInWorld block)
    {
        Objects.requireNonNull(block, "block");

        return block.getState().is(tag);
    }

    @Override
    public MapCodec<? extends FunctionalBlockDetector> codec()
    {
        return CODEC;
    }
}
