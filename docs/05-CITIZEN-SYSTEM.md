# 05 — Citizen System

## Overview

Citizens are custom entities (`EntityCitizen extends PathfinderMob`), not vanilla villagers. They have traits, skills, mood, jobs, schedules, and (V2) peer relationships.

## Entity model

```java
public class EntityCitizen extends PathfinderMob
{
    // Identity (immutable per citizen)
    private final UUID citizenId;
    private final String displayName;
    private final CitizenAppearance appearance;

    // Affiliation (mutable)
    private @Nullable ColonyId colony;
    private @Nullable BuildingId homeBuilding;
    private @Nullable RoomId homeRoom;
    private @Nullable BuildingId workBuilding;
    private @Nullable JobAssignment job;

    // State (mutable, synced via DataAttachment)
    private CitizenState state;          // EATING, WORKING, RESTING, IDLE, TRAVELING
    private CitizenMood mood;            // happiness modifier stack
    private CitizenNeeds needs;          // hunger, fatigue, social (V2)
    private ImmutableList<CitizenTrait> traits;
    private ImmutableMap<JobType, SkillLevel> skills;

    // Behavior
    private final IntentQueue intentQueue;
}
```

State is split between:

- **Entity fields** for things synced naturally by Minecraft (position, look, animation).
- **DataAttachment** for colony-specific state (traits, mood, current intent). Syncable, persists across reloads.
- **Per-colony state** for things that don't belong on the entity (the citizen's home assignment, the colony reference itself, audit trail). Stored in the colony's NBT.

## Spawning

When the Town Hall is placed and the colony is founded:

1. 4 initial citizens spawn in a configurable radius around the Town Hall.
2. Each citizen has:
   - A randomly assigned name from the lang JSON pool.
   - Randomly assigned appearance (skin variant, hair, clothing).
   - 1-3 starting traits drawn from the trait pool weighted by rarity.
   - No job, no home.
3. Spawn search expands by 2 blocks per attempt, up to `CITIZEN_SPAWN_RETRY_ATTEMPTS` (server config, default 5). If no valid spawn location is found, the citizen is queued for next tick.

Configuration:

- `STARTING_CITIZEN_COUNT` (default 4, range 1-10)
- `CITIZEN_SPAWN_SEARCH_RADIUS` (default 6)
- `CITIZEN_SPAWN_VERTICAL_SEARCH` (default 3)
- `CITIZEN_SPAWN_RETRY_ATTEMPTS` (default 5)

## Lifecycle

Citizens do not breed, age, or die of old age in V1. They:

- Spawn at colony founding.
- Live in the colony indefinitely.
- Can be removed only by player action (fire from job + remove from colony via GUI) or death (combat, environment).
- On death: corpse drops, citizen is removed from colony. No respawn. Replacement requires immigration (V2).

## Identity rules

- Citizens never rename. Their `displayName` is fixed at spawn.
- Citizens never change jobs autonomously. Only the player assigns/unassigns.
- Citizens can refuse a job assignment if requirements are not met (skill, trait, tool, housing tier). The GUI explains why.

## Needs

V1 implements two needs:

### Hunger

- Each citizen has a `hunger` value (0-100, 100 = full).
- Hunger decreases by `HUNGER_DECAY_PER_DAY` (default 20) per in-game day.
- When `hunger < HUNGER_HUNGRY_THRESHOLD` (default 40), the citizen's `IntentQueue` prioritizes finding food.
- When `hunger < HUNGER_STARVING_THRESHOLD` (default 10), productivity drops 50% and mood receives a `starving` modifier.
- Eating restores hunger to 100 minus a small inefficiency. Food quality affects mood (V2).

Food sources, in priority order:

1. Cookhouse with a prepared meal in its `output` chest.
2. Tavern (V2).
3. Town Hall fallback (slow, low-quality, free).
4. Personal storage in their home (if they carry food).
5. Forage (V2).

### Fatigue

- Each citizen has a `fatigue` value (0-100, 100 = fully rested).
- Fatigue increases while working, decreases while sleeping.
- When `fatigue < FATIGUE_TIRED_THRESHOLD`, the citizen wants to go home.
- Sleeping in a bed in a valid assigned `bedroom` recovers fatigue fully.
- Sleeping at the Town Hall fallback (homeless citizens) recovers 50% only.

V1 keeps fatigue simple: no insomnia, no nightmare modifiers. V2 expands.

## Mood

Mood is a stack of `MoodModifier`s, each with a magnitude, decay curve, and expiration tick.

