package com.akikazu.colony.core.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Namespaced identifier used to key entries in a {@link Registry}.
 *
 * <p>
 * Validity: {@code namespace} matches {@code [a-z0-9_.-]+} and {@code path} matches {@code [a-z0-9_./-]+}. Both must be
 * non-empty.
 */
public record Identifier(String namespace, String path)
{
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH_PATTERN = Pattern.compile("[a-z0-9_./-]+");

    public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(
            Identifier::tryParse,
            Identifier::toString);

    public Identifier
    {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");

        if (namespace.isEmpty())
        {
            throw new IllegalArgumentException("namespace must be non-empty");
        }

        if (path.isEmpty())
        {
            throw new IllegalArgumentException("path must be non-empty");
        }

        if (!NAMESPACE_PATTERN.matcher(namespace).matches())
        {
            throw new IllegalArgumentException(
                    "namespace '%s' must match [a-z0-9_.-]+".formatted(namespace));
        }

        if (!PATH_PATTERN.matcher(path).matches())
        {
            throw new IllegalArgumentException(
                    "path '%s' must match [a-z0-9_./-]+".formatted(path));
        }
    }

    public static Identifier of(String namespace, String path)
    {
        return new Identifier(namespace, path);
    }

    public static Identifier parse(String s)
    {
        Objects.requireNonNull(s, "s");
        int colon = s.indexOf(':');

        if (colon < 0)
        {
            throw new IllegalArgumentException(
                    "identifier '%s' must contain ':' separator".formatted(s));
        }

        String ns = s.substring(0, colon);
        String p = s.substring(colon + 1);

        return new Identifier(ns, p);
    }

    private static DataResult<Identifier> tryParse(String s)
    {
        try
        {
            return DataResult.success(parse(s));
        }
        catch (IllegalArgumentException e)
        {
            return DataResult.error(e::getMessage);
        }
    }

    @Override
    public String toString()
    {
        return namespace + ":" + path;
    }
}
