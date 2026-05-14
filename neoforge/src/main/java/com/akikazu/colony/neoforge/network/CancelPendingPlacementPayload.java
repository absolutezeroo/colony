package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server payload. The player pressed Esc while a pending hut placement was active; the server discards their
 * {@code PendingPlacement} record.
 */
public record CancelPendingPlacementPayload() implements CustomPacketPayload
{
    public static final CancelPendingPlacementPayload INSTANCE = new CancelPendingPlacementPayload();

    public static final Type<CancelPendingPlacementPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "cancel_pending_placement"));

    public static final StreamCodec<ByteBuf, CancelPendingPlacementPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
