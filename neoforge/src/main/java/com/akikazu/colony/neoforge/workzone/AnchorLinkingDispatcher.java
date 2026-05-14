package com.akikazu.colony.neoforge.workzone;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.common.workzone.impl.AnchorIndex;
import com.akikazu.colony.common.workzone.impl.AnchorLinkingResult;
import com.akikazu.colony.common.workzone.impl.AnchorLinkingResult.Reason;
import com.akikazu.colony.common.workzone.impl.AnchorLinkingService;
import com.akikazu.colony.common.workzone.impl.AnchorMetadata;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.neoforge.blockentity.ScarecrowBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * Server-side entry point bridging {@link AnchorLinkingService} results to player-visible feedback and block-entity
 * mutation. Lives in {@code :neoforge} because it touches block entities directly; the pure decision logic stays in
 * {@link AnchorLinkingService} so {@code :common} unit tests can cover validation without spinning up a level.
 */
public final class AnchorLinkingDispatcher
{
    private AnchorLinkingDispatcher()
    {
    }

    public static AnchorLinkingResult link(
            ServerLevel level,
            ServerPlayer player,
            AnchorId anchor,
            BuildingId building,
            Identifier slotId)
    {
        AnchorLinkingResult result = AnchorLinkingService.link(level, anchor, building, slotId);
        announce(level, player, result);

        return result;
    }

    public static AnchorLinkingResult unlink(ServerLevel level, ServerPlayer player, AnchorId anchor)
    {
        AnchorLinkingResult result = AnchorLinkingService.unlink(level, anchor);
        announce(level, player, result);

        return result;
    }

    private static void announce(ServerLevel level, ServerPlayer player, AnchorLinkingResult result)
    {
        switch (result)
        {
            case AnchorLinkingResult.Linked linked ->
            {
                applyToBlockEntity(level, linked.anchor(), Optional.of(linked.building()));
                player.sendSystemMessage(Component.translatable(
                        "colony.message.anchor.linked",
                        linked.building().toString().substring(0, 8)));
            }
            case AnchorLinkingResult.Unlinked unlinked ->
            {
                applyToBlockEntity(level, unlinked.anchor(), Optional.empty());
                player.sendSystemMessage(Component.translatable("colony.message.anchor.unlinked"));
            }
            case AnchorLinkingResult.Rejected rejected -> player.sendSystemMessage(rejectionMessage(rejected.reason()));
        }
    }

    private static Component rejectionMessage(Reason reason)
    {
        return switch (reason)
        {
            case NO_ANCHOR -> Component.translatable("colony.message.anchor.not_found");
            case NO_BUILDING -> Component.translatable("colony.message.anchor.building_not_found");
            case WRONG_TYPE -> Component.translatable("colony.message.anchor.wrong_type");
            case OUTSIDE_RANGE -> Component.translatable("colony.message.anchor.outside_range");
            case CAPACITY_FULL -> Component.translatable("colony.message.anchor.capacity_full");
            case ALREADY_LINKED -> Component.translatable("colony.message.anchor.already_linked");
            case UNKNOWN_SLOT -> Component.translatable("colony.message.anchor.unknown_slot");
        };
    }

    private static void applyToBlockEntity(ServerLevel level, AnchorId anchor, Optional<BuildingId> link)
    {
        Optional<AnchorMetadata> meta = AnchorIndex.get(level).find(anchor);

        if (meta.isEmpty())
        {
            return;
        }

        BlockPos pos = meta.get().position();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ScarecrowBlockEntity scarecrow)
        {
            scarecrow.setLinkedBuilding(link.orElse(null));
        }
    }
}
