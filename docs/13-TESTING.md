# 13 — Testing Strategy

## Philosophy

Tests have one job: catch regressions before users do. Tests that take more time to maintain than they save are deleted. Coverage percentage is **not a target** — coverage of critical invariants is.

Three layers of testing, mapped to the module structure.

## Layer 1 — Pure unit tests in `:core` and `:common`

JUnit 5. Headless. No Minecraft. Fast (< 5 seconds total run).

### What MUST be tested at this layer

- Every Codec roundtrip (encode then decode yields the original).
- Every `TierRequirement` evaluator.
- Every `JobBehavior.nextIntent()` for known states.
- Every persistence migration step.
- All `Registry<T>` operations.
- All event bus dispatch logic.
- All math helpers (`BlockBox.contains`, `ColumnPos` math, scoring curve functions).

### What MUST NOT be tested at this layer

- Anything that requires `Minecraft.getInstance()`.
- Anything that requires a `Level` (use `:neoforge` GameTests instead).
- Rendering (visual tests are GUI baselines, see `12-UX-GUI.md`).
- Network packet flow (use integration tests, see Layer 2).

### Test structure

```
<module>/src/test/java/com/akikazu/colony/<area>/
├── codec/
│   ├── BuildingTierCodecTest.java
│   └── JobCodecTest.java
├── tier/
│   └── BuildingTierEvaluatorTest.java
├── persistence/
│   └── migration/
│       └── steps/
│           └── V1ToV2MigrationTest.java
└── event/
    └── ColonyEventBusTest.java
```

Test classes mirror production class paths. Test method names: `behaviorUnderCondition()` or `should<X>When<Y>()`.

### Codec test pattern

```java
@Test
void roundtripsThroughDispatchCodec()
{
    Job original = new HarvestingJob(
        Identifier.of("colony", "tree_chopping"),
        16,
        List.of(Identifier.of("colony", "filter/farming_tools")));

    DataResult<Tag> encoded = Job.DISPATCH.encodeStart(NbtOps.INSTANCE, original);
    assertTrue(encoded.result().isPresent());

    DataResult<Pair<Job, Tag>> decoded = Job.DISPATCH.decode(NbtOps.INSTANCE, encoded.result().get());
    assertTrue(decoded.result().isPresent());

    Job restored = decoded.result().get().getFirst();
    assertEquals(original, restored);
}
```

Every Codec gets a roundtrip test. No exceptions.

### Migration test pattern

```java
@Test
void migratesV1ColonySnapshotToV2()
{
    CompoundTag v1Data = createV1Snapshot();
    v1Data.putInt("dataVersion", 1);

    MigrationResult result = MigrationChain.migrate(v1Data, ColonySnapshot.CURRENT_VERSION);

    assertTrue(result.success());
    assertEquals(2, result.tag().getInt("dataVersion"));
    assertHasField(result.tag(), "treasury");
}
```

Every migration step has a self-test verifying:

- V→V+1 succeeds on representative data.
- V→V+1 preserves invariants (no citizens lost, no buildings duplicated).
- V→V+1 sets the new `dataVersion` correctly.

### Mocking

We do **not** use Mockito by default. Use real implementations or simple test doubles (records, fakes).

```java
public record TestEvalContext(float structuralScore, int volume, Map<Identifier, Integer> roomCounts)
    implements BuildingEvaluation
{
}

@Test
void evaluatesTier1WhenStructuralAboveThreshold()
{
    BuildingType type = TestBuildingTypes.basicFarmerHut();
    BuildingEvaluation eval = new TestEvalContext(0.25f, 60, Map.of(Identifier.of("colony", "office"), 1));

    BuildingTier result = new BuildingTierEvaluator().evaluateTier(type, eval);

    assertEquals("tier_1_basic", result.id().getPath());
}
```

If Mockito becomes truly necessary (rare), add it as a `testImplementation` dep with reason in the build script.

## Layer 2 — GameTests in `:neoforge`

NeoForge's GameTest framework. Spawns a test world, places blocks/entities, asserts behavior. Slower than unit tests but tests real Minecraft interactions.

### What gets tested at this layer

- Block placement and interaction (Hut block triggers PendingPlacement).
- Entity spawning (Town Hall founds colony, 4 citizens appear).
- Pathfinding (citizen walks from A to B).
- Custom navigation (citizen avoids claimed zone of another colony).
- Anchor right-click interactions (right-click scarecrow with seed sets crop).
- Networking end-to-end (client payload triggers server mutation triggers client delta).
- Persistence end-to-end (save world, reload, state intact).

### Test setup

```java
@GameTestHolder("colony")
public class ColonyFoundationGameTests
{
    @GameTest(template = "empty_3x3_platform")
    public static void townHallFoundsColonyAndSpawnsCitizens(GameTestHelper helper)
    {
        helper.setBlock(new BlockPos(1, 2, 1), ColonyBlocks.TOWN_HALL.get());

        helper.succeedWhen(() -> {
            long citizensCount = helper.getLevel().getEntitiesOfClass(EntityCitizen.class, helper.absoluteAABB())
                .size();
            helper.assertTrue(citizensCount == 4, "Expected 4 citizens, found " + citizensCount);
        });
    }
}
```

Templates live in `:neoforge/src/main/resources/data/colony/structures/test/`. Generated via vanilla structure block tooling, exported as `.nbt`.

