# 15 — API: Reputation System (V2 frozen)

This document specifies the public API for the V2 reputation system. The interfaces below are committed contracts. They exist in `:api` from V1.0 with no-op implementations, become functional in V2.

V1 ships with a single scalar `mood` field on `Citizen`. V2 replaces it with the four-axis model below. The V1 mood is mapped to V2's `citizen_loyalty` axis on migration; the other three axes start at 0.

---

## Conceptual model

Reputation has **four independent axes**. Each axis measures a different relationship.

| Axis | Subject → Target | Range | Drives |
|---|---|---|---|
| `citizen_loyalty` | One citizen → their colony | -100 to +100 | Productivity, desertion risk, tax compliance |
| `citizen_peer` | One citizen → another citizen | -100 to +100 | Work refusal, friendship, conflict |
| `player_standing` | A social class within the colony → the player | -100 to +100 | Class-specific behaviors (merchants refuse contracts, etc.) |
| `colony_prosperity` | The colony as a whole | -100 to +100 | Immigration rate, public mood, faction events |

Axes do not interact directly. A citizen with high loyalty can hate another citizen (negative peer). The player can be loved by merchants and hated by laborers simultaneously.

---

## Core interface

```java
package com.akikazu.colony.api.reputation;

import com.akikazu.colony.api.registry.Identifier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Reputation
{
    ReputationScope scope();

    Identifier subjectId();

    Identifier targetId();

    float current();

    float trendOverDays(int days);

    java.util.List<ReputationModifier> activeModifiers();
}
```

`subjectId` and `targetId` are domain identifiers (`CitizenId.asIdentifier()`, `ColonyId.asIdentifier()`, etc.). The same `Reputation` record represents a typed relationship.

## Scope enumeration

```java
public enum ReputationScope
{
    CITIZEN_LOYALTY,
    CITIZEN_PEER,
    PLAYER_STANDING,
    COLONY_PROSPERITY
}
```

V1.0 ships this enum with all four constants, even though only the first is functional. Addons can register modifiers against any scope; they'll be inert in V1.

## Modifier model

```java
public record ReputationModifier(
    Identifier source,
    Identifier modifierType,
    float magnitude,
    long appliedAtTick,
    long expiresAtTick,
    DecayCurve decay)
{
    public float currentMagnitude(long currentTick)
    {
        if (currentTick >= expiresAtTick)
        {
            return 0.0f;
        }

        return decay.apply(magnitude, appliedAtTick, currentTick, expiresAtTick);
    }
}

public sealed interface DecayCurve permits Linear, Exponential, Stepped, None
{
    float apply(float baseMagnitude, long appliedAt, long current, long expires);

    record Linear() implements DecayCurve { /* ... */ }
    record Exponential(float halfLifeTicks) implements DecayCurve { /* ... */ }
    record Stepped(java.util.List<StepPoint> steps) implements DecayCurve { /* ... */ }
    record None() implements DecayCurve { /* ... */ }
}
```

**Invariants** (enforced by codec validation):
- `magnitude` is finite (no NaN, no Infinity).
- `appliedAtTick <= expiresAtTick`. Equal means single-tick modifier.
- `source` identifies the gameplay event that produced this modifier (e.g. `colony:event/tax_collected_42`). Used for retroactive removal.
- `modifierType` identifies the category (e.g. `colony:mood/tax_burden`). Used for display and aggregation.

## Service interface

```java
package com.akikazu.colony.api.reputation;

@NullMarked
public interface ReputationService
{
    Reputation get(ReputationScope scope, Identifier subject, Identifier target);

    void applyModifier(Reputation reputation, ReputationModifier modifier);

    void removeModifiersBySource(Identifier source);

    void removeModifiersByType(Reputation reputation, Identifier modifierType);

    java.util.stream.Stream<Reputation> queryBySubject(ReputationScope scope, Identifier subject);

    java.util.stream.Stream<Reputation> queryByTarget(ReputationScope scope, Identifier target);
}
```

**Contract:**
- All operations are server-side only. The client receives `Reputation` snapshots via networking; it cannot call `applyModifier`.
- `get` always returns a non-null `Reputation`. If no record exists, it returns a `Reputation` with `current = 0` and no modifiers.
- `applyModifier` is idempotent for `(source, modifierType)` pairs: applying the same source+type twice replaces the existing modifier, never stacks. Stacking with the same type requires distinct sources.
- `removeModifiersBySource` removes ALL modifiers from that source across ALL reputations. Used when an event is retracted (e.g. a tax assessment was incorrect and reversed).
- Stream methods return lazy sequences. Implementations must support iteration without holding locks.

## Querying current value

