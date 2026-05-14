package com.akikazu.colony.common.workzone.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.api.workzone.AnchorSlot;
import com.akikazu.colony.common.building.BuildingIndex;
import com.akikazu.colony.common.building.BuildingMetadata;
import com.akikazu.colony.common.workzone.impl.AnchorLinkingResult.Reason;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-side workflow for linking and unlinking anchors to {@link AnchorSlot anchor slots} declared by a
 * {@link com.akikazu.colony.api.building.hut.HutType HutType}.
 *
 * <p>
 * Pure validation logic: takes a {@link ServerLevel} for state lookup and returns an {@link AnchorLinkingResult} with
 * no direct networking. The neoforge tier wires the result into chat feedback and block-entity mutation. V1 hardcodes
 * the building tier to 1 (Basic) because tier evaluation has not landed yet — same pattern as
 * {@link com.akikazu.colony.common.storage.impl.ChestTypingService}.
 *
 * <p>
 * Validation rules per {@code docs/04-BUILDING-SYSTEM.md} "Anchors":
 * <ul>
 * <li>Anchor must be registered in {@link AnchorIndex}.</li>
 * <li>Building must exist.</li>
 * <li>The slot must accept the anchor's type.</li>
 * <li>Anchor must be within {@link #MAX_LINK_RANGE_BLOCKS} blocks of the building (chebyshev distance to outer
 * zone).</li>
 * <li>Slot must not be at capacity for the current tier.</li>
 * <li>Anchor must not already be linked to a different building (V1: one anchor → one building).</li>
 * </ul>
 */
public final class AnchorLinkingService
{
    public static final int MAX_LINK_RANGE_BLOCKS = 64;

    private static final int V1_TIER = 1;

    private AnchorLinkingService()
    {
    }

    public static AnchorLinkingResult link(
            ServerLevel level,
            AnchorId anchor,
            BuildingId building,
            Identifier slotId)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");

        AnchorIndex anchors = AnchorIndex.get(level);
        Optional<AnchorMetadata> anchorOpt = anchors.find(anchor);

        if (anchorOpt.isEmpty())
        {
            return new AnchorLinkingResult.Rejected(Reason.NO_ANCHOR);
        }

        AnchorMetadata anchorMeta = anchorOpt.get();

        BuildingIndex buildings = BuildingIndex.get(level);
        Optional<BuildingMetadata> buildingOpt = buildings.find(building);

        if (buildingOpt.isEmpty())
        {
            return new AnchorLinkingResult.Rejected(Reason.NO_BUILDING);
        }

        BuildingMetadata buildingMeta = buildingOpt.get();

        Optional<AnchorSlot> slotOpt = buildingMeta.hutType().anchorSlots().stream()
                .filter(s -> s.slotId().equals(slotId))
                .findFirst();

        if (slotOpt.isEmpty())
        {
            return new AnchorLinkingResult.Rejected(Reason.UNKNOWN_SLOT);
        }

        AnchorSlot slot = slotOpt.get();

        if (!slot.acceptedAnchorType().equals(anchorMeta.type()))
        {
            return new AnchorLinkingResult.Rejected(Reason.WRONG_TYPE);
        }

        if (anchorMeta.linkedBuilding().isPresent() && !anchorMeta.linkedBuilding().get().equals(building))
        {
            return new AnchorLinkingResult.Rejected(Reason.ALREADY_LINKED);
        }

        if (!withinRange(anchorMeta.position(), buildingMeta))
        {
            return new AnchorLinkingResult.Rejected(Reason.OUTSIDE_RANGE);
        }

        int currentCount = (int) anchors.allLinkedTo(building)
                .filter(e -> e.getValue().type().equals(anchorMeta.type()))
                .count();

        int maxCount = slot.maxAtTier(V1_TIER);

        boolean alreadyOnSlot = anchorMeta.linkedBuilding().isPresent()
                && anchorMeta.linkedBuilding().get().equals(building);

        if (!alreadyOnSlot && currentCount >= maxCount)
        {
            return new AnchorLinkingResult.Rejected(Reason.CAPACITY_FULL);
        }

        anchors.updateLink(anchor, Optional.of(building));

        return new AnchorLinkingResult.Linked(anchor, building, slotId);
    }

    public static AnchorLinkingResult unlink(ServerLevel level, AnchorId anchor)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(anchor, "anchor");

        AnchorIndex anchors = AnchorIndex.get(level);
        Optional<AnchorMetadata> anchorOpt = anchors.find(anchor);

        if (anchorOpt.isEmpty())
        {
            return new AnchorLinkingResult.Rejected(Reason.NO_ANCHOR);
        }

        anchors.updateLink(anchor, Optional.empty());

        return new AnchorLinkingResult.Unlinked(anchor);
    }

    private static boolean withinRange(BlockPos anchorPos, BuildingMetadata building)
    {
        BlockPos hut = building.hutPos();
        int dx = Math.abs(anchorPos.getX() - hut.getX());
        int dz = Math.abs(anchorPos.getZ() - hut.getZ());
        int dy = Math.abs(anchorPos.getY() - hut.getY());

        return Math.max(Math.max(dx, dz), dy) <= MAX_LINK_RANGE_BLOCKS;
    }
}
