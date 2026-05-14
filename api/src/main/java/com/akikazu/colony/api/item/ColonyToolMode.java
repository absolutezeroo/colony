package com.akikazu.colony.api.item;

import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import io.netty.buffer.ByteBuf;

/**
 * Operation mode of the Colony Tool. See {@code docs/04-BUILDING-SYSTEM.md} "Colony Tool" — modes are cycled via
 * shift+scroll and determine which interaction the tool performs on right-click.
 *
 * <p>
 * Lives in {@code :api} because both the {@code :neoforge} item code and the C2S mode-cycle payload reference it.
 */
public enum ColonyToolMode implements StringRepresentable
{
    ZONE("zone"), STORAGE("storage"), LINK("link"), INSPECT("inspect");

    public static final ColonyToolMode DEFAULT = ZONE;

    public static final Codec<ColonyToolMode> CODEC = StringRepresentable.fromEnum(ColonyToolMode::values);

    public static final StreamCodec<ByteBuf, ColonyToolMode> STREAM_CODEC = ByteBufCodecs
            .idMapper(ColonyToolMode::byOrdinal, ColonyToolMode::networkId);

    private final String name;

    ColonyToolMode(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String getSerializedName()
    {
        return name;
    }

    @SuppressWarnings("EnumOrdinal")
    public ColonyToolMode next()
    {
        ColonyToolMode[] values = values();

        return values[(ordinal() + 1) % values.length];
    }

    @SuppressWarnings("EnumOrdinal")
    public ColonyToolMode previous()
    {
        ColonyToolMode[] values = values();

        return values[(ordinal() - 1 + values.length) % values.length];
    }

    public static ColonyToolMode byOrdinal(int ordinal)
    {
        ColonyToolMode[] values = values();

        if (ordinal < 0 || ordinal >= values.length)
        {
            return DEFAULT;
        }

        return values[ordinal];
    }

    @SuppressWarnings("EnumOrdinal")
    private int networkId()
    {
        return ordinal();
    }
}
