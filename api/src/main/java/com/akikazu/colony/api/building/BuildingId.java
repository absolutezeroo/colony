package com.akikazu.colony.api.building;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

/**
 * Stable identity for a building. Backed by a {@link UUID} that is generated at designation and never mutates through
 * the building's lifetime — including across save migrations.
 */
public record BuildingId(UUID value)
{
    public static final Codec<BuildingId> CODEC = UUIDUtil.CODEC.xmap(BuildingId::new, BuildingId::value);

    public static final StreamCodec<ByteBuf, BuildingId> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public BuildingId decode(ByteBuf buf)
        {
            long most = buf.readLong();
            long least = buf.readLong();

            return new BuildingId(new UUID(most, least));
        }

        @Override
        public void encode(ByteBuf buf, BuildingId id)
        {
            UUID uuid = id.value();
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public BuildingId
    {
        Objects.requireNonNull(value, "value");
    }

    public static BuildingId random()
    {
        return new BuildingId(UUID.randomUUID());
    }

    @Override
    public String toString()
    {
        return value.toString();
    }
}
