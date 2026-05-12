# 18 — Save Versioning Strategy

This document specifies how Colony handles save format evolution across versions. Without a clear policy, the first breaking change destroys every player's save and the project's reputation along with it.

## Versioning model

Every persisted record has a top-level `int dataVersion` field. Versions are sequential integers starting at 1.

```java
public record ColonySnapshot(
    int dataVersion,
    UUID id,
    /* ... */
)
{
    public static final int CURRENT_VERSION = 1;
}
```

Each persisted entity has its own version counter: `ColonySnapshot.CURRENT_VERSION`, `CitizenSnapshot.CURRENT_VERSION`, `BuildingSnapshot.CURRENT_VERSION`, etc. They evolve independently.

## When dataVersion bumps

The data version bumps when **the on-disk representation changes**, not when behavior changes.

Bump on:

- Adding a required field to a record.
- Removing a field.
- Renaming a field.
- Changing a field type.
- Restructuring nested data.

Do NOT bump on:

- Adding optional fields with defaults (forward-compatible).
- Behavioral changes that consume existing data differently.
- Pure rendering or UI changes.
- New event types fired (events are transient, not persisted).

## Migration chain

When loading a record with `dataVersion < CURRENT_VERSION`, the migration chain runs in sequence:

```
saved_version  →  saved+1  →  saved+2  →  ...  →  CURRENT_VERSION
```

Each step is a registered `MigrationStep` in `:common/persistence/migration/steps/`. Steps are pure functions: `CompoundTag → CompoundTag`. They do not have side effects, do not access services.

```java
package com.akikazu.colony.common.persistence.migration.steps;

public final class V1ToV2ColonySnapshotMigration implements MigrationStep
{
    @Override
    public int fromVersion()
    {
        return 1;
    }

    @Override
    public int toVersion()
    {
        return 2;
    }

    @Override
    public Identifier scope()
    {
        return Identifier.of("colony", "snapshot");
    }

    @Override
    public CompoundTag migrate(CompoundTag input)
    {
        CompoundTag output = input.copy();
        output.putInt("dataVersion", 2);

        if (output.contains("mood"))
        {
            float mood = output.getFloat("mood");
            output.remove("mood");

            ListTag reputations = new ListTag();
            CompoundTag loyalty = new CompoundTag();
            loyalty.putString("scope", "citizen_loyalty");
            loyalty.putFloat("magnitude", mood);
            loyalty.putString("source", "colony:migration/v1_mood");
            reputations.add(loyalty);

            output.put("reputations", reputations);
        }

        return output;
    }
}
```

## Backward compatibility window

We commit to:

- **Always**: support loading saves from V1.0 going forward. A V1.0 save will load on every future version.
- **Within a minor version** (V1.0 → V1.1): no breaking save changes. Players upgrade transparently.
- **Across a major version** (V1.x → V2.0): migration runs automatically, transparent to player. The save is upgraded in place.
- **Across two major versions** (V1.x → V3.0): migration chain runs V1→V2→V3 automatically. No direct V1→V3 migration is written; we always go through intermediate versions.

We do NOT commit to:

- Loading future-version saves on older clients (forward compatibility). A V2 save will not load on a V1 client. The client refuses to load with a clear error.
- Loading corrupted or hand-edited saves. Malformed data is rejected.

## Save corruption recovery

If a save is corrupted or fails migration, the policy is:

1. **Refuse to load the corrupted file**. Do not partially load.
2. **Log the failure** with full diagnostic info to a separate file `colony-load-failure-{timestamp}.log` in the world directory.
3. **Show a clear error** to the player with the path to the log file.
4. **Preserve the original file** untouched. We do not overwrite a corrupted save with a "default" state.
5. **Suggest backup recovery**: the player can manually restore from `world/data/colony/colonies/{uuid}.nbt.bak` if available.

We maintain automatic `.bak` files:

- Before each migration, the original file is copied to `{name}.nbt.premigration.{from_version}`.
- Before each save during normal play, the previous version is kept as `{name}.nbt.bak`.

