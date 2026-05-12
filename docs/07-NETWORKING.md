# 07 — Networking & Server Authority

## Cardinal rule

The dedicated server owns all colony, citizen, building, room, storage, anchor, and economy state. The client renders snapshots and sends typed action requests. **The client never mutates state directly.**

This is non-negotiable. Retrofitting server authority kills multiplayer mods. We build it in from MVP-1.

## Payload system

NeoForge 1.21.1 provides `CustomPacketPayload` + `StreamCodec`. We use this exclusively. No `SimpleChannel`, no legacy Forge networking.

Each payload is a record:

```java
public record OpenBuildingScreenPayload(
    ColonyId colony,
    BuildingId building,
    int tabIndex)
    implements CustomPacketPayload
{
    public static final Type<OpenBuildingScreenPayload> TYPE =
        new Type<>(Identifier.of("colony", "open_building_screen"));

    public static final StreamCodec<ByteBuf, OpenBuildingScreenPayload> STREAM_CODEC =
        StreamCodec.composite(
            ColonyId.STREAM_CODEC, OpenBuildingScreenPayload::colony,
            BuildingId.STREAM_CODEC, OpenBuildingScreenPayload::building,
            ByteBufCodecs.VAR_INT, OpenBuildingScreenPayload::tabIndex,
            OpenBuildingScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
```

Registered in `:neoforge` via `RegisterPayloadHandlersEvent`:

```java
event.registrar("1")
    .playToServer(OpenBuildingScreenPayload.TYPE, OpenBuildingScreenPayload.STREAM_CODEC,
        OpenBuildingScreenHandler::handle);
```

## Payload categories

Payloads are organized into three buckets by direction and purpose.

### Command payloads (client → server)

Player actions. The server validates and applies. Examples:

- `DesignateZonePayload` — player confirms a zone painted with Colony Tool.
- `AssignZoneToSlotPayload` — player assigns a painted zone to a Building slot.
- `TypeChestPayload` — player right-clicks a chest with Storage-mode Colony Tool.
- `LinkAnchorPayload` — player links a scarecrow to a Farmer Hut.
- `HireCitizenPayload` — player assigns a citizen to a job slot.
- `FireCitizenPayload` — player removes a citizen from a job.
- `ConfigureAnchorPayload` — player updates anchor zone dimensions via GUI.

All command payloads:

- Are received on the main server thread.
- Validate: player has permission (owns colony or has trust level), entity is in range, target exists.
- Mutate state via the appropriate service.
- Push a delta payload back to all affected clients.

### Snapshot payloads (server → client)

Full state of a domain object, sent when the player opens a GUI. Heavy, rare. Examples:

- `ColonySnapshotPayload` — overview of a colony (citizens count, buildings list, treasury balance V2).
- `BuildingSnapshotPayload` — full state of a building (rooms, storage, anchors, current quality, tier).
- `CitizenSnapshotPayload` — full state of a citizen (traits, skills, mood, current intent).

Sent in response to:

- Player opens a GUI for the corresponding object.
- Player joins server (initial sync of accessible colonies).
- `/colony resync` admin command.

### Delta payloads (server → client)

Incremental updates while a GUI is open. Light, frequent. Examples:

- `BuildingTierChangedPayload` — tier transition.
- `RoomScoredPayload` — room quality recomputed.
- `CitizenStateChangedPayload` — citizen entered new state (EATING, WORKING, RESTING).
- `StorageContentsChangedPayload` — typed chest contents changed.
- `AnchorLinkedPayload` — anchor link added/removed.

The client maintains a `ColonyView` mirror, updated by delta payloads. When the player closes a GUI, the server stops pushing deltas for that domain object (subscription model).

## Subscription model

To avoid flooding clients with deltas they don't care about:

1. When a player opens a GUI for object X, the client sends a `SubscribePayload(X)`.
2. The server registers the client's subscription to deltas for X.
3. While subscribed, the server pushes all deltas affecting X to that client.
4. When the player closes the GUI, the client sends `UnsubscribePayload(X)`.
5. The server removes the subscription.

Subscriptions are cleaned up automatically on player disconnect or dimension change.

## Validation rules

