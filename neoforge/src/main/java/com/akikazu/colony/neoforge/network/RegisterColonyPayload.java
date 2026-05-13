package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Debug client → server payload exercising the registration wire. The server-side handler validates and writes through
 * {@link com.akikazu.colony.common.colony.ColonyIndex}.
 */
public record RegisterColonyPayload(ColonyId id, String name, BlockPos pos) implements CustomPacketPayload
{

    public static final Type<RegisterColonyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "register_colony"));

    public static final StreamCodec<ByteBuf, RegisterColonyPayload> STREAM_CODEC = StreamCodec.composite(
            ColonyId.STREAM_CODEC, RegisterColonyPayload::id,
            ByteBufCodecs.STRING_UTF8, RegisterColonyPayload::name,
            BlockPos.STREAM_CODEC, RegisterColonyPayload::pos,
            RegisterColonyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