```java
public record MoodModifier(
    Identifier source,
    Identifier modifierType,
    float magnitude,
    long appliedAtTick,
    long expiresAtTick,
    DecayCurve decay)
{
}
```

Current mood = sum of active modifiers, clamped to [-100, +100].

Modifier sources in V1:

- `colony:mood/well_fed` (+10 after eating quality meal, decays over 1 day)
- `colony:mood/well_rested` (+5 after good sleep, decays over 12 hours)
- `colony:mood/homeless` (-15 if no home room assigned, no decay until resolved)
- `colony:mood/starving` (-25 while hunger < 10, no decay until resolved)
- `colony:mood/tier_low_housing` (-5 to -15 if home room tier is below citizen's preference)

Mood affects:

- Productivity multiplier (mood 50 = +20% productivity, mood -50 = -30%).
- Job refusal threshold (very low mood citizens may refuse work).

V2 expands to 4-axis reputation; V1 keeps a single mood scalar for simplicity.

## Traits

Traits are stable characteristics of a citizen, assigned at spawn and immutable.

```java
public interface CitizenTrait
{
    CitizenTraitType type();

    TraitTier tier();
}
```

Trait types are registered (`ColonyRegistries.CITIZEN_TRAIT_TYPE`). Each trait affects:

- Job eligibility (e.g. `colony:trait/strong` enables Miner/Lumberjack at higher tier).
- Productivity modifier for matching jobs (e.g. `colony:trait/green_thumb` +20% on farming).
- Mood thresholds (e.g. `colony:trait/luxurious` requires higher housing tier).

V1 trait pool (10 traits):

- `strong` — bonus to physical jobs (mining, lumber).
- `green_thumb` — bonus to farming.
- `dexterous` — bonus to crafting (V2).
- `patient` — fewer mood penalties from waiting.
- `loyal` — slower mood decay.
- `lazy` — productivity penalty, lower hunger drain.
- `hot_headed` — faster mood swings.
- `claustrophobic` — penalty in small rooms.
- `glutton` — eats more, mood bonus when fed well.
- `night_owl` — productivity bonus during night work (rare assignment).

Trait probability is JSON-driven; modpacks can shift the distribution.

## Skills

Each citizen has a `Map<JobType, SkillLevel>` of skills earned through work.

```java
public record SkillLevel(int xp, int level)
{
    public static final int MAX_LEVEL = 100;
}
```

XP is gained by performing job-specific actions (a Farmer planting crops gains XP in `colony:job/farmer`). Skill level multiplies productivity and unlocks higher job tiers.

V1 has flat XP curves. V2 introduces diminishing returns and skill ceilings.

## Jobs

A `Job` is the assignment of a citizen to a work Building.

```java
public interface Job
{
    JobType type();

    CitizenId worker();

    BuildingId workBuilding();

    JobState state();
}
```

`JobType` is registered. It includes:

- Required `WorkstationModule` in the Building (e.g. Farmer requires a `farmer_office`).
- Required tools (e.g. Farmer requires `#minecraft:hoes`).
- Required traits or skills (optional, can be null).
- Required housing tier for the citizen (e.g. some jobs require `tier_1` housing minimum).
- The `JobBehavior` implementation.

### Job assignment workflow

1. Player opens a Building GUI, navigates to Citizens tab.
2. Sees `JobSlot`s declared by the Building (e.g. Farmer Hut has one slot of type `colony:job/farmer`).
3. Clicks "Assign worker".
4. Sees a list of eligible citizens (filtered by trait/skill/housing requirements).
5. Selects one. The assignment is created.

If no citizen is eligible, the GUI explains why.

### Job behavior

`JobBehavior` is stateless and shared across all citizens of the same job. It exposes:

```java
public interface JobBehavior
{
    CitizenIntent nextIntent(JobContext ctx, Citizen citizen, Job job);

    default void onAssigned(JobContext ctx, Citizen citizen, Job job) {}

    default void onUnassigned(JobContext ctx, Citizen citizen, Job job) {}
}
```

`nextIntent` is called by the scheduler when the citizen finishes its current intent. It returns the next action: go to work zone, take tool from storage, plant seeds, return crops to output, eat at cookhouse, sleep at home, etc.

V1 jobs:

- `colony:job/builder` — places blocks per template (when templates ship in V2; in V1, just demolition/clearing).
- `colony:job/farmer` — works scarecrow zones, plants/harvests crops.

V2 adds: miner, lumberjack, cook, hunter, fisher, herder, guard.

## Schedule

Each job has a daily schedule defining what hours the citizen works.

```java
public record JobSchedule(
    int workStartTick,
    int workEndTick,
    int mealBreakTick,
    int mealBreakDurationTicks)
{
}
```

V1 has a fixed schedule per job type, hardcoded sensibly (Farmer: 6am-6pm, meal at noon). V2 adds player-customizable schedules and night shifts.

Outside work hours, the citizen returns home, eats if hungry, sleeps if tired, otherwise idles in the colony.

## Intent queue

The `IntentQueue` is the citizen's short-term action plan. The scheduler:

1. Picks up to N citizens this tick (round-robin, budget-capped).
2. For each, if the current intent is complete, calls `jobBehavior.nextIntent()` to compute the next.
3. Pushes movement, interaction, and animation commands to the entity.

Intents in V1:

- `MoveToIntent(BlockPos target)`
- `MoveToZoneIntent(WorkZoneAnchor zone)`
- `TakeFromStorageIntent(Storage source, ItemFilter filter, int count)`
- `DepositToStorageIntent(Storage target, ItemStack stack)`
- `PerformJobActionIntent(JobAction action)` — abstract, job-specific
- `EatIntent(FoodSource source)`
- `SleepIntent(Bed bed)`
- `IdleAtIntent(BlockPos location)`

Citizens "think" at 2-5 Hz (intent re-evaluation). They animate and move at 20 Hz (vanilla mob behavior).

## Pathfinding

Custom pathfinding from scratch, in `:common/citizen/impl/pathfinding/`.

### Rationale

Vanilla `PathNavigation` was tested by the user and found insufficient:

- No concept of "claimed zones" (citizens path through neighbors' buildings).
- No avoidance of unsafe blocks specific to colony (lava in mining zones, unstable scaffolding).
- No route preference for colony-built paths.
- Performance degrades when 30+ entities path simultaneously.

### Components

**`ColonyNodeEvaluator extends NodeEvaluator`**

Computes node accessibility and cost. Extends vanilla with:

- Higher cost for blocks inside other colonies' claimed zones.
- Hard block (impassable) for marked "dangerous" blocks per colony rules.
- Bonus (lower cost) for blocks tagged as colony pathway material (cobblestone path, gravel, configurable).
- Climb cost adjusted per citizen trait (`strong` cheaper to climb, `lazy` more expensive).

**`ColonyPathNavigation extends GroundPathNavigation`**

Wraps vanilla nav, swaps the evaluator. Caches paths per (citizen, destination) for 60-120 ticks. Invalidates cache when block changes detected in the path's bounding box.

**`PathCache` service**

Stores recent successful paths. LRU eviction. Shared across citizens to enable "follow leader" patterns where multiple citizens reuse the same path.

**Hierarchical pathfinding (V2)**

For long-distance routes, plans coarse path between rooms/buildings first (room graph), then fine path within each room. Reduces A* search space dramatically for cross-colony movement.

### Cost estimate

2-4 months of focused solo work to reach production quality. Tracked as the largest single risk in V1 roadmap.

### Fallback

If custom pathfinding hits an unrecoverable bug in production, the entity can fall back to vanilla `GroundPathNavigation` for that tick to avoid soft-locks. This is a debugging tool, not a long-term mode.

## Snapshot for GUI

When the player opens a citizen's detail GUI, the server builds a `CitizenSnapshot` and sends it via `CustomPacketPayload`:

```java
public record CitizenSnapshot(
    UUID citizenId,
    String displayName,
    @Nullable ColonyId colony,
    @Nullable BuildingId homeBuilding,
    @Nullable RoomId homeRoom,
    @Nullable BuildingId workBuilding,
    @Nullable JobAssignment job,
    CitizenState state,
    CitizenMood mood,
    CitizenNeeds needs,
    List<CitizenTrait> traits,
    Map<Identifier, SkillLevel> skills)
{
}
```

The client renders. The server pushes delta updates while the GUI is open.

## What's deferred to V2

- Peer relationships (`SocialGraph`).
- Immigration (new citizens arrive from outside).
- Disease, injury, healing.
- Children, aging, death by old age.
- 4-axis reputation (citizen_loyalty, citizen_peer, player_standing, colony_prosperity).
- Customizable schedules.
- Night shifts and 24/7 colonies.
- Citizen-to-citizen item handoff (porter chains).
- Skill ceilings and prestige progression.

## What's NOT planned

- Vanilla villager subclassing.
- Romance system.
- Religious beliefs or factional alignment.
