package com.akikazu.colony.api.storage;

import com.akikazu.colony.core.registry.Identifier;

/**
 * Built-in {@link StorageRoleType} identifiers. The actual {@link StorageRoleType} instances live in {@code :common}
 * and are registered into {@link com.akikazu.colony.api.registry.ColonyRegistries#STORAGE_ROLE_TYPE} at mod init; this
 * class is the loader-agnostic surface that {@link StorageSlot} declarations and JSON content can reference by id.
 */
public final class StorageRoles
{
    public static final Identifier INPUT = Identifier.of("colony", "storage_role/input");

    public static final Identifier OUTPUT = Identifier.of("colony", "storage_role/output");

    public static final Identifier TOOLS = Identifier.of("colony", "storage_role/tools");

    public static final Identifier MATERIALS = Identifier.of("colony", "storage_role/materials");

    public static final Identifier GENERAL = Identifier.of("colony", "storage_role/general");

    private StorageRoles()
    {
    }
}
