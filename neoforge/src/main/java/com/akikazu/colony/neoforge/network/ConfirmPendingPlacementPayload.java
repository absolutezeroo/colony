package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server payload. The player pressed Enter while a pending hut placement was active; in prompt 2.3 this drives
 * the actual painting validation and block placement. For now the server just acknowledges in chat.
 */
public record ConfirmPendingPlacementPayload() implements CustomPacketPayload
{
    public static final ConfirmPendingPlacementPayload INSTANCE = new ConfirmPendingPlacementPayload();

    public static final Type<ConfirmPendingPlacementPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "confirm_pending_placement"));

    public static final StreamCodec<ByteBuf, ConfirmPendingPlacementPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
