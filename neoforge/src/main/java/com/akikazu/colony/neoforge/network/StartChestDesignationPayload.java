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
 * Client → server: the player clicked "Designate Chests" on the Storage tab of a Building screen, arming this
 * {@code (building, slotId)} pair as the active slot for their next right-click in STORAGE mode. The server stores the
 * pair on {@link com.akikazu.colony.common.storage.impl.SlotSelectionManager} and answers with an
 * {@link ActivateStorageModePayload} so the client closes its screen and the tool snaps into STORAGE mode.
 */
public record StartChestDesignationPayload(BuildingId building, Identifier slotId) implements CustomPacketPayload
{
    public static final Type<StartChestDesignationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "start_chest_designation"));

    public static final StreamCodec<ByteBuf, StartChestDesignationPayload> STREAM_CODEC = StreamCodec.of(
            StartChestDesignationPayload::encode,
            StartChestDesignationPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, StartChestDesignationPayload payload)
    {
        BuildingId.STREAM_CODEC.encode(buf, payload.building());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().namespace());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().path());
    }

    private static StartChestDesignationPayload decode(ByteBuf buf)
    {
        BuildingId building = BuildingId.STREAM_CODEC.decode(buf);
        String ns = ByteBufCodecs.STRING_UTF8.decode(buf);
        String path = ByteBufCodecs.STRING_UTF8.decode(buf);

        return new StartChestDesignationPayload(building, Identifier.of(ns, path));
    }
}
