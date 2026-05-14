package com.akikazu.colony.api.workzone;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

/**
 * Stable identity for a work-zone anchor. Backed by a {@link UUID} generated when the anchor block is first placed,
 * preserved across save migrations so an anchor's linkage to a building survives even when the underlying block entity
 * is unloaded and re-serialized.
 */
public record AnchorId(UUID value)
{
    public static final Codec<AnchorId> CODEC = UUIDUtil.CODEC.xmap(AnchorId::new, AnchorId::value);

    public static final StreamCodec<ByteBuf, AnchorId> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public AnchorId decode(ByteBuf buf)
        {
            long most = buf.readLong();
            long least = buf.readLong();

            return new AnchorId(new UUID(most, least));
        }

        @Override
        public void encode(ByteBuf buf, AnchorId id)
        {
            UUID uuid = id.value();
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public AnchorId
    {
        Objects.requireNonNull(value, "value");
    }

    public static AnchorId random()
    {
        return new AnchorId(UUID.randomUUID());
    }

    @Override
    public String toString()
    {
        return value.toString();
    }
}
