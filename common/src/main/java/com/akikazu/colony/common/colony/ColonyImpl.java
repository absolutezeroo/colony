package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.api.colony.Colony;
import com.akikazu.colony.api.colony.ColonyId;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/**
 * Default in-memory implementation of {@link Colony}. Constructed by the {@code ColonyManager} when a colony is founded
 * or re-loaded from its per-colony snapshot file; addons do not subclass (see {@link Colony}'s {@code @NonExtendable}
 * contract).
 */
public final class ColonyImpl implements Colony
{
    private final ColonyId id;

    private final String name;

    private final BlockPos townHallPos;

    private final ResourceKey<Level> dimension;

    private final long foundedAtTick;

    private final List<CitizenId> citizens;

    public ColonyImpl(
            ColonyId id,
            String name,
            BlockPos townHallPos,
            ResourceKey<Level> dimension,
            long foundedAtTick,
            List<CitizenId> citizens)
    {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.townHallPos = Objects.requireNonNull(townHallPos, "townHallPos");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.foundedAtTick = foundedAtTick;
        this.citizens = List.copyOf(Objects.requireNonNull(citizens, "citizens"));
    }

    public static ColonyImpl fromSnapshot(ColonySnapshot snapshot)
    {
        return new ColonyImpl(
                snapshot.id(),
                snapshot.name(),
                snapshot.townHallPos(),
                snapshot.dimension(),
                snapshot.foundedAtTick(),
                snapshot.citizens());
    }

    public ColonySnapshot toSnapshot()
    {
        return ColonySnapshot.of(id, name, townHallPos, dimension, foundedAtTick, citizens);
    }

    @Override
    public ColonyId id()
    {
        return id;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public BlockPos townHallPos()
    {
        return townHallPos;
    }

    @Override
    public ResourceKey<Level> dimension()
    {
        return dimension;
    }

    @Override
    public long foundedAtTick()
    {
        return foundedAtTick;
    }

    @Override
    public List<CitizenId> citizens()
    {
        return citizens;
    }
}
