package com.akikazu.colony.neoforge.data;

import com.akikazu.colony.api.building.functional.FunctionalBlockDetector;
import com.akikazu.colony.common.building.functional.FunctionalBlockDetectorRegistry;
import com.akikazu.colony.core.registry.Identifier;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;

/**
 * {@code /reload} listener that re-parses every {@code data/colony/functional_block_detector/*.json} file and swaps the
 * datapack slice of {@link FunctionalBlockDetectorRegistry} atomically.
 *
 * <p>
 * Built-in detectors registered at bootstrap remain in place; only the datapack-loaded slice is replaced. Malformed
 * files are logged and skipped (per {@code docs/06-DATA-DRIVEN.md} §Schemas and validation: one bad JSON does not break
 * the rest).
 */
public final class FunctionalBlockDetectorReloadListener extends SimpleJsonResourceReloadListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionalBlockDetectorReloadListener.class);

    private static final Gson GSON = new Gson();

    private static final String DIRECTORY = "functional_block_detector";

    public FunctionalBlockDetectorReloadListener()
    {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler)
    {
        FunctionalBlockDetectorRegistry registry = FunctionalBlockDetectorRegistry.get();
        List<FunctionalBlockDetector> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet())
        {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            if (!json.isJsonObject() || !json.getAsJsonObject().has("type"))
            {
                LOGGER.error("functional_block_detector {} is missing required \"type\" field", file);

                continue;
            }

            String typeName = json.getAsJsonObject().get("type").getAsString();
            Identifier typeId;

            try
            {
                typeId = Identifier.parse(typeName);
            }
            catch (IllegalArgumentException e)
            {
                LOGGER.error("functional_block_detector {} has invalid type {}: {}", file, typeName, e.getMessage());

                continue;
            }

            Optional<MapCodec<? extends FunctionalBlockDetector>> codec = registry.codecForType(typeId);

            if (codec.isEmpty())
            {
                LOGGER.error("functional_block_detector {} references unknown type {}", file, typeId);

                continue;
            }

            DataResult<? extends FunctionalBlockDetector> decoded = codec.get()
                    .codec()
                    .parse(JsonOps.INSTANCE, json);
            decoded.resultOrPartial(err -> LOGGER.error(
                    "functional_block_detector {} failed to decode: {}", file, err))
                    .ifPresent(loaded::add);
        }

        registry.replaceDatapackDetectors(loaded);
        LOGGER.info("Loaded {} datapack FunctionalBlockDetector(s)", loaded.size());
    }
}