### Required GameTest coverage by end of V1

| System | GameTests required |
|---|---|
| Town Hall founding | 3 (spawn count, position validity, retry-on-blocked-spawn) |
| Hut placement workflow | 5 (Zone Wand required, AABB paint, freeform paint, validation failure, cancellation) |
| Room designation | 4 (within outer zone, overlap rejected, requirements pass/fail, slot assignment) |
| Storage chest typing | 3 (typed via Colony Tool, role cycling, orphan detection) |
| Anchor right-click | 4 (scarecrow + seed, configuration GUI, linking, unlinking) |
| Citizen pathfinding | 6 (basic A to B, avoid claimed zone, recovery after wall change, cache hit, hierarchical fallback if implemented, stuck recovery) |
| Citizen needs | 3 (eats when hungry, sleeps when tired, Town Hall fallback) |
| Persistence | 2 (save and reload preserve state, migration applied on load of old save) |

Total: ~30 GameTests for V1. CI runs them all on every PR via `./gradlew :neoforge:runGameTestServer`.

### GameTest performance

Each test should complete in < 10 seconds. Long-running scenarios (multi-day citizen behavior) use accelerated tick rates via `helper.setBlockAndUpdate` and manual tick advance.

If a test takes > 30 seconds, split it.

## Layer 3 — Manual integration testing

Some scenarios are too complex or stateful for automated tests. These are documented and run manually before each release.

### Manual test checklist (run before alpha, beta, V1.0)

- Place Town Hall in survival mode, run the full Phase 2 farmer loop (place hut, paint zones, designate scarecrow, hire citizen, watch crops grow).
- Run a dedicated server with 3 connected clients, each founds a colony, no cross-colony interference observed.
- Save world after 30 minutes of play, reload, verify state intact.
- Use `/colony` admin commands and verify each works.
- Install Create + Farmer's Delight, verify no recipe/tag conflicts.
- Stress test: 30 citizens active, observe TPS > 18.
- Memory test: 5 colonies running for 1 hour, observe heap usage stable.

Checklist lives in `docs/manual-test-checklist.md` (created in Phase 2 when relevant).

## Performance benchmarking

Benchmarks in `:core/src/jmh/` using JMH (Java Microbenchmark Harness).

### What gets benchmarked

- Codec encode/decode for full colony snapshots.
- `BuildingTierEvaluator` on representative buildings.
- `ColonyNodeEvaluator` cost computation for 100 nodes.
- Path cache hit/miss rates.
- Room scoring on 100-block rooms.

### Benchmark execution

Run nightly in CI on a dedicated runner (or weekly if cost matters). Results published as a chart.

Regression detection:

- If a benchmark slows down by > 20% between commits, the responsible commit is flagged in the PR.
- Acceptable regressions (e.g. added a new feature that costs cycles) require an explicit justification comment in the PR description.

### Benchmark targets (initial, may evolve)

| Benchmark | Target | Hard limit |
|---|---|---|
| Codec encode `ColonySnapshot` (50 citizens) | < 5 ms | < 20 ms |
| Codec decode same | < 5 ms | < 20 ms |
| `BuildingTierEvaluator` (representative building) | < 1 ms | < 5 ms |
| Room scoring (100-block room) | < 5 ms | < 20 ms |
| Path computation (50-block direct line) | < 10 ms | < 50 ms |
| Path computation (cross-zone, 100-block obstacle-heavy) | < 50 ms | < 200 ms |
| Outer zone validation (1000-column footprint) | < 50 ms | < 200 ms |
| Tick budget for 50 citizens combined | < 8 ms | < 16 ms |

Hard limits failing means the build fails. Targets failing means a yellow warning.

## CI configuration

GitHub Actions workflow `.github/workflows/ci.yml`:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - run: ./gradlew spotlessCheck checkstyleMain build
      - run: ./gradlew :core:test :common:test
      - run: ./gradlew :neoforge:runGameTestServer
      - uses: actions/upload-artifact@v4
        with:
          name: jars
          path: '**/build/libs/*.jar'

  benchmark:
    runs-on: ubuntu-latest
    if: github.event_name == 'schedule'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - run: ./gradlew :core:jmh
      - run: ./gradlew :common:jmh
      - uses: actions/upload-artifact@v4
        with:
          name: benchmarks
          path: '**/build/reports/jmh/'

  publish:
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')
    needs: build
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew :api:publish
      - run: ./gradlew :neoforge:build
      - uses: actions/upload-release-asset@v1
```

`build` runs on every push and PR. `benchmark` runs nightly on `schedule`. `publish` runs on tags.

## What's NOT tested

We accept that the following are untested and require manual verification:

- Visual rendering correctness (relies on GUI baselines, which require human review).
- Network reliability over real internet (only local network in CI).
- Modpack interactions (each pack has unique conflicts, tested manually by modpack authors).
- Multi-version Minecraft compatibility (we target 1.21.1 only).

Acknowledging these gaps prevents false confidence.

## When tests fail in CI

The PR author is responsible for:

1. Investigating the failure (don't blame "flaky CI").
2. Fixing the test or fixing the code.
3. **Not** disabling the test to ship the PR (forbidden).
4. If the test is genuinely broken (test bug, not code bug), opening a separate PR to fix the test and explaining why.

CI is not a gate to game with. CI is a contract: green CI means the change is safe to merge.
