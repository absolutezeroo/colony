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
 * Server → client confirmation that the slot selection was armed. Client closes any open screen, snaps the Colony
 * Tool's mode display to STORAGE, and surfaces the designation HUD prompt for the named slot.
 */
public record ActivateStorageModePayload(BuildingId building, Identifier slotId) implements CustomPacketPayload
{
    public static final Type<ActivateStorageModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "activate_storage_mode"));

    public static final StreamCodec<ByteBuf, ActivateStorageModePayload> STREAM_CODEC = StreamCodec.of(
            ActivateStorageModePayload::encode,
            ActivateStorageModePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, ActivateStorageModePayload payload)
    {
        BuildingId.STREAM_CODEC.encode(buf, payload.building());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().namespace());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().path());
    }

    private static ActivateStorageModePayload decode(ByteBuf buf)
    {
        BuildingId building = BuildingId.STREAM_CODEC.decode(buf);
        String ns = ByteBufCodecs.STRING_UTF8.decode(buf);
        String path = ByteBufCodecs.STRING_UTF8.decode(buf);

        return new ActivateStorageModePayload(building, Identifier.of(ns, path));
    }
}