```java
public interface Reputation
{
    default float currentMagnitudeAt(long currentTick)
    {
        return (float) activeModifiers().stream()
            .mapToDouble(m -> m.currentMagnitude(currentTick))
            .sum();
    }

    default float clampedCurrent(long currentTick)
    {
        return Math.max(-100.0f, Math.min(100.0f, currentMagnitudeAt(currentTick)));
    }
}
```

`current()` in the main interface returns the clamped sum at the current tick. Addons that need raw (unclamped) values use `currentMagnitudeAt`.

## Events

```java
package com.akikazu.colony.api.event.reputation;

import com.akikazu.colony.core.event.Event;
import com.akikazu.colony.api.reputation.Reputation;
import com.akikazu.colony.api.reputation.ReputationModifier;

public record ReputationModifierAppliedEvent(
    Reputation reputation,
    ReputationModifier modifier,
    float previousValue,
    float newValue)
    implements Event
{
}

public record ReputationCrossedThresholdEvent(
    Reputation reputation,
    float threshold,
    boolean ascending)
    implements Event
{
}
```

Thresholds are configurable per scope per identifier. The default thresholds:
- `CITIZEN_LOYALTY`: -50 (desertion risk), +50 (loyalty bonus), +75 (devoted).
- `CITIZEN_PEER`: -50 (work refusal), +50 (friendship), +75 (companionship).
- `PLAYER_STANDING`: -25 (resistance), +50 (favor).
- `COLONY_PROSPERITY`: 0 (neutral), +50 (immigration trigger).

Addons can register additional thresholds via `ReputationService.registerThreshold(scope, target, threshold)`. Each threshold fires `ReputationCrossedThresholdEvent` once per crossing.

## Social graph (citizen peer)

The `CITIZEN_PEER` scope uses a sparse storage model. For N citizens, full N×N matrix would be wasteful (e.g. 200 citizens = 40 000 entries).

```java
public interface SocialGraph
{
    Reputation opinion(CitizenId observer, CitizenId target);

    java.util.stream.Stream<PeerRelation> relationsOf(CitizenId citizen);

    void recordInteraction(CitizenId a, CitizenId b, InteractionType type);
}

public record PeerRelation(CitizenId other, float current, java.util.List<ReputationModifier> modifiers)
{
}
```

**Storage:**
- Only relations with non-zero current value or active modifiers are stored.
- Relations are symmetric in storage but not in value: A's opinion of B can differ from B's opinion of A. We store both directions independently.
- Pruning: a relation with current value in [-1, +1] and no active modifiers is deleted at end-of-day cleanup.

**Interaction types:**

```java
public enum InteractionType
{
    WORKED_TOGETHER,
    DINED_TOGETHER,
    CONFLICT,
    SHARED_HARDSHIP,
    SHARED_CELEBRATION,
    TRAIT_CLASH,
    TRAIT_SYMPATHY
}
```

Each interaction type maps to a default `(modifierType, magnitude, decay)` triplet, JSON-overridable.

## Reputation drivers (JSON-defined)

In V2, drivers are declared in datapack JSON:

```json
{
  "id": "colony:reputation_rule/wage_paid",
  "trigger": "colony:event/wage_paid",
  "scope": "citizen_loyalty",
  "subject_resolver": "event.citizen_id",
  "target_resolver": "event.colony_id",
  "modifier": {
    "type": "colony:mood/wage_satisfaction",
    "magnitude_formula": "wage_ratio - 1.0",
    "decay": { "type": "linear", "duration_ticks": 24000 }
  }
}
```

`magnitude_formula` is **NOT** an arbitrary expression engine. V1.0 commits to a closed set of pre-registered formulas:

```java
public enum BuiltinMagnitudeFormula
{
    CONSTANT,            // returns base
    WAGE_RATIO_DELTA,    // wage_received / expected_wage - 1.0
    HUNGER_NORMALIZED,   // (50 - hunger) / 50
    TAX_BURDEN_RATIO,    // tax_paid / income
    HOUSING_TIER_DELTA,  // citizen_expected_tier - actual_tier
    PEER_TRAIT_AFFINITY  // trait_pair lookup table
}
```

Addons can register additional formulas via `MagnitudeFormulaRegistry`. Custom formulas are Java code, not script — no sandbox needed.

This is a deliberate scope cut. A real expression engine (mvel, JEXL) costs 1-2 months and adds security surface. We commit to "Java formulas only" forever for this system.

## V1 stub implementation

In V1.0, the following minimal implementation ships:

```java
@ApiStatus.Internal
public final class V1NoOpReputationService implements ReputationService
{
    @Override
    public Reputation get(ReputationScope scope, Identifier subject, Identifier target)
    {
        if (scope == ReputationScope.CITIZEN_LOYALTY)
        {
            return new V1ScalarBackedReputation(scope, subject, target);
        }

        return new EmptyReputation(scope, subject, target);
    }

    @Override
    public void applyModifier(Reputation reputation, ReputationModifier modifier)
    {
        if (reputation.scope() == ReputationScope.CITIZEN_LOYALTY)
        {
            // Routes to the V1 mood scalar.
        }
    }

    // Other methods: no-op or return empty streams.
}
```

