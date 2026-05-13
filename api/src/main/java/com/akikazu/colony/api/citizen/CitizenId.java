package com.akikazu.colony.api.citizen;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

import io.netty.buffer.ByteBuf;

/**
 * Stable identity for a citizen. Backed by a {@link UUID} that is generated at spawn and never mutates through the
 * citizen's lifetime — including across save migrations.
 */
public record CitizenId(UUID value)
{
    public static final Codec<CitizenId> CODEC = UUIDUtil.CODEC.xmap(CitizenId::new, CitizenId::value);

    public static final StreamCodec<ByteBuf, CitizenId> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public CitizenId decode(ByteBuf buf)
        {
            long most = buf.readLong();
            long least = buf.readLong();

            return new CitizenId(new UUID(most, least));
        }

        @Override
        public void encode(ByteBuf buf, CitizenId id)
        {
            UUID uuid = id.value();
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public CitizenId
    {
        if (value == null)
        {
            throw new IllegalArgumentException("CitizenId cannot wrap null UUID");
        }
    }

    public static CitizenId random()
    {
        return new CitizenId(UUID.randomUUID());
    }

    @Override
    public String toString()
    {
        return "CitizenId(" + value + ")";
    }
}
