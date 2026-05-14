package com.akikazu.colony.common.building.functional;

import com.akikazu.colony.api.building.functional.FunctionalBlockDetector;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable registry of {@link FunctionalBlockDetector}s, indexed both by detector id and by the function each detector
 * targets. Used by {@link com.akikazu.colony.common.building.room.RoomRequirementEvaluator} to discover which detectors
 * apply when counting blocks of a given function.
 *
 * <p>
 * Two sources populate this registry: built-in detectors registered at bootstrap (see
 * {@link com.akikazu.colony.common.bootstrap.ColonyBootstrap}), and datapack-loaded detectors swapped in on
 * {@code /reload}. The {@link #replaceDatapackDetectors} entry point preserves bootstrap entries by accepting a fresh
 * map of datapack detectors — bootstrap detectors stay registered, datapack ones swap atomically.
 */
public final class FunctionalBlockDetectorRegistry
{
    private static final FunctionalBlockDetectorRegistry INSTANCE = new FunctionalBlockDetectorRegistry();

    private final Map<Identifier, MapCodec<? extends FunctionalBlockDetector>> typeCodecs = new LinkedHashMap<>();
    private final Map<Identifier, FunctionalBlockDetector> builtinDetectors = new LinkedHashMap<>();
    private final Map<Identifier, FunctionalBlockDetector> datapackDetectors = new LinkedHashMap<>();

    @Nullable
    private volatile Codec<FunctionalBlockDetector> cachedDispatchCodec;

    private FunctionalBlockDetectorRegistry()
    {
    }

    public static FunctionalBlockDetectorRegistry get()
    {
        return INSTANCE;
    }

    public synchronized void registerType(Identifier typeId, MapCodec<? extends FunctionalBlockDetector> codec)
    {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(codec, "codec");

        if (typeCodecs.containsKey(typeId))
        {
            throw new IllegalStateException(
                    "FunctionalBlockDetector type already registered: " + typeId);
        }

        typeCodecs.put(typeId, codec);
        cachedDispatchCodec = null;
    }

    public synchronized void registerBuiltin(FunctionalBlockDetector detector)
    {
        Objects.requireNonNull(detector, "detector");

        if (builtinDetectors.containsKey(detector.id()))
        {
            throw new IllegalStateException(
                    "Built-in FunctionalBlockDetector already registered: " + detector.id());
        }

        builtinDetectors.put(detector.id(), detector);
    }

    /**
     * Atomically replaces all datapack-loaded detectors with the supplied collection. Built-in detectors are preserved.
     * Called by the {@code /reload} listener after re-parsing every {@code functional_block_detector/*.json} file.
     */
    public synchronized void replaceDatapackDetectors(Collection<FunctionalBlockDetector> detectors)
    {
        Objects.requireNonNull(detectors, "detectors");

        Map<Identifier, FunctionalBlockDetector> fresh = new LinkedHashMap<>();

        for (FunctionalBlockDetector detector : detectors)
        {
            if (fresh.put(detector.id(), detector) != null)
            {
                throw new IllegalStateException(
                        "Duplicate datapack FunctionalBlockDetector id: " + detector.id());
            }
        }

        datapackDetectors.clear();
        datapackDetectors.putAll(fresh);
    }

    public synchronized void clearAll()
    {
        typeCodecs.clear();
        builtinDetectors.clear();
        datapackDetectors.clear();
        cachedDispatchCodec = null;
    }

    public Optional<MapCodec<? extends FunctionalBlockDetector>> codecForType(Identifier typeId)
    {
        Objects.requireNonNull(typeId, "typeId");

        return Optional.ofNullable(typeCodecs.get(typeId));
    }

    public synchronized Map<Identifier, FunctionalBlockDetector> all()
    {
        Map<Identifier, FunctionalBlockDetector> merged = new LinkedHashMap<>(builtinDetectors);
        merged.putAll(datapackDetectors);

        return Collections.unmodifiableMap(merged);
    }

    public synchronized Collection<FunctionalBlockDetector> detectorsForFunction(Identifier function)
    {
        Objects.requireNonNull(function, "function");

        Map<Identifier, FunctionalBlockDetector> merged = all();

        return merged.values().stream()
                .filter(d -> d.detects().equals(function))
                .toList();
    }

    public Codec<FunctionalBlockDetector> dispatchCodec()
    {
        Codec<FunctionalBlockDetector> cached = cachedDispatchCodec;

        if (cached != null)
        {
            return cached;
        }

        synchronized (this)
        {
            if (cachedDispatchCodec == null)
            {
                cachedDispatchCodec = buildDispatchCodec();
            }

            return cachedDispatchCodec;
        }
    }

    private Codec<FunctionalBlockDetector> buildDispatchCodec()
    {
        Codec<Identifier> typeIdCodec = Identifier.CODEC.flatXmap(
                id -> typeCodecs.containsKey(id)
                        ? DataResult.success(id)
                        : DataResult.error(() -> "Unknown FunctionalBlockDetector type: " + id),
                DataResult::success);

        return typeIdCodec.dispatch(
                "type",
                detector -> findTypeIdFor(detector.codec())
                        .orElseThrow(() -> new IllegalStateException(
                                "Detector codec not registered: " + detector.id())),
                this::resolveCodec);
    }

    private Optional<Identifier> findTypeIdFor(MapCodec<? extends FunctionalBlockDetector> codec)
    {
        for (Map.Entry<Identifier, MapCodec<? extends FunctionalBlockDetector>> entry : typeCodecs.entrySet())
        {
            if (entry.getValue() == codec)
            {
                return Optional.of(entry.getKey());
            }
        }

        return Optional.empty();
    }

    private MapCodec<? extends FunctionalBlockDetector> resolveCodec(Identifier typeId)
    {
        MapCodec<? extends FunctionalBlockDetector> codec = typeCodecs.get(typeId);

        if (codec == null)
        {
            throw new IllegalStateException("Unknown FunctionalBlockDetector type: " + typeId);
        }

        return codec;
    }
}