This means at any time, a player has at minimum the last saved state plus the pre-migration state. Worst case, they lose the last play session, not the whole colony.

## Migration safety rules

Every migration step must:

- **Be idempotent**: running `migrate(migrate(x))` produces the same result as `migrate(x)`. (Technically running migrate twice would happen on a bug — idempotence prevents data loss in this case.)
- **Preserve data identity**: a colony's UUID never changes through migration. A citizen's identity is preserved.
- **Add a `_migration_log` entry**: the output CompoundTag has a `ListTag _migration_log` that records each migration applied. Allows post-hoc debugging of save state.
- **Fail loudly**: if migration cannot proceed (impossible data), throw a `MigrationException` with detailed reason. Do NOT silently produce malformed output.

```java
public class MigrationException extends RuntimeException
{
    private final Identifier scope;
    private final int fromVersion;
    private final int toVersion;
    private final CompoundTag offendingData;

    // Constructor and getters omitted.
}
```

## Migration testing

Every migration step has a self-test:

```java
package com.akikazu.colony.common.persistence.migration.selftest;

@Test
void v1ToV2ColonySnapshotPreservesMood()
{
    CompoundTag v1 = new CompoundTag();
    v1.putInt("dataVersion", 1);
    v1.putString("name", "TestColony");
    v1.putFloat("mood", 42.5f);

    CompoundTag v2 = new V1ToV2ColonySnapshotMigration().migrate(v1);

    assertEquals(2, v2.getInt("dataVersion"));
    assertEquals("TestColony", v2.getString("name"));
    assertFalse(v2.contains("mood"));
    assertTrue(v2.contains("reputations"));

    ListTag reps = v2.getList("reputations", Tag.TAG_COMPOUND);
    assertEquals(1, reps.size());
    CompoundTag loyalty = reps.getCompound(0);
    assertEquals("citizen_loyalty", loyalty.getString("scope"));
    assertEquals(42.5f, loyalty.getFloat("magnitude"), 0.001);
}
```

Self-tests are required for every migration. CI runs them. Missing self-tests fail the build.

## Cross-migration testing

Beyond per-step tests, end-to-end migration chains are tested:

```java
@Test
void migratesFromV1ToCurrentVersion()
{
    CompoundTag v1Save = loadFixture("fixtures/v1_full_colony.nbt");

    MigrationResult result = MigrationChain.migrate(v1Save, ColonySnapshot.CURRENT_VERSION,
        Identifier.of("colony", "snapshot"));

    assertTrue(result.success());
    assertEquals(ColonySnapshot.CURRENT_VERSION, result.tag().getInt("dataVersion"));

    // Decode through current Codec, verify everything makes sense
    Codec<ColonySnapshot> codec = ColonySnapshot.CODEC;
    DataResult<Pair<ColonySnapshot, Tag>> decoded = codec.decode(NbtOps.INSTANCE, result.tag());
    assertTrue(decoded.result().isPresent());

    ColonySnapshot snapshot = decoded.result().get().getFirst();
    assertEquals("TestColony", snapshot.name());
    assertEquals(4, snapshot.citizens().size());
}
```

Fixtures are committed to `:common/src/test/resources/fixtures/`. Each major version has a fixture of "representative save data from that era." When a new migration lands, add a new fixture to ensure we never regress on old version loading.

## Announcing save-breaking changes

The semver of the mod jar (not `:api`) follows:

- **PATCH** (1.0.0 → 1.0.1): bugfixes, no save changes. Player upgrades freely.
- **MINOR** (1.0.x → 1.1.0): new features, no save format changes. Player upgrades freely.
- **MAJOR** (1.x.y → 2.0.0): save format may change. Migration runs automatically. Player should backup before upgrading.

Release notes for any MAJOR version include:

- Explicit statement of save migration: "This release migrates saves from V1.x to V2.0 automatically. Backups are recommended."
- List of migration steps applied.
- Known limitations or data loss (if any — we strive for zero).

