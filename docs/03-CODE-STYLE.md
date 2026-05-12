# 03 — Code Style

Code style is enforced by **Spotless** (Eclipse formatter with custom config) and **Checkstyle**. CI fails on violations. There is no negotiation per-file.

## Bracing — Allman strict

Every brace on its own line. No exceptions.

```java
public final class ColonyService
{
    private final Map<ColonyId, Colony> colonies = new HashMap<>();

    public Optional<Colony> findColony(ColonyId id)
    {
        if (id == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(colonies.get(id));
    }
}
```

Never:

```java
public final class ColonyService {                 // ❌ K&R
    public Optional<Colony> findColony(ColonyId id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(colonies.get(id));
    }
}
```

## Blank lines

**Mandatory blank line before:**

- Every `return` statement (unless it's the only statement in the block, e.g. one-line lambda body)
- Every `if` block at any nesting level
- Every `for`/`while`/`do` block at any nesting level
- Every `try` block at any nesting level
- Logical sections within a method (lookups, calculations, side effects)

```java
public BuildingTier evaluateTier(BuildingType type, BuildingEvaluation eval)
{
    List<BuildingTier> sortedDesc = type.tiers().stream()
        .sorted((a, b) -> Integer.compare(b.rank(), a.rank()))
        .toList();

    for (BuildingTier tier : sortedDesc)
    {
        if (tier.structuralThreshold().min() > eval.structuralScore())
        {
            continue;
        }

        boolean allMet = tier.requirements().stream()
            .map(req -> req.evaluate(eval))
            .allMatch(TierRequirementResult::passes);

        if (allMet)
        {
            return tier;
        }
    }

    return type.unbuildedTier();
}
```

## No inline comments

**Comments do not live on the same line as code, nor inside method bodies explaining what the next line does.**

Wrong:

```java
public void process(Citizen citizen)
{
    int hunger = citizen.hunger();          // get hunger value     ❌
    if (hunger < 20) // very hungry                                  ❌
    {
        feed(citizen);
    }
    citizen.tick(); // tick the citizen                              ❌
}
```

Right:

```java
public void process(Citizen citizen)
{
    int hunger = citizen.hunger();

    if (hunger < HUNGER_FEED_THRESHOLD)
    {
        feed(citizen);
    }

    citizen.tick();
}
```

If you need a magic number explained, **name it as a constant**. If you need to explain what a method does, **rename the method or extract a smaller method**. Code reads itself when written for humans.

## Where comments ARE allowed

- **JavaDoc** on public types and public methods that take parameters or have non-trivial contracts. Skip for trivial getters/setters/constructors.
- **Class-level comments** describing the role of the class in the architecture (one paragraph, above the class declaration).
- **TODO comments** with mandatory milestone target: `// TODO(V2-economy): wire treasury credit on wage payment.`
- **Invariant comments** above fields that encode gameplay rules: `// Cached scoring result, recomputed on block change in zone. Not source of truth.`

Comments should explain **why**, not **what**. The code itself is the "what."

## Imports

- No wildcard imports. Spotless rejects them.
- Static imports allowed for test assertions only (`assertThat`, `assertEquals`).
- Imports grouped: java.* / javax.* / org.* / com.akikazu.colony.* / others. Blank line between groups.

## Naming

| Element | Convention | Example |
|---|---|---|
| Package | `lowercase.dotted` | `com.akikazu.colony.api.building` |
| Class | `PascalCase` | `BuildingTierEvaluator` |
| Interface | `PascalCase`, no `I` prefix | `BuildingTier`, not `IBuildingTier` |
| Method | `camelCase`, verb | `evaluateTier`, `findColony` |
| Field | `camelCase` | `structuralScore` |
| Constant | `UPPER_SNAKE` | `HUNGER_FEED_THRESHOLD` |
| Type parameter | Single uppercase letter | `T`, `E`, `K`, `V` |
| Identifier (registry id) | `lower_snake` | `colony:tier_req/min_volume` |

Names should be descriptive. Avoid abbreviations except universal ones (`id`, `ctx`, `pos`). Never single-letter variables outside loops.

## Null safety

- `@NullMarked` on every package via `package-info.java`. Everything is non-null by default.
- `@Nullable` (JSpecify) on any field, parameter, or return that can be null.
- NullAway (Error Prone plugin) enforces this. Violations fail compilation.
- `Optional<T>` for return types where absence is a normal outcome (lookups, queries). Not for fields.
- Never return `null` from a public method without `@Nullable` and a clear reason in JavaDoc.

## Records vs classes

- Use `record` for immutable data carriers (DTOs, configs, payload definitions, value objects).
- Use `class` for behavior carriers (services, controllers, state machines).
- Records can have static factory methods and validation in compact constructors. They cannot have mutable state.

```java
public record BuildingId(UUID value)
{
    public BuildingId
    {
        if (value == null)
        {
            throw new IllegalArgumentException("BuildingId cannot wrap null");
        }
    }

    public static BuildingId random()
    {
        return new BuildingId(UUID.randomUUID());
    }
}
```

## Sealed types for closed hierarchies

When a polymorphic type has a fixed set of variants, use `sealed`:

```java
public sealed interface ZoneValidationError
    permits Disconnected, OverlapsWithSibling, OutsideParent, TooSmall, TooLarge
{
}
```

The compiler enforces exhaustive `switch`. Adding a new variant forces the user to update all switches. Use for error types, state machines, command types.

## Exceptions

- Never catch `Exception` broadly. Catch specific types.
- Custom exceptions extend `RuntimeException` (we don't use checked exceptions outside I/O).
- Exception messages are user-facing-ish: contain the offending value, the expected condition, and (when relevant) the file/line context.

```java
throw new IllegalStateException(
    "BuildingTier evaluation failed: tier %s claims rank %d but structural %f below threshold %f"
        .formatted(tier.id(), tier.rank(), structural, tier.structuralThreshold().min()));
```

## Logging

- Use SLF4J via NeoForge's logger. Never `System.out.println`.
- Log levels:
  - `ERROR`: something is broken, requires investigation.
  - `WARN`: something is suspicious but not broken (e.g. fallback used).
  - `INFO`: significant lifecycle events (colony created, hut placed). Sparse.
  - `DEBUG`: developer-only, gated by config flag.
  - `TRACE`: never commit code with TRACE enabled by default.

## Tests

- JUnit 5 for pure logic in `:core` and partly in `:common`.
- GameTest framework for in-world behavior in `:neoforge`.
- Test class name = `{ClassUnderTest}Test`. Test method name = `should{Behavior}When{Condition}` or `{behavior}_{condition}`.
- One assertion per test ideally; multiple if they validate one logical outcome.
- Mock external state with simple records/fakes, not Mockito (start without it; introduce only if needed).

## Git commit conventions

Conventional Commits:

```
feat(building): add freeform footprint validation
fix(citizen): correct pathfinding stall when home zone is unloaded
refactor(persistence): extract migration step interface
docs(architecture): clarify :api module boundary
test(building): cover overcrowded room edge cases
chore(deps): bump Parchment to 2025.06.15
```

Commit messages explain **why** in the body if the change is non-obvious. The subject line explains **what**.

## Code style enforcement files

- `build-logic/config/eclipse-allman.xml` — Eclipse JDT formatter exported from IntelliJ with Allman + Colony tweaks.
- `build-logic/config/checkstyle.xml` — Custom Checkstyle config with rules for blank-line-before-return, no-trailing-comment, no-wildcard-import.
- `.editorconfig` at root — basic indent/charset rules for non-Java files (.md, .json, .toml).

CI runs `./gradlew spotlessCheck checkstyleMain` on every push. Failures block merges.

## When the style is wrong

If you encounter Spotless/Checkstyle blocking something legitimate, **do not disable the rule**. Open an issue describing the case. Style rules are global, not per-PR concessions.
