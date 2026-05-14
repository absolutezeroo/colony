package com.akikazu.colony.common.storage.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.storage.StorageSlot;
import com.akikazu.colony.common.building.BuildingIndex;
import com.akikazu.colony.common.building.BuildingMetadata;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Reason;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-side workflow for assigning a vanilla chest to a Storage Slot declared by a Building's
 * {@link com.akikazu.colony.api.building.hut.HutType HutType}.
 *
 * <p>
 * Pure logic: takes a {@link ServerLevel} for state lookup and returns a {@link ChestTypingResult}, with no direct
 * networking. The neoforge tier wires the result into chat feedback and the {@code ChestTypedClientPayload} particle
 * indicator. V1 hardcodes the building tier to 1 (Basic) because tier evaluation has not landed yet; once
 * {@code BuildingTierEvaluator} produces a tier this constant moves to whatever the building's evaluated tier is at the
 * time of typing.
 */
public final class ChestTypingService
{
    private static final int V1_TIER = 1;

    private ChestTypingService()
    {
    }

    public static ChestTypingResult assignChest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BuildingId building,
            Identifier slotId)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");

        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof ChestBlock))
        {
            return new ChestTypingResult.Rejected(Reason.NOT_A_CHEST);
        }

        BuildingIndex buildings = BuildingIndex.get(level);
        Optional<BuildingMetadata> metadataOpt = buildings.find(building);

        if (metadataOpt.isEmpty() || !metadataOpt.get().outerZone().contains(pos))
        {
            return new ChestTypingResult.Rejected(Reason.OUTSIDE_BUILDING);
        }

        BuildingMetadata metadata = metadataOpt.get();

        Optional<StorageSlot> slotOpt = metadata.hutType().storageSlots().stream()
                .filter(s -> s.slotId().equals(slotId))
                .findFirst();

        if (slotOpt.isEmpty())
        {
            return new ChestTypingResult.Rejected(Reason.UNKNOWN_SLOT);
        }

        StorageSlot slot = slotOpt.get();

        if (!hasPermission(player, metadata))
        {
            return new ChestTypingResult.Rejected(Reason.NO_PERMISSION);
        }

        ChestTypingIndex index = ChestTypingIndex.get(level);
        Optional<TypedChest> existing = index.findAt(pos);

        if (existing.isPresent())
        {
            TypedChest current = existing.get();

            if (current.building().equals(building) && current.slotId().equals(slotId))
            {
                return new ChestTypingResult.Success(current);
            }

            return new ChestTypingResult.Rejected(Reason.ALREADY_TYPED);
        }

        int currentCount = (int) index.inSlot(building, slotId).count();
        int maxCount = slot.maxChestsAtTier(V1_TIER);

        if (currentCount >= maxCount)
        {
            return new ChestTypingResult.Rejected(Reason.CAPACITY_FULL);
        }

        index.assign(pos, metadata.colony(), building, slotId, slot.acceptedRole());

        TypedChest assigned = index.findAt(pos).orElseThrow();

        return new ChestTypingResult.Success(assigned);
    }

    // TODO(V2-permissions): enforce OFFICER+ on the colony once colony permissions land. V1 has no owner field on
    // ColonyMetadata, so we accept any player who reached this point with the tool. The hook stays here so the call
    // site already returns Rejected(NO_PERMISSION) when the rule is wired.
    private static boolean hasPermission(ServerPlayer player, BuildingMetadata metadata)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(metadata, "metadata");

        return true;
    }
}
