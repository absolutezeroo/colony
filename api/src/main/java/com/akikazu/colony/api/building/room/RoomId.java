package com.akikazu.colony.api.building.room;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

/**
 * Stable identity for a designated {@link Room}. Mirrors the shape of {@link com.akikazu.colony.api.building.BuildingId
 * BuildingId} and {@link com.akikazu.colony.api.colony.ColonyId ColonyId} so persistence, networking, and addon code
 * can round-trip room references without re-deriving them from {@link Room#id()}.
 */
public record RoomId(UUID value)
{
    public static final Codec<RoomId> CODEC = UUIDUtil.CODEC.xmap(RoomId::new, RoomId::value);

    public static final StreamCodec<ByteBuf, RoomId> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public RoomId decode(ByteBuf buf)
        {
            long most = buf.readLong();
            long least = buf.readLong();

            return new RoomId(new UUID(most, least));
        }

        @Override
        public void encode(ByteBuf buf, RoomId id)
        {
            UUID uuid = id.value();
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public RoomId
    {
        Objects.requireNonNull(value, "value");
    }

    public static RoomId random()
    {
        return new RoomId(UUID.randomUUID());
    }

    @Override
    public String toString()
    {
        return "RoomId(" + value + ")";
    }
}