Every command payload handler runs these checks before applying:

1. **Identity check.** The payload is signed by the player who sent it (vanilla packet listener context).
2. **Permission check.** The player has permission for this action (owns the colony, or has trust level ≥ required).
3. **Target existence.** The target colony/building/chest/citizen exists and is loaded.
4. **Range check.** For physical actions (zone painting, chest typing), the player is within plausible range of the target (configurable, default 64 blocks).
5. **State validity.** The mutation is valid in the current state (e.g. you can't assign a citizen to a tier-1 hut requiring tier-2 housing).
6. **Anti-cheat.** Rates of action checked against thresholds (e.g. no more than 1 zone designation per 5 seconds, prevents script abuse).

Failed validation:

- Logs at `WARN` with player UUID, payload type, failure reason.
- Sends a `RequestRejectedPayload` to the client with a user-facing message.
- Does not mutate state.

## Threading

Default handler thread is the main server thread. For computationally heavy validation (zone scanning):

```java
event.registrar("1")
    .playToServer(DesignateZonePayload.TYPE, DesignateZonePayload.STREAM_CODEC,
        DesignateZoneHandler::handle)
    .executesOn(HandlerThread.NETWORK);
```

The handler runs on the network thread, computes the scan async via `CompletableFuture`, then schedules the state mutation back to the main thread via `server.execute(...)`.

## Bundling

Initial colony sync sends many payloads (one snapshot per building, many deltas). Use `ServerGamePacketListener.sendBundled(...)` to send them atomically.

## Versioning

The payload registrar version (`"1"` in `event.registrar("1")`) bumps when payload format changes incompatibly. Clients with incompatible versions are disconnected at handshake with a clear message.

V1 → V2 will likely require a version bump (new payloads for economy, reputation). We plan for it.

## Anti-cheat baseline

V1 implements basic anti-cheat:

- Rate limiting per player per payload type.
- Distance validation for spatial actions.
- Permission validation (no spoofing colony ID).

V1 does **not** implement:

- Cryptographic signing of payloads (overkill for vanilla MC trust model).
- Replay protection (sequence numbers).
- Bot detection (action pattern analysis).

These are V2+ considerations if grief becomes a documented problem.

## Permissions

Each colony has a permission table:

```java
public record ColonyPermissions(
    Map<UUID, PermissionLevel> playerPermissions,
    PermissionLevel defaultLevel)
{
}

public enum PermissionLevel
{
    NONE,
    VIEW,
    INTERACT,
    OFFICER,
    OWNER
}
```

- `NONE` — cannot see colony at all.
- `VIEW` — can open GUIs read-only.
- `INTERACT` — can deposit/take from chests, use anchors.
- `OFFICER` — can hire/fire citizens, designate zones, modify huts.
- `OWNER` — can transfer ownership, dissolve colony.

V1 defaults to `OWNER` for the colony founder, `NONE` for everyone else. V2 adds a permission management GUI.

## Audit log

Significant server-side mutations are logged to a per-colony audit:

```
[colony abc-123] 2026-05-15 14:23:10 OWNER absolutezeroo: designated zone {uuid} for Building {bid}
[colony abc-123] 2026-05-15 14:24:02 OFFICER bob: assigned citizen {cid} to job colony:job/farmer in Building {bid}
[colony abc-123] 2026-05-15 14:25:48 OWNER absolutezeroo: fired citizen {cid}
```

Stored in colony NBT, viewable via `/colony audit {colonyId}` for admins. Useful for debugging player reports and griefing investigations on shared servers.

## Single-player vs dedicated server

Internally, single-player runs an integrated server. All network code paths are identical to dedicated server. The client→server payloads still go through the registrar, just over the integrated connection.

This means **no special-casing** for single-player. The same code paths are tested in both contexts. CI runs both modes.

## What we don't do

- **No client-authoritative state.** Ever. Not for "optimization." Not for "responsiveness."
- **No raw NBT in payloads.** Use Codec + StreamCodec.
- **No payloads that mutate state via reflection.** Each payload has an explicit handler.
- **No global broadcast payloads.** Always targeted (specific client subscriptions).
- **No fire-and-forget mutations.** Every command has a server response (success/failure).
