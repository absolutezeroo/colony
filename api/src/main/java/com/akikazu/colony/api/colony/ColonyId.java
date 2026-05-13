package com.akikazu.colony.api.colony;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

/**
 * Stable identity for a colony. Backed by a {@link UUID} that is generated once at creation and never mutates through
 * the colony's lifetime — including across save migrations.
 */
public record ColonyId(UUID value)
{
    public static final Codec<ColonyId> CODEC = UUIDUtil.CODEC.xmap(ColonyId::new, ColonyId::value);

    public static final StreamCodec<ByteBuf, ColonyId> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public ColonyId decode(ByteBuf buf)
        {
            long most = buf.readLong();
            long least = buf.readLong();

            return new ColonyId(new UUID(most, least));
        }

        @Override
        public void encode(ByteBuf buf, ColonyId id)
        {
            UUID uuid = id.value();
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public ColonyId
    {
        Objects.requireNonNull(value, "value");
    }

    public static ColonyId random()
    {
        return new ColonyId(UUID.randomUUID());
    }

    @Override
    public String toString()
    {
        return value.toString();
    }
}