V1 mood modifiers from `colony:mood/*` get mapped to `citizen_loyalty` modifiers automatically. The migration from V1 to V2 preserves these.

## Migration V1 → V2

When V2 loads a V1 save:

```java
// In V1ToV2ReputationMigration
public CompoundTag migrate(CompoundTag v1Data)
{
    CompoundTag v2 = new CompoundTag();
    v2.putInt("dataVersion", 2);

    if (v1Data.contains("mood"))
    {
        float v1Mood = v1Data.getFloat("mood");
        ListTag modifiers = new ListTag();

        // Migrate to single citizen_loyalty modifier
        CompoundTag loyaltyModifier = new CompoundTag();
        loyaltyModifier.putString("scope", "citizen_loyalty");
        loyaltyModifier.putFloat("magnitude", v1Mood);
        loyaltyModifier.putString("source", "colony:migration/v1_mood");
        loyaltyModifier.putString("decay", "none");

        modifiers.add(loyaltyModifier);
        v2.put("reputations", modifiers);
    }

    return v2;
}
```

V2 reputation maintains separate `citizen_peer`, `player_standing`, `colony_prosperity` records starting at 0. V1 saves never had these; they appear cleanly in V2.

## Consequences and listeners

Reputation crossing thresholds fires events that other systems consume. V2 includes these built-in listeners:

```java
public final class DesertionRiskListener
{
    @Subscribe
    public void onCrossed(ReputationCrossedThresholdEvent event)
    {
        if (event.reputation().scope() != ReputationScope.CITIZEN_LOYALTY)
        {
            return;
        }

        if (event.threshold() == DESERTION_THRESHOLD && !event.ascending())
        {
            // Mark citizen as desertion candidate.
            // Each subsequent tick has a stochastic chance of leaving.
        }
    }
}
```

V1 contains zero such listeners. V2 ships these:
- `DesertionRiskListener` (citizen_loyalty < -50).
- `WorkRefusalListener` (citizen_peer < -50, same workplace).
- `ImmigrationListener` (colony_prosperity > +50, periodic check).
- `ProductivityModifierListener` (any axis, scaled).

Addons can register their own listeners via `ColonyEventBus.subscribe(...)`.

## Addons consuming the API

A V2 addon adding a new reputation driver:

```java
public final class MyReputationAddon implements ColonyAddon
{
    @Override
    public void register(ColonyAddonContext ctx)
    {
        ctx.eventBus().subscribe(WagePaidEvent.class, this::onWagePaid);
    }

    private void onWagePaid(WagePaidEvent event)
    {
        ReputationService reputation = ctx.service(ReputationService.class);
        Reputation citizenLoyalty = reputation.get(
            ReputationScope.CITIZEN_LOYALTY,
            event.citizen().asIdentifier(),
            event.colony().asIdentifier());

        ReputationModifier modifier = new ReputationModifier(
            Identifier.of("myaddon", "event/wage_" + event.id()),
            Identifier.of("myaddon", "mood/big_wage_satisfaction"),
            event.wage().amount() * 0.02f,
            event.atTick(),
            event.atTick() + 24000L,
            new DecayCurve.Linear());

        reputation.applyModifier(citizenLoyalty, modifier);
    }
}
```

This compiles against `:api` in V1.0. Runs as no-op in V1. Becomes functional in V2 with no code changes.

## What V1.0 commits to API-wise

- `Reputation` interface, exact signature above.
- `ReputationScope` enum, all 4 values.
- `ReputationModifier` record, exact fields.
- `DecayCurve` sealed interface, 4 variants (Linear, Exponential, Stepped, None).
- `ReputationService` interface, all 7 methods.
- `SocialGraph` interface, all 3 methods.
- `InteractionType` enum, 7 values.
- `BuiltinMagnitudeFormula` enum, 6 values.
- `MagnitudeFormulaRegistry` for addon extension.
- `ReputationModifierAppliedEvent`, `ReputationCrossedThresholdEvent` records.

These are frozen. Changes after V1.0 are MAJOR version bumps of `:api`.

## What we explicitly DO NOT commit to

- Internal storage format. Sparse map, dense map, database backed — implementation choice.
- Threshold defaults — these are JSON-configurable, can change per pack.
- Performance characteristics beyond "should not block server tick".
- Visual representation in GUI — UX may change.

Future expansion (V2.x or V3) can add:
- New `ReputationScope` values (factional, religious, etc.).
- New `DecayCurve` variants.
- New `InteractionType` values.

Additions are MINOR version bumps. Existing addons keep working.
