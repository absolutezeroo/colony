package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Server → client: confirms that the player has been armed for anchor linking. The client closes its building screen
 * (if any), switches the Colony Tool to LINK mode, and shows the LINK HUD prompt.
 */
public record ActivateLinkModePayload(BuildingId building, Identifier slotId) implements CustomPacketPayload
{
    public static final Type<ActivateLinkModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "activate_link_mode"));

    public static final StreamCodec<ByteBuf, ActivateLinkModePayload> STREAM_CODEC = StreamCodec.of(
            ActivateLinkModePayload::encode,
            ActivateLinkModePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, ActivateLinkModePayload payload)
    {
        BuildingId.STREAM_CODEC.encode(buf, payload.building());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().namespace());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().path());
    }

    private static ActivateLinkModePayload decode(ByteBuf buf)
    {
        BuildingId building = BuildingId.STREAM_CODEC.decode(buf);
        String ns = ByteBufCodecs.STRING_UTF8.decode(buf);
        String path = ByteBufCodecs.STRING_UTF8.decode(buf);

        return new ActivateLinkModePayload(building, Identifier.of(ns, path));
    }
}
