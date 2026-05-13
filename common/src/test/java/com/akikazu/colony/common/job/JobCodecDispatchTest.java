package com.akikazu.colony.common.job;

import com.akikazu.colony.api.job.Job;
import com.akikazu.colony.api.job.JobType;
import com.akikazu.colony.api.registry.ColonyRegistries;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.common.job.impl.IdleJob;
import com.akikazu.colony.common.job.impl.IdleJobType;
import com.akikazu.colony.core.registry.Registry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobCodecDispatchTest
{
    private static Codec<Job> dispatch;

    @BeforeAll
    static void setUp()
    {
        ColonyBootstrap.register();
        Registry<JobType> registry = ColonyBootstrap.jobTypes();
        dispatch = ColonyRegistries.jobDispatchCodec(registry);
    }

    @Test
    void encodesIdleJobAsTypeOnlyObject()
    {
        DataResult<JsonElement> encoded = dispatch.encodeStart(JsonOps.INSTANCE, IdleJob.INSTANCE);

        JsonElement json = encoded.getOrThrow();

        assertTrue(json.isJsonObject(), "encoded form should be a JSON object");
        JsonObject obj = json.getAsJsonObject();
        assertEquals("colony:idle", obj.get("type").getAsString());
    }

    @Test
    void roundTripsIdleJobThroughDispatch()
    {
        JsonElement encoded = dispatch.encodeStart(JsonOps.INSTANCE, IdleJob.INSTANCE).getOrThrow();
        Job decoded = dispatch.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(IdleJob.INSTANCE, decoded);
        assertSame(IdleJobType.INSTANCE, decoded.type());
    }

    @Test
    void rejectsUnknownJobType()
    {
        JsonObject bogus = new JsonObject();
        bogus.addProperty("type", "colony:does_not_exist");

        DataResult<Job> result = dispatch.parse(JsonOps.INSTANCE, bogus);

        assertTrue(result.isError(), "decoding unknown type id should fail");
    }
}