## Save data inspection

For debugging, a command `/colony save-info {colonyId}` exposes:

- Current dataVersion of each record type.
- Last migration applied (timestamp + from→to versions).
- Disk size of the colony file.
- Number of records in each sub-list (citizens, buildings, etc.).

The command is OP-restricted on dedicated servers (information leakage potential) and unrestricted in single-player.

## What V1 doesn't have

V1 ships with `dataVersion = 1` everywhere. No migration steps exist initially because there's nothing to migrate from.

The first real migration appears when V2 (or any V1.x change to save format) lands.

We commit, from V1.0 onward, that the data version pattern is part of the on-disk format. No retroactive change to the schema is possible.

## Future-version refusal

When a save claims a `dataVersion` higher than the loading client can handle:

```
The save data for this colony (version 3) was created by a newer version of Colony.
Update Colony to version 2.0.0 or later to load this save.

Save path: world/data/colony/colonies/abc-123.nbt
Save version: 3
This client's version: 2
```

The client refuses to load. It does not attempt "best effort partial load" — that path leads to silent corruption.

## What gets saved vs computed

The save format excludes anything that can be recomputed:

- **Saved**: colony identity, citizen identity and traits, building zones, room assignments, chest typings, anchor configurations.
- **Recomputed**: room quality scores, building tier evaluations, adjacency relationships, mood scalar (from modifier list).

This minimizes save size and migration surface. Adding a new computed field never bumps the version. Adding a new persisted field does.

## Save size budget

Targets:

- Empty colony: < 5 KB.
- Colony with 10 citizens, 5 buildings: < 50 KB.
- Colony with 50 citizens, 20 buildings: < 500 KB.
- Colony with 200 citizens, 50 buildings (V2+ stretch): < 5 MB.

Exceeding these targets in a release triggers an investigation. We may need to denormalize or compress further.

## Forward planning for V2

V2 adds significant persisted state:

- `Wallet` per citizen.
- `Treasury` per colony (potentially multiple).
- `Reputation` records (sparse, only non-default).
- `SocialGraph` (sparse).
- `TaxPolicy` instances per colony.

Each gets its own `dataVersion` field starting at 1 in V2.0. They are added to the colony save format as new sub-records, not modifications to existing records. This means **V1 saves load into V2 cleanly** — the new sub-records start empty when missing.

The migration step for V2 simply adds default-empty sub-records:

```java
public CompoundTag migrate(CompoundTag v1)
{
    CompoundTag v2 = v1.copy();
    v2.putInt("dataVersion", 2);

    if (!v2.contains("treasuries"))
    {
        v2.put("treasuries", new ListTag());
    }

    if (!v2.contains("tax_policies"))
    {
        v2.put("tax_policies", new ListTag());
    }

    // Citizens get default empty wallets via their own migration.

    return v2;
}
```

This is the cleanest migration strategy: **additive only**. V2 never removes V1 fields, only adds new ones.

We commit to this approach for V1 → V2. If V2 → V3 needs to remove or restructure fields, that migration will be more complex, but V1 → V2 stays clean.

## What this strategy costs

- Every record type has a `dataVersion` field, increasing file size by ~12 bytes per record. Negligible.
- Every migration step requires a self-test. Maintenance overhead: small.
- Backup files double save disk usage. Acceptable.
- Migration chain runs synchronously on load, blocking world load by some milliseconds per record. Acceptable for typical colony sizes.

For colonies with thousands of records, migration runs on a background thread with a loading screen. The player sees progress; the server doesn't block.

## What this strategy buys

- Zero data loss across version upgrades (assuming migration steps are correct).
- Confidence in releasing breaking changes — we know the format evolves cleanly.
- Easier debugging when reports surface: "what data version did this save start at?"
- Modpack authors can trust that updating Colony won't trash their pack.

Without this strategy, the project hits the wall on the first V1 → V2 release. With it, the project can evolve indefinitely.
