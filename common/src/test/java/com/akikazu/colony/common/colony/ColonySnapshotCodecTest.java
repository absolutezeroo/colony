package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.colony.ColonyId;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonySnapshotCodecTest
{
    @Test
    void roundtripsThroughCodec()
    {
        ColonySnapshot original = ColonySnapshot.of(
                ColonyId.random(),
                "TestColony",
                new BlockPos(123, 64, -456));

        JsonElement encoded = ColonySnapshot.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ColonySnapshot decoded = ColonySnapshot.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, decoded);
        assertEquals(ColonySnapshot.CURRENT_VERSION, decoded.dataVersion());
    }

    @Test
    void encodesCurrentVersionInJson()
    {
        ColonySnapshot snapshot = ColonySnapshot.of(
                ColonyId.random(),
                "Encoded",
                BlockPos.ZERO);

        JsonElement encoded = ColonySnapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow();

        assertTrue(encoded.isJsonObject());
        JsonObject obj = encoded.getAsJsonObject();
        assertEquals(ColonySnapshot.CURRENT_VERSION, obj.get("dataVersion").getAsInt());
        assertEquals("Encoded", obj.get("name").getAsString());
    }

    @Test
    void rejectsMissingDataVersion()
    {
        JsonObject malformed = new JsonObject();
        malformed.addProperty("name", "NoVersion");

        DataResult<ColonySnapshot> result = ColonySnapshot.CODEC.parse(JsonOps.INSTANCE, malformed);

        assertTrue(result.isError(), "missing dataVersion should fail to decode");
    }
}
