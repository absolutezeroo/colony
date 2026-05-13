package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.colony.Colony;
import com.akikazu.colony.api.colony.ColonyEvents;
import com.akikazu.colony.api.colony.ColonyId;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Server-side service for colony lifecycle.
 *
 * <p>
 * Wraps the always-loaded {@link ColonyIndex} (the directory of known colonies) with the operations that need to happen
 * atomically when a colony is created: index registration, per-colony NBT write, event dispatch. Per-colony snapshots
 * live in {@code <world>/data/colony/colonies/<uuid>.nbt} so the index stays cheap to load and the heavy roster data
 * can be paged in on demand.
 *
 * <p>
 * Each call to {@link #get(ServerLevel)} returns a lightweight wrapper bound to that level; instances do not hold
 * mutable state of their own — the SavedData and the on-disk files are the source of truth.
 */
public final class ColonyManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyManager.class);

    private static final String COLONIES_RELATIVE_DIR = "data/colony/colonies";

    private final ServerLevel level;

    private final ColonyIndex index;

    private ColonyManager(ServerLevel level, ColonyIndex index)
    {
        this.level = level;
        this.index = index;
    }

    public static ColonyManager get(ServerLevel level)
    {
        Objects.requireNonNull(level, "level");

        return new ColonyManager(level, ColonyIndex.get(level));
    }

    public ColonyIndex index()
    {
        return index;
    }

    public Optional<ColonyMetadata> find(ColonyId id)
    {
        return index.find(id);
    }

    public Stream<ColonyId> allInDimension(ResourceKey<Level> dimension)
    {
        return index.allInDimension(dimension);
    }

    public int size()
    {
        return index.size();
    }

    public ColonyId createColony(String name, BlockPos townHallPos, UUID founder)
    {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(townHallPos, "townHallPos");
        Objects.requireNonNull(founder, "founder");

        ColonyId id = ColonyId.random();
        ResourceKey<Level> dimension = level.dimension();
        long foundedAtTick = level.getGameTime();

        ColonyMetadata metadata = new ColonyMetadata(dimension, townHallPos, foundedAtTick);

        if (!index.register(id, metadata))
        {
            throw new IllegalStateException("ColonyId collision when registering newly minted id " + id);
        }

        ColonySnapshot snapshot = ColonySnapshot.empty(id, name, townHallPos, dimension, foundedAtTick);
        ColonyImpl colony = ColonyImpl.fromSnapshot(snapshot);

        writeSnapshot(snapshot);

        LOGGER.info(
                "Colony '{}' (id={}) founded by {} at {} in {}",
                name,
                id,
                founder,
                townHallPos,
                dimension.location());

        ColonyEventBus.get().post(new ColonyEvents.ColonyFoundedEvent(colony, founder));

        return id;
    }

    public Optional<Colony> loadFull(ColonyId id)
    {
        Objects.requireNonNull(id, "id");

        return readSnapshot(id).map(ColonyImpl::fromSnapshot);
    }

    public ColonyId registerInitialCitizens(ColonyId id, List<com.akikazu.colony.api.citizen.CitizenId> citizens)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(citizens, "citizens");

        Optional<ColonySnapshot> existing = readSnapshot(id);

        if (existing.isEmpty())
        {
            LOGGER.warn("Cannot record initial citizens for unknown colony {}", id);

            return id;
        }

        ColonySnapshot prev = existing.get();
        ColonySnapshot updated = ColonySnapshot.of(
                prev.id(),
                prev.name(),
                prev.townHallPos(),
                prev.dimension(),
                prev.foundedAtTick(),
                citizens);

        writeSnapshot(updated);

        return id;
    }

    private Path coloniesDirectory()
    {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(COLONIES_RELATIVE_DIR);
    }

    private Path colonyFile(ColonyId id)
    {
        return coloniesDirectory().resolve(id.value().toString() + ".nbt");
    }

    private void writeSnapshot(ColonySnapshot snapshot)
    {
        Path dir = coloniesDirectory();
        Path file = dir.resolve(snapshot.id().value().toString() + ".nbt");

        DataResult<Tag> encoded = ColonySnapshot.CODEC.encodeStart(NbtOps.INSTANCE, snapshot);
        Tag tag = encoded.getOrThrow();

        if (!(tag instanceof CompoundTag compound))
        {
            throw new IllegalStateException(
                    "ColonySnapshot codec produced a non-compound tag: " + tag.getClass().getName());
        }

        try
        {
            Files.createDirectories(dir);
            NbtIo.writeCompressed(compound, file);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(
                    "Failed to write colony snapshot for " + snapshot.id() + " at " + file, e);
        }
    }

    private Optional<ColonySnapshot> readSnapshot(ColonyId id)
    {
        Path file = colonyFile(id);

        if (!Files.isRegularFile(file))
        {
            return Optional.empty();
        }

        try
        {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            ColonySnapshot snapshot = ColonySnapshot.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();

            return Optional.of(snapshot);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(
                    "Failed to read colony snapshot for " + id + " at " + file, e);
        }
    }
}
