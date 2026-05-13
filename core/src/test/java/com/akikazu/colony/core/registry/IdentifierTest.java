package com.akikazu.colony.core.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierTest
{
    @Test
    void should_accept_valid_namespace_and_path()
    {
        Identifier id = Identifier.of("colony", "job/farmer");

        assertEquals("colony", id.namespace());
        assertEquals("job/farmer", id.path());
    }

    @Test
    void toString_returns_namespace_colon_path()
    {
        Identifier id = Identifier.of("colony", "tier_req/min_volume");

        assertEquals("colony:tier_req/min_volume", id.toString());
    }

    @Test
    void parse_accepts_well_formed_string()
    {
        Identifier parsed = Identifier.parse("colony:job/farmer");

        assertEquals(Identifier.of("colony", "job/farmer"), parsed);
    }

    @Test
    void parse_rejects_missing_separator()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("colony"));
    }

    @Test
    void parse_rejects_uppercase_namespace()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("Colony:job"));
    }

    @Test
    void parse_rejects_uppercase_path()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("colony:Job"));
    }

    @Test
    void parse_rejects_empty_namespace()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse(":job"));
    }

    @Test
    void parse_rejects_empty_path()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("colony:"));
    }

    @Test
    void of_rejects_empty_namespace()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.of("", "job"));
    }

    @Test
    void of_rejects_empty_path()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.of("colony", ""));
    }

    @Test
    void of_rejects_invalid_namespace_chars()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.of("colony!", "job"));
    }

    @Test
    void of_rejects_invalid_path_chars()
    {
        assertThrows(IllegalArgumentException.class, () -> Identifier.of("colony", "job!"));
    }

    @Test
    void equals_and_hashCode_are_value_based()
    {
        Identifier a = Identifier.of("colony", "job/farmer");
        Identifier b = Identifier.parse("colony:job/farmer");
        Identifier c = Identifier.of("colony", "job/miner");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
