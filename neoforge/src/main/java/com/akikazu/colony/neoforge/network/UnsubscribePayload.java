package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server opt-out for the delta stream of a given {@link ColonyId}.
 *
 * <p>
 * Counterpart to {@link SubscribePayload}; sent when the client closes a Colony GUI. The server drops the subscription
 * via {@link ColonySubscriptionService} and stops pushing deltas for that colony to this player.
 */
public record UnsubscribePayload(ColonyId colony) implements CustomPacketPayload
{

    public static final Type<UnsubscribePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "unsubscribe"));

    public static final StreamCodec<ByteBuf, UnsubscribePayload> STREAM_CODEC = StreamCodec.composite(
            ColonyId.STREAM_CODEC, UnsubscribePayload::colony,
            UnsubscribePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
